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
import org.bedework.util.logging.Logged;
import org.bedework.util.misc.response.GetEntityResponse;
import org.bedework.util.misc.response.Response;
import org.bedework.util.xml.XmlEmit;
import org.bedework.util.xml.XmlEmit.NameSpace;
import org.bedework.util.xml.XmlEmit.Notifier;
import org.bedework.util.xml.XmlUtil;
import org.bedework.util.xml.tagdefs.WebdavTags;
import org.bedework.webdav.servlet.shared.WebdavBadRequest;
import org.bedework.webdav.servlet.shared.WebdavException;
import org.bedework.webdav.servlet.shared.WebdavNsIntf;
import org.bedework.webdav.servlet.shared.WebdavNsNode;
import org.bedework.webdav.servlet.shared.WebdavProperty;
import org.bedework.webdav.servlet.shared.WebdavStatusCode;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.io.IOException;
import java.io.Reader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.namespace.QName;
import javax.xml.ws.Holder;

/** Base class for all webdav servlet methods.
 */
public abstract class MethodBase implements Logged, SecureXml {
  protected boolean dumpContent;

  protected boolean hasBriefHeader;

//  private ServletConfig config;

  /** namespace interface for this request
   */
  protected WebdavNsIntf nsIntf;

  private String resourceUri;

  // private String content;

  protected XmlEmit xml;

  /** Called at each request
   */
  public abstract void init();

  private final SimpleDateFormat httpDateFormatter =
      new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss ");

  /**
   * @param req http request
   * @param resp http response
   */
  public abstract void doMethod(HttpServletRequest req,
                                HttpServletResponse resp);

  /** Allow servlet to create method.
   */
  public static class MethodInfo {
    private final Class<?> methodClass;

    private final boolean requiresAuth;

    /**
     * @param methodClass class of method
     * @param requiresAuth true for requires auth
     */
    public MethodInfo(final Class<?> methodClass,
                      final boolean requiresAuth) {
      this.methodClass = methodClass;
      this.requiresAuth = requiresAuth;
    }

    /**
     * @return Class for this method
     */
    public Class<?> getMethodClass() {
      return methodClass;
    }

    /** Called when servicing a request to determine if this method requires
     * authentication. Allows the servlet to reject attempts to change state
     * while unauthenticated.
     *
     * @return boolean true if authentication required.
     */
    public boolean getRequiresAuth() {
      return requiresAuth;
    }
  }

  /** Called at each request
   *
   * @param nsIntf interface
   * @param dumpContent true to provide debug content dump
   */
  public void init(final WebdavNsIntf nsIntf,
                   final boolean dumpContent) {
    this.nsIntf = nsIntf;
    this.dumpContent = dumpContent;

//    config = servlet.getServletConfig();
    xml = nsIntf.getXmlEmit();

    // content = null;
    resourceUri = null;

    init();
  }

  /** Get namespace interface
   *
   * @return WebdavNsIntf
   */
  public WebdavNsIntf getNsIntf() {
    return nsIntf;
  }

  /** Get the decoded and fixed resource URI
   *
   * @param req      Servlet request object
   * @return String  fixed up uri
   */
  public String getResourceUri(final HttpServletRequest req) {
    if (resourceUri != null) {
      return resourceUri;
    }

    resourceUri = getNsIntf().getResourceUri(req);

    if (debug()) {
      debug("resourceUri: " + resourceUri);
    }

    return resourceUri;
  }

  protected int defaultDepth(final int depth,
                             final int def) {
    if (depth < 0) {
      return def;
    }

    return depth;
  }

  /* depth must have given value
   */
  protected void checkDepth(final int depth,
                            final int val) {
    if (depth != val) {
      throw new WebdavBadRequest();
    }
  }

  protected String getStatus(final int status, String message) {
    if (message == null) {
      message = WebdavStatusCode.getMessage(status);
    }

    return "HTTP/1.1 " + status + " " + message;
  }

  protected void addStatus(final int status, String message) {
    if (message == null) {
      message = WebdavStatusCode.getMessage(status);
    }

    property(WebdavTags.status, "HTTP/1.1 " + status + " " + message);
  }

