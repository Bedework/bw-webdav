/* ********************************************************************
    Licensed to Jasig under one or more contributor license
    agreements. See the NOTICE file distributed with this work
    for additional information regarding copyright ownership.
    Jasig licenses this file to you under the Apache License,
    Version 2.0 (the "License"); you may not use this file
    except in compliance with the License. You may obtain a
    copy of the License at:

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on
    an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied. See the License for the
    specific language governing permissions and limitations
    under the License.
*/
package org.bedework.webdav.servlet.common;

import org.bedework.util.logging.BwLogger;
import org.bedework.util.servlet.HttpAppLogger;
import org.bedework.util.servlet.io.CharArrayWrappedResponse;
import org.bedework.util.xml.XmlEmit;
import org.bedework.util.xml.tagdefs.WebdavTags;
import org.bedework.webdav.servlet.common.MethodBase.MethodInfo;
import org.bedework.webdav.servlet.shared.WebdavException;
import org.bedework.webdav.servlet.shared.WebdavForbidden;
import org.bedework.webdav.servlet.shared.WebdavNsIntf;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Enumeration;
import java.util.HashMap;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;
import javax.xml.namespace.QName;

/** WebDAV Servlet.
 * This abstract servlet handles the request/response nonsense and calls
 * abstract routines to interact with an underlying data source.
 *
 * @author Mike Douglass   douglm   rpi.edu
 * @version 1.0
 */
