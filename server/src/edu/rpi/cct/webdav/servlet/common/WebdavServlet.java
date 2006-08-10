/*
 Copyright (c) 2000-2005 University of Washington.  All rights reserved.

 Redistribution and use of this distribution in source and binary forms,
 with or without modification, are permitted provided that:

   The above copyright notice and this permission notice appear in
   all copies and supporting documentation;

   The name, identifiers, and trademarks of the University of Washington
   are not used in advertising or publicity without the express prior
   written permission of the University of Washington;

   Recipients acknowledge that this distribution is made available as a
   research courtesy, "as is", potentially with defects, without
   any obligation on the part of the University of Washington to
   provide support, services, or repair;

   THE UNIVERSITY OF WASHINGTON DISCLAIMS ALL WARRANTIES, EXPRESS OR
   IMPLIED, WITH REGARD TO THIS SOFTWARE, INCLUDING WITHOUT LIMITATION
   ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
   PARTICULAR PURPOSE, AND IN NO EVENT SHALL THE UNIVERSITY OF
   WASHINGTON BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL
   DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR
   PROFITS, WHETHER IN AN ACTION OF CONTRACT, TORT (INCLUDING
   NEGLIGENCE) OR STRICT LIABILITY, ARISING OUT OF OR IN CONNECTION WITH
   THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */
/* **********************************************************************
    Copyright 2005 Rensselaer Polytechnic Institute. All worldwide rights reserved.

    Redistribution and use of this distribution in source and binary forms,
    with or without modification, are permitted provided that:
       The above copyright notice and this permission notice appear in all
        copies and supporting documentation;

        The name, identifiers, and trademarks of Rensselaer Polytechnic
        Institute are not used in advertising or publicity without the
        express prior written permission of Rensselaer Polytechnic Institute;

    DISCLAIMER: The software is distributed" AS IS" without any express or
    implied warranty, including but not limited to, any implied warranties
    of merchantability or fitness for a particular purpose or any warrant)'
    of non-infringement of any current or pending patent rights. The authors
    of the software make no representations about the suitability of this
    software for any particular purpose. The entire risk as to the quality
    and performance of the software is with the user. Should the software
    prove defective, the user assumes the cost of all necessary servicing,
    repair or correction. In particular, neither Rensselaer Polytechnic
    Institute, nor the authors of the software are liable for any indirect,
    special, consequential, or incidental damages related to the software,
    to the maximum extent the law permits.
*/

package edu.rpi.cct.webdav.servlet.common;

import edu.rpi.cct.webdav.servlet.shared.WebdavException;
import edu.rpi.cct.webdav.servlet.shared.WebdavNsIntf;
import edu.rpi.sss.util.servlets.io.CharArrayWrappedResponse;

import java.io.InputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import org.apache.log4j.Logger;

/** WebDAV Servlet.
 * This abstract servlet handles the request/response nonsense and calls
 * abstract routines to interact with an underlying data source.
 *
 * @author Mike Douglass   douglm@rpi.edu
 * @version 1.0
 */
public abstract class WebdavServlet extends HttpServlet {
  protected boolean debug;

  protected boolean dumpContent;

  protected transient Logger log;

  /** Global resources for the servlet - not to be modified.
   */
  protected Properties props;

  /** Some sort of identifying string for logging
   *
   * @return String id
   */
  public abstract String getId();

  /** Get an interface for the namespace
   *
   * @param req       HttpServletRequest
   * @param config    ServletConfig
   * @param props     Properties
   * @return WebdavNsIntf  or subclass of
   * @throws WebdavException
   */
  public abstract WebdavNsIntf getNsIntf(HttpServletRequest req,
                                         ServletConfig config,
                                         Properties props)
      throws WebdavException;