  /* A node of null corresponds to * for an OPTIONS request
   */
  protected void addHeaders(final HttpServletRequest req,
                            final HttpServletResponse resp,
                            final WebdavNsNode node) {
    addDavHeader(resp, node);

    // Lisa say's we need this
    resp.addHeader("MS-Author-Via", "DAV");

    final StringBuilder methods = new StringBuilder();
    for (final String name: getNsIntf().getMethodNames()) {
      if (!methods.isEmpty()) {
        methods.append(", ");
      }

      methods.append(name);
    }

    resp.addHeader("Allow", methods.toString());
  }

  public void checkServerInfo(final HttpServletRequest req,
                              final HttpServletResponse resp) {
    // This probably needs changes

    final String curToken = getNsIntf().getServerInfo().getToken();

    final String method = req.getMethod();
    boolean sendServerInfoUrl = false;

    final String theirToken = req.getHeader("server-info-token");

    if (theirToken == null) {
      // Always send header for no token on OPTIONS
      sendServerInfoUrl = method.equalsIgnoreCase("options");
    } else if (!theirToken.equals(curToken)) {
      // * will not match so covers that case
      sendServerInfoUrl = true;
    }

    if (sendServerInfoUrl) {
      resp.addHeader("Link",
                     "<" + getNsIntf().makeServerInfoUrl(req) +
                             ">; rel=\"server-info\"; " +
                             "token=\"" + curToken + "\"");
    }
  }

  protected void addDavHeader(final HttpServletResponse resp,
                              final WebdavNsNode node) {
    resp.addHeader("DAV", getNsIntf().getDavHeader(node));
  }

  /** Parse the Webdav request body, and return the DOM representation.
   *
   * @param req        Servlet request object
   * @param resp       Servlet response object for bad status
   * @return Document  Parsed body or null for no body
   * @exception WebdavException Some error occurred.
   */
  protected Document parseContent(final HttpServletRequest req,
                                  final HttpServletResponse resp) {
    hasBriefHeader = Headers.brief(req);

    return parseContent(req.getContentLength(), getNsIntf().getReader(req));
  }

  /** Parse a reader and return the DOM representation.
   *
   * @param contentLength        Content length
   * @param reader        Reader
   * @return Document  Parsed body or null for no body
   */
  protected Document parseContent(final int contentLength,
                                  final Reader reader) {
    final Document doc = parseXmlSafely(contentLength, reader);
    if (doc == null) {
      debug("No document");
    } else {
      debug(doc.toString());
    }

    return doc;
  }

  private class XmlNotifier extends Notifier {
    private boolean enabled;

    private final Holder<Boolean> openFlag;

    XmlNotifier(final Holder<Boolean> openFlag) {
      this.openFlag = openFlag;
      enabled = true;
    }

    @Override
    public void doNotification() {
      enabled = false;

      if (!openFlag.value) {
        openFlag.value = true;
        openTag(WebdavTags.propstat);
        openTag(WebdavTags.prop);
      }
    }

    @Override
    public boolean isEnabled() {
      return enabled;
    }
  }

  /** Build the response for a single node for a propfind request
   *
   * @param node webdav node
   * @param props list of properties
   */
  public void doPropFind(final WebdavNsNode node,
                         final Collection<WebdavProperty> props) {
    final WebdavNsIntf intf = getNsIntf();
    final Collection<WebdavProperty> unknowns = new ArrayList<>();

    final Holder<Boolean> openFlag = new Holder<>(Boolean.FALSE);
    final XmlNotifier notifier = new XmlNotifier(openFlag);
    try {
      xml.setNotifier(notifier);

      for (final WebdavProperty pr: props) {
        if (!intf.knownProperty(node, pr)) {
          unknowns.add(pr);
        } else  {
          /*
          if (!open) {
            openTag(WebdavTags.propstat);
            openTag(WebdavTags.prop);
            open = true;
          }*/

          addNs(pr.getTag().getNamespaceURI());
          if (!intf.generatePropValue(node, pr, false)) {
            unknowns.add(pr);
          }
        }
      }

      if (openFlag.value) {
        closeTag(WebdavTags.prop);
        addStatus(node.getStatus(), null);

        closeTag(WebdavTags.propstat);
      }

      xml.setNotifier(null);

      if (!hasBriefHeader && !unknowns.isEmpty()) {
        openTag(WebdavTags.propstat);
        openTag(WebdavTags.prop);

        for (final WebdavProperty prop: unknowns) {
          xml.emptyTag(prop.getTag());
        }

        closeTag(WebdavTags.prop);
        addStatus(HttpServletResponse.SC_NOT_FOUND, null);

        closeTag(WebdavTags.propstat);
      }
    } finally {
      xml.setNotifier(null);
    }
  }