public abstract class WebdavServlet extends HttpServlet
        implements HttpAppLogger, HttpSessionListener {
  protected boolean dumpContent;

  /* If true we don't invalidate the session - this might allow the application
   * to be used as the server for CAS authenticated widgets etc.
   */
  protected boolean preserveSession;

  /** Table of methods - set at init
   */
  protected HashMap<String, MethodInfo> methods = new HashMap<>();

  /* Try to serialize requests from a single session
   * This is very imperfect.
   */
  static class Waiter {
    boolean active;
    int waiting;
  }

  private static final HashMap<String, Waiter> waiters = new HashMap<>();

  public String getLogPrefix(final HttpServletRequest request) {
    return "webdav";
  }

  @Override
  public void init(final ServletConfig config) throws ServletException {
    super.init(config);

    dumpContent = "true".equals(config.getInitParameter("dumpContent"));
    preserveSession = "true".equals(config.getInitParameter("preserve-session"));

    addMethods();
  }

  public void setPreserveSession(final boolean val) {
    preserveSession = val;
  }

  /** Get an interface for the namespace
   *
   * @param req       HttpServletRequest
   * @return WebdavNsIntf  or subclass of
   */
  public abstract WebdavNsIntf getNsIntf(HttpServletRequest req);

  @Override
  protected void service(final HttpServletRequest req,
                         HttpServletResponse resp)
      throws IOException {
    WebdavNsIntf intf = null;
    boolean serverError = false;

    try {
      logRequest(req);

      if (debug()) {
        debug("entry: " + req.getMethod());
        dumpRequest(req);
      }

      tryWait(req, true);

      intf = getNsIntf(req);

      if (req.getCharacterEncoding() == null) {
        req.setCharacterEncoding("UTF-8");
        if (debug()) {
          debug("No charset specified in request; forced to UTF-8");
        }
      }

      if (debug() && dumpContent) {
        resp = new CharArrayWrappedResponse(resp);
      }

      String methodName = req.getHeader("X-HTTP-Method-Override");

      if (methodName == null) {
        methodName = req.getMethod();
      }

      final MethodBase method = intf.getMethod(methodName);

      //resp.addHeader("DAV", intf.getDavHeader());

      if (method == null) {
        info("No method for '" + methodName + "'");

        // ================================================================
        //     Set the correct response
        // ================================================================
        resp.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
      } else {
        method.checkServerInfo(req, resp);
        method.doMethod(req, resp);
      }
    } catch (final WebdavForbidden wdf) {
      sendError(intf, wdf, resp);
    } catch (final Throwable t) {
      serverError = handleException(intf, t, resp, serverError);
    } finally {
      if (intf != null) {
        try {
          intf.close();
        } catch (final Throwable t) {
          serverError = handleException(intf, t, resp, serverError);
        }
      }

      try {
        tryWait(req, false);
      } catch (final Throwable ignored) {}

      if (debug() && dumpContent &&
          (resp instanceof final CharArrayWrappedResponse wresp)) {
        /* instanceof check because we might get a subsequent exception before
         * we wrap the response
         */

        if (wresp.getUsedOutputStream()) {
          debug("------------------------ response written to output stream -------------------");
        } else {
          final String str = wresp.toString();

          if ((str == null) || (str.isEmpty())) {
            debug("------------------------ No response content -------------------");
            resp.setContentLength(0);
          } else {
            debug(
                    "------------------------ Dump of response -------------------");
            debug(str);
            debug(
                    "---------------------- End dump of response -----------------");

            final byte[] bs = str.getBytes();
            resp = (HttpServletResponse)wresp.getResponse();
            debug("contentLength=" + bs.length);
            resp.setContentLength(bs.length);
            resp.getOutputStream().write(bs);
          }
        }
      }

      try {
        logRequestOut(req);
      } catch (final Throwable ignored) {}

      if (!preserveSession) {
        /* WebDAV is stateless - toss away the session */
        try {
          final HttpSession sess = req.getSession(false);
          if (sess != null) {
            sess.invalidate();
          }
        } catch (final Throwable ignored) {}
      }
    }
  }

  /* Return true if it's a server error */
  private boolean handleException(final WebdavNsIntf intf, final Throwable t,
                                  final HttpServletResponse resp,
                                  boolean serverError) {
    if (serverError) {
      return true;
    }

    try {
      if (t instanceof final WebdavException wde) {

        final int status = wde.getStatusCode();
        if (status == HttpServletResponse.SC_INTERNAL_SERVER_ERROR) {
          error(wde);
          serverError = true;
        }
        sendError(intf, wde, resp);
        return serverError;
      }

      if (debug()) {
        error(t);
      }

      sendError(intf, t, resp);
      return true;
    } catch (final Throwable t1) {
      error(t1);
      // Pretty much screwed if we get here
      return true;
    }
  }

  private void sendError(final WebdavNsIntf intf, final Throwable t,
                         final HttpServletResponse resp) {
    try {
      try {
        intf.rollback();
      } catch (final Throwable ignored) {}

      if (t instanceof final WebdavException wde) {
        final QName errorTag = wde.getErrorTag();

        if (errorTag != null) {
          if (debug()) {
            debug("setStatus(" + wde.getStatusCode() + ")" +
                     " message=" + wde.getMessage());
          }
          resp.setStatus(wde.getStatusCode());
          resp.setContentType("text/xml; charset=UTF-8");
          if (!emitError(intf, errorTag, wde.getMessage(),
                         resp.getWriter())) {
            final StringWriter sw = new StringWriter();
            emitError(intf, errorTag, wde.getMessage(), sw);

            try {
              if (debug()) {
                debug("setStatus(" + wde.getStatusCode() + ")" +
                         " message=" + wde.getMessage());
              }
              resp.sendError(wde.getStatusCode(), sw.toString());
            } catch (final Throwable ignored) {
            }
          }
        } else {
          if (debug()) {
            debug("setStatus(" + wde.getStatusCode() + ")" +
                     " message=" + wde.getMessage());
          }
          resp.sendError(wde.getStatusCode(), wde.getMessage());
        }
      } else {
        if (debug()) {
          debug("setStatus(" + HttpServletResponse.SC_INTERNAL_SERVER_ERROR + ")" +
                   " message=" + t.getMessage());
        }
        resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                       t.getMessage());
      }
    } catch (final Throwable ignored) {
      // Pretty much screwed if we get here
      resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }
  }

  private boolean emitError(final WebdavNsIntf intf,
                            final QName errorTag,
                            final String extra,
                            final Writer wtr) {
      final XmlEmit xml = new XmlEmit();
      intf.addNamespace(xml);

      xml.startEmit(wtr);
      xml.openTag(WebdavTags.error);

      intf.emitError(errorTag, extra, xml);

      xml.closeTag(WebdavTags.error);
      xml.flush();

      return true;
  }
  /** Add methods for this namespace
   *
   */
  protected void addMethods() {
    methods.put("ACL", new MethodInfo(AclMethod.class, false));
    methods.put("COPY", new MethodInfo(CopyMethod.class, false));
    methods.put("GET", new MethodInfo(GetMethod.class, false));
    methods.put("HEAD", new MethodInfo(HeadMethod.class, false));
    methods.put("OPTIONS", new MethodInfo(OptionsMethod.class, false));
    methods.put("PROPFIND", new MethodInfo(PropFindMethod.class, false));

    methods.put("DELETE", new MethodInfo(DeleteMethod.class, true));
    methods.put("MKCOL", new MethodInfo(MkcolMethod.class, true));
    methods.put("MOVE", new MethodInfo(MoveMethod.class, true));
    methods.put("POST", new MethodInfo(PostMethod.class, true));
    methods.put("PROPPATCH", new MethodInfo(PropPatchMethod.class, true));
    methods.put("PUT", new MethodInfo(PutMethod.class, true));

    //methods.put("LOCK", new MethodInfo(LockMethod.class, true));
    //methods.put("UNLOCK", new MethodInfo(UnlockMethod.class, true));
  }

  private void tryWait(final HttpServletRequest req, final boolean in) throws Throwable {
    Waiter wtr;
    synchronized (waiters) {
      //String key = req.getRequestedSessionId();
      final String key = req.getRemoteUser();
      if (key == null) {
        return;
      }

      wtr = waiters.get(key);
      if (wtr == null) {
        if (!in) {
          return;
        }

        wtr = new Waiter();
        wtr.active = true;
        waiters.put(key, wtr);
        return;
      }
    }

    synchronized (wtr) {
      if (!in) {
        wtr.active = false;
        wtr.notify();
        return;
      }

      wtr.waiting++;
      while (wtr.active) {
        if (debug()) {
          debug("in: waiters=" + wtr.waiting);
        }

        wtr.wait();
      }
      wtr.waiting--;
      wtr.active = true;
    }
  }

  @Override
  public void sessionCreated(final HttpSessionEvent se) {
  }

  @Override
  public void sessionDestroyed(final HttpSessionEvent se) {
    final HttpSession session = se.getSession();
    final String sessid = session.getId();
    if (sessid == null) {
      return;
    }

    synchronized (waiters) {
      waiters.remove(sessid);
    }
  }

  /** Debug
   *
   * @param req servlet request to dump
   */
  public void dumpRequest(final HttpServletRequest req) {
    Enumeration<String> names = req.getHeaderNames();

    String title = "Request headers";

    debug(title);

    while (names.hasMoreElements()) {
      final String key = names.nextElement();
      final Enumeration<String> vals = req.getHeaders(key);

      while (vals.hasMoreElements()) {
        String val = vals.nextElement();

        if (key.equalsIgnoreCase("authorization") &&
                (val != null) &&
                (val.toLowerCase().startsWith("basic"))) {
          val = "Basic **********";
        }

        debug("  " + key + " = \"" + val + "\"");
      }
    }

    names = req.getParameterNames();

    title = "Request parameters";

    debug(title + " - global info and uris");
    debug("getRemoteAddr = " + req.getRemoteAddr());
    debug("getRequestURI = " + req.getRequestURI());
    debug("getRemoteUser = " + req.getRemoteUser());
    debug("getRequestedSessionId = " + req.getRequestedSessionId());
    debug("HttpUtils.getRequestURL(req) = " + req.getRequestURL());
    debug("contextPath=" + req.getContextPath());
    debug("query=" + req.getQueryString());
    debug("contentlen=" + req.getContentLength());
    debug("request=" + req);
    debug("parameters:");

    debug(title);

    while (names.hasMoreElements()) {
      final String key = names.nextElement();
      final String val = req.getParameter(key);
      debug("  " + key + " = \"" + val + "\"");
    }
  }

  /* ====================================================================
   *                   Logged methods
   * ==================================================================== */

  private final BwLogger logger = new BwLogger();

  @Override
  public BwLogger getLogger() {
    if ((logger.getLoggedClass() == null) && (logger.getLoggedName() == null)) {
      logger.setLoggedClass(getClass());
    }

    return logger;
  }
}
