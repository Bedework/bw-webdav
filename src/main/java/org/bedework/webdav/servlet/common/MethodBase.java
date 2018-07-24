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

import org.bedework.util.misc.Logged;
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
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.Reader;
import java.net.URLDecoder;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.StringTokenizer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.ws.Holder;

/** Base class for all webdav servlet methods.
 */
public abstract class MethodBase extends Logged {
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

  private SimpleDateFormat httpDateFormatter =
      new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss ");

  /**
   * @param req
   * @param resp
   * @throws WebdavException
   */
  public abstract void doMethod(HttpServletRequest req,
                                HttpServletResponse resp)
        throws WebdavException;

  /** Allow servelt to create method.
   */
  public static class MethodInfo {
    private Class methodClass;

    private boolean requiresAuth;

    /**
     * @param methodClass
     * @param requiresAuth
     */
    public MethodInfo(final Class methodClass, final boolean requiresAuth) {
      this.methodClass = methodClass;
      this.requiresAuth = requiresAuth;
    }

    /**
     * @return Class for this method
     */
    public Class getMethodClass() {
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
   * @param nsIntf
   * @param dumpContent
   * @throws WebdavException
   */
  public void init(final WebdavNsIntf nsIntf,
                   final boolean dumpContent) throws WebdavException{
    this.nsIntf = nsIntf;
    debug = getLogger().isDebugEnabled();
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
   * @throws WebdavException
   */
  public String getResourceUri(final HttpServletRequest req)
      throws WebdavException {
    if (resourceUri != null) {
      return resourceUri;
    }

    resourceUri = getNsIntf().getResourceUri(req);

    if (debug) {
      debug("resourceUri: " + resourceUri);
    }

    return resourceUri;
  }

  /** Return a path, broken into its elements, after "." and ".." are removed.
   * If the parameter path attempts to go above the root we return null.
   *
   * Other than the backslash thing why not use URI?
   *
   * @param path      String path to be fixed
   * @return String[]   fixed path broken into elements
   * @throws WebdavException
   */
  public static List<String> fixPath(final String path) throws WebdavException {
    if (path == null) {
      return null;
    }

    String decoded;
    try {
      decoded = URLDecoder.decode(path, "UTF8");
    } catch (final Throwable t) {
      throw new WebdavException("bad path: " + path);
    }

    if (decoded == null) {
      return (null);
    }

    /** Make any backslashes into forward slashes.
     */
    if (decoded.indexOf('\\') >= 0) {
      decoded = decoded.replace('\\', '/');
    }

    /** Ensure a leading '/'
     */
    if (!decoded.startsWith("/")) {
      decoded = "/" + decoded;
    }

    /** Remove all instances of '//'.
     */
    while (decoded.contains("//")) {
      decoded = decoded.replaceAll("//", "/");
    }

    /** Somewhere we may have /./ or /../
     */

    final StringTokenizer st = new StringTokenizer(decoded, "/");

    final ArrayList<String> al = new ArrayList<String>();
    while (st.hasMoreTokens()) {
      final String s = st.nextToken();

      if (s.equals(".")) {
        // ignore
      } else if (s.equals("..")) {
        // Back up 1
        if (al.size() == 0) {
          // back too far
          return null;
        }

        al.remove(al.size() - 1);
      } else {
        al.add(s);
      }
    }

    return al;
  }

  protected int defaultDepth(final int depth, final int def) {
    if (depth < 0) {
      return def;
    }

    return depth;
  }

  /* depth must have given value
   */
  protected void checkDepth(final int depth, final int val) throws WebdavException {
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

  protected void addStatus(final int status, String message) throws WebdavException {
    try {
      if (message == null) {
        message = WebdavStatusCode.getMessage(status);
      }

      property(WebdavTags.status, "HTTP/1.1 " + status + " " + message);
    } catch (WebdavException wde) {
      throw wde;
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  /* A node of null corresponds to * for an OPTIONS request
   */
  protected void addHeaders(final HttpServletRequest req,
                            final HttpServletResponse resp,
                            final WebdavNsNode node) throws WebdavException {
    addDavHeader(resp, node);

    // Lisa say's we need this
    resp.addHeader("MS-Author-Via", "DAV");

    final StringBuilder methods = new StringBuilder();
    for (final String name: getNsIntf().getMethodNames()) {
      if (methods.length() > 0) {
        methods.append(", ");
      }

      methods.append(name);
    }

    resp.addHeader("Allow", methods.toString());
  }

  public void checkServerInfo(final HttpServletRequest req,
                              final HttpServletResponse resp) throws WebdavException {
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
                              final WebdavNsNode node) throws WebdavException {
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
                                  final HttpServletResponse resp)
      throws WebdavException{
    try {
      hasBriefHeader = Headers.brief(req);

      return parseContent(req.getContentLength(), getNsIntf().getReader(req));
    } catch (WebdavException we) {
      throw we;
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  /** Parse a reader and return the DOM representation.
   *
   * @param len        Content length
   * @param rdr        Reader
   * @return Document  Parsed body or null for no body
   * @exception WebdavException Some error occurred.
   */
  protected Document parseContent(final int len,
                                  final Reader rdr) throws WebdavException{
    if (len == 0) {
      return null;
    }

    if (rdr == null) {
      // No content?
      return null;
    }

    try {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setNamespaceAware(true);

      DocumentBuilder builder = factory.newDocumentBuilder();

      return builder.parse(new InputSource(rdr));
    } catch (SAXException e) {
      throw new WebdavBadRequest();
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  protected String formatHTTPDate(final Timestamp val) {
    if (val == null) {
      return null;
    }

    synchronized (httpDateFormatter) {
      return httpDateFormatter.format(val) + "GMT";
    }
  }

  private class XmlNotifier extends Notifier {
    private boolean enabled;

    private Holder<Boolean> openFlag;

    XmlNotifier(final Holder<Boolean> openFlag) {
      this.openFlag = openFlag;
      enabled = true;
    }

    @Override
    public void doNotification() throws Throwable {
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
   * @param node
   * @param props
   * @throws WebdavException on fatal error
   */
  public void doPropFind(final WebdavNsNode node,
                         final Collection<WebdavProperty> props) throws WebdavException {
    final WebdavNsIntf intf = getNsIntf();
    final Collection<WebdavProperty> unknowns = new ArrayList<WebdavProperty>();

    final Holder<Boolean> openFlag = new Holder<Boolean>(Boolean.FALSE);
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
          try {
            xml.emptyTag(prop.getTag());
          } catch (Throwable t) {
            throw new WebdavException(t);
          }
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

  protected Collection<Element> getChildren(final Node nd) throws WebdavException {
    try {
      return XmlUtil.getElements(nd);
    } catch (Throwable t) {
      if (debug) {
        getLogger().error(this, t);
      }

      throw new WebdavBadRequest(t.getMessage());
    }
  }

  protected Element[] getChildrenArray(final Node nd) throws WebdavException {
    try {
      return XmlUtil.getElementsArray(nd);
    } catch (Throwable t) {
      if (debug) {
        getLogger().error(this, t);
      }

      throw new WebdavBadRequest(t.getMessage());
    }
  }

  protected Element getOnlyChild(final Node nd) throws WebdavException {
    try {
      return XmlUtil.getOnlyElement(nd);
    } catch (Throwable t) {
      if (debug) {
        getLogger().error(this, t);
      }

      throw new WebdavBadRequest(t.getMessage());
    }
  }

  protected String getElementContent(final Element el) throws WebdavException {
    try {
      return XmlUtil.getElementContent(el);
    } catch (Throwable t) {
      if (debug) {
        getLogger().error(this, t);
      }

      throw new WebdavBadRequest(t.getMessage());
    }
  }

  protected boolean isEmpty(final Element el) throws WebdavException {
    try {
      return XmlUtil.isEmpty(el);
    } catch (Throwable t) {
      if (debug) {
        getLogger().error(this, t);
      }

      throw new WebdavBadRequest(t.getMessage());
    }
  }

  /* ====================================================================
   *                   XmlEmit wrappers
   * ==================================================================== */

  protected void startEmit(final HttpServletResponse resp) throws WebdavException {
    try {
      xml.startEmit(resp.getWriter());
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  /** Add a namespace
   *
   * @param val
   * @throws WebdavException
   */
  public void addNs(final String val) throws WebdavException {
    if (xml.getNameSpace(val) == null) {
      try {
        xml.addNs(new NameSpace(val, null), false);
      } catch (IOException e) {
        throw new WebdavException(e);
      }
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

  protected void openTag(final QName tag) throws WebdavException {
    try {
      xml.openTag(tag);
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  protected void openTagNoNewline(final QName tag) throws WebdavException {
    try {
      xml.openTagNoNewline(tag);
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  protected void closeTag(final QName tag) throws WebdavException {
    try {
      xml.closeTag(tag);
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  /** Emit an empty tag
   *
   * @param tag
   * @throws WebdavException
   */
  public void emptyTag(final QName tag) throws WebdavException {
    try {
      xml.emptyTag(tag);
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  /** Emit an empty tag corresponding to a node
  *
  * @param nd
  * @throws WebdavException
  */
  public void emptyTag(final Node nd) throws WebdavException {
    String ns = nd.getNamespaceURI();
    String ln = nd.getLocalName();

    emptyTag(new QName(ns, ln));
  }

  /** Emit a property
   *
   * @param tag
   * @param val
   * @throws WebdavException
   */
  public void property(final QName tag, final String val) throws WebdavException {
    try {
      xml.property(tag, val);
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  /** Emit a property
   *
   * @param tag
   * @param val
   * @throws WebdavException
   */
  public void cdataProperty(final QName tag,
                            final String attrName,
                            final String attrVal,
                            final String val) throws WebdavException {
    try {
      if (attrName == null) {
        xml.cdataProperty(tag, val);
      } else {
        xml.openTagSameLine(tag, attrName, attrVal);
        xml.cdataValue(val);
        xml.closeTagSameLine(tag);
      }
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  /** Emit a property
   *
   * @param tag
   * @param val
   * @throws WebdavException
   */
  public void property(final QName tag, final Reader val) throws WebdavException {
    try {
      xml.property(tag, val);
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  /** Emit a property
  *
   * @param tag
   * @param tagVal
   * @throws WebdavException
   */
  public void propertyTagVal(final QName tag, final QName tagVal) throws WebdavException {
    try {
      xml.propertyTagVal(tag, tagVal);
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  protected void flush() throws WebdavException {
    try {
      xml.flush();
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }
}