  protected void service(HttpServletRequest req,
                         HttpServletResponse resp)
      throws ServletException, IOException {
    WebdavNsIntf intf = null;

    try {
      String debugStr = getInitParameter("debug");
      if (debugStr != null) {
        debug = !"0".equals(debugStr);
      }

      if (debug) {
        debugMsg("entry: " + req.getMethod());
        dumpRequest(req);
      }

      intf = getNsIntf(req, getServletConfig(), props);

      intf.init(this, req, props, debug);

      initMethods(intf, debug, dumpContent);

      if (dumpContent) {
        resp = new CharArrayWrappedResponse(resp,
                                            getLogger(), debug);
      }

      // resp.setStatus(HttpServletResponse.SC_OK);

      String methodName = req.getMethod();

      MethodBase method = (MethodBase)intf.getMethods().get(methodName.toUpperCase());

      if (method == null) {
        logIt("No method for '" + methodName + "'");

        // ================================================================
        //     Set the correct response
        // ================================================================
      } else {
        method.doMethod(req, resp);
      }
    } catch (WebdavException wde) {
      getLogger().error(this, wde);
      resp.sendError(wde.getStatusCode());
    } catch (Throwable t) {
      getLogger().error(this, t);
      resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    } finally {
      if (intf != null) {
        try {
          intf.close();
        } catch (Throwable t) {
          getLogger().error(this, t);
          resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
      }

      if (dumpContent) {
        CharArrayWrappedResponse wresp = (CharArrayWrappedResponse)resp;
        String str = wresp.toString();

        debugMsg("------------------------ Dump of response -------------------");
        debugMsg(str);
        debugMsg("---------------------- End dump of response -----------------");

        byte[] bs = str.getBytes();
        resp = (HttpServletResponse)wresp.getResponse();
        resp.setContentLength(bs.length);
        resp.getOutputStream().write(bs);
      }
    }
  }

  /** Init the methods.
   *
   * @param nsIntf
   * @param debug
   * @param dumpContent
   * @throws WebdavException
   */
  public void initMethods(WebdavNsIntf nsIntf,
                          boolean debug,
                          boolean dumpContent) throws WebdavException{
    HashMap methods = nsIntf.getMethods();
    addMethods(nsIntf);

    Iterator mnames = methods.keySet().iterator();
    while (mnames.hasNext()) {
      MethodBase mb = (MethodBase)methods.get(mnames.next());

      if (mb != null) {
        mb.init(nsIntf, debug, dumpContent);
      }
    }
  }

  /** Add methods for this namespace
   *
   * @param nsIntf
   * @throws WebdavException
   */
  public void addMethods(WebdavNsIntf nsIntf) throws WebdavException{
    HashMap methods = nsIntf.getMethods();

    methods.put("ACL", new AclMethod());
    methods.put("COPY", new CopyMethod());
    methods.put("GET", new GetMethod());
    methods.put("HEAD", new HeadMethod());
    methods.put("OPTIONS", new OptionsMethod());
    methods.put("PROPFIND", new PropFindMethod());

    if (!nsIntf.getAnonymous()) {
      methods.put("DELETE", new DeleteMethod());
      methods.put("MKCOL", new MkcolMethod());
      methods.put("MOVE", new MoveMethod());
      methods.put("POST", new PostMethod());
      methods.put("PROPPATCH", new PropPatchMethod());
      methods.put("PUT", new PutMethod());

      //methods.put("LOCK", new LockMethod());
      //methods.put("UNLOCK", new UnlockMethod());
    }
  }

  public void init(ServletConfig config) throws ServletException {
    super.init(config);

    dumpContent = "true".equals(config.getInitParameter("dumpContent"));

    getResources(config);
  }

  private void getResources(ServletConfig config) throws ServletException {
    String resname = config.getInitParameter("application");

    if (resname != null) {
      InputStream is;

      ClassLoader classLoader =
          Thread.currentThread().getContextClassLoader();
      if (classLoader == null) {
        classLoader = this.getClass().getClassLoader();
      }
      is = classLoader.getResourceAsStream(resname + ".properties");

      props = new Properties();
      try {
        props.load(is);
      } catch (IOException ie) {
        log.error(ie);
        throw new ServletException(ie);
      }
    }
  }

  /** Debug
   *
   * @param req
   */
  public void dumpRequest(HttpServletRequest req) {
    Logger log = getLogger();

    try {
      Enumeration names = req.getHeaderNames();

      String title = "Request headers";

      log.debug(title);

      while (names.hasMoreElements()) {
        String key = (String)names.nextElement();
        String val = req.getHeader(key);
        log.debug("  " + key + " = \"" + val + "\"");
      }

      names = req.getParameterNames();

      title = "Request parameters";

      log.debug(title + " - global info and uris");
      log.debug("getRequestURI = " + req.getRequestURI());
      log.debug("getRemoteUser = " + req.getRemoteUser());
      log.debug("getRequestedSessionId = " + req.getRequestedSessionId());
      log.debug("HttpUtils.getRequestURL(req) = " + req.getRequestURL());
      log.debug("contextPath=" + req.getContextPath());
      log.debug("query=" + req.getQueryString());
      log.debug("contentlen=" + req.getContentLength());
      log.debug("request=" + req);
      log.debug("parameters:");

      log.debug(title);

      while (names.hasMoreElements()) {
        String key = (String)names.nextElement();
        String val = req.getParameter(key);
        log.debug("  " + key + " = \"" + val + "\"");
      }
    } catch (Throwable t) {
    }
  }

  /**
   * @return LOgger
   */
  public Logger getLogger() {
    if (log == null) {
      log = Logger.getLogger(this.getClass());
    }

    return log;
  }

  /** Debug
   *
   * @param msg
   */
  public void debugMsg(String msg) {
    getLogger().debug(msg);
  }

  /** Info messages
   *
   * @param msg
   */
  public void logIt(String msg) {
    getLogger().info(msg);
  }
}