  /* ====================================================================
   *                   XmlUtil wrappers
   * ==================================================================== */

  protected Collection<Element> getChildren(final Node nd) {
    return XmlUtil.getElements(nd);
  }

  protected Element[] getChildrenArray(final Node nd) {
    return XmlUtil.getElementsArray(nd);
  }

  protected GetEntityResponse<Element> getOnlyChild(final Node nd) {
    final GetEntityResponse<Element> resp = new GetEntityResponse<>();

    try {
      resp.setEntity(XmlUtil.getOnlyElement(nd));
      return resp;
    } catch (final Throwable t) {
      return Response.error(resp, t);
    }
  }

  protected String getElementContent(final Element el) {
    return XmlUtil.getElementContent(el);
  }

  protected boolean isEmpty(final Element el) {
    return XmlUtil.isEmpty(el);
  }

  /* ==============================================================
   *                   XmlEmit wrappers
   * ============================================================== */

  protected void startEmit(final HttpServletResponse resp) {
    try {
      xml.startEmit(resp.getWriter());
    } catch (final IOException ie) {
      throw new RuntimeException(ie);
    }
  }

  /** Add a namespace
   *
   * @param val String namespace
   * @throws RuntimeException on fatal error
   */
  public void addNs(final String val) {
    if (xml.getNameSpace(val) == null) {
      xml.addNs(new NameSpace(val, null), false);
    }
  }

  /** Get a namespace abbreviation
   *
   * @param ns namespace
   * @return String abbrev
   */
  public String getNsAbbrev(final String ns) {
    return xml.getNsAbbrev(ns);
  }

  protected void openTag(final QName tag) {
    xml.openTag(tag);
  }

  protected void openTagNoNewline(final QName tag) {
    xml.openTagNoNewline(tag);
  }

  protected void closeTag(final QName tag) {
    xml.closeTag(tag);
  }

  /** Emit an empty tag
   *
   * @param tag qname
   * @throws RuntimeException on fatal error
   */
  public void emptyTag(final QName tag) {
    xml.emptyTag(tag);
  }

  /** Emit an empty tag corresponding to a node
  *
  * @param nd xml node
   * @throws RuntimeException on fatal error
  */
  public void emptyTag(final Node nd) {
    final String ns = nd.getNamespaceURI();
    final String ln = nd.getLocalName();

    emptyTag(new QName(ns, ln));
  }

  /** Emit a property
   *
   * @param tag qname
   * @param val element value
   * @throws RuntimeException on fatal error
   */
  public void property(final QName tag, final String val) {
    xml.property(tag, val);
  }

  /** Emit a property in a cdata
   *
   * @param tag qname
   * @param val element value
   * @throws RuntimeException on fatal error
   */
  public void cdataProperty(final QName tag,
                            final String attrName,
                            final String attrVal,
                            final String val) {
    if (attrName == null) {
      xml.cdataProperty(tag, val);
    } else {
      xml.openTagSameLine(tag, attrName, attrVal);
      xml.cdataValue(val);
      xml.closeTagSameLine(tag);
    }
  }

  /** Emit a property
   *
   * @param tag qname
   * @param val element value
   * @throws RuntimeException on fatal error
   */
  public void property(final QName tag, final Reader val) {
    xml.property(tag, val);
  }

  /** Emit a property with a qname value
  *
   * @param tag qname
   * @param tagVal qname
   * @throws RuntimeException on fatal error
   */
  public void propertyTagVal(final QName tag, final QName tagVal) {
    xml.propertyTagVal(tag, tagVal);
  }

  protected void flush() {
    xml.flush();
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

