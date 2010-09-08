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

import edu.rpi.cct.webdav.servlet.shared.WebdavBadRequest;
import edu.rpi.cct.webdav.servlet.shared.WebdavException;
import edu.rpi.cct.webdav.servlet.shared.WebdavNsIntf;
import edu.rpi.cct.webdav.servlet.shared.WebdavNsNode;
import edu.rpi.cct.webdav.servlet.shared.WebdavProperty;
import edu.rpi.cct.webdav.servlet.shared.WebdavStatusCode;
import edu.rpi.sss.util.xml.XmlEmit;
import edu.rpi.sss.util.xml.XmlUtil;
import edu.rpi.sss.util.xml.XmlEmit.NameSpace;
import edu.rpi.sss.util.xml.tagdefs.WebdavTags;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.Reader;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

/** Base class for all webdav servlet methods.
 */
public abstract class MethodBase {
  protected boolean debug;

  protected boolean dumpContent;

  protected transient Logger log;

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
   * @param debug
   * @param dumpContent
   * @throws WebdavException
   */
  public void init(final WebdavNsIntf nsIntf,
                   final boolean debug,
                   final boolean dumpContent) throws WebdavException{
    this.nsIntf = nsIntf;
    this.debug = debug;
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

    resourceUri = WebdavNsIntf.getResourceUri(req);

    if (debug) {
      trace("resourceUri: " + resourceUri);
    }

    return resourceUri;
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

  /* depth must be in min-max
   */
  protected void checkDepth(final int depth, final int min, final int max) throws WebdavException {
    if ((depth < min) || (depth > max)) {
      throw new WebdavBadRequest();
    }
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

  protected void addHeaders(final HttpServletResponse resp,
                            final WebdavNsNode node) throws WebdavException {
    addDavHeader(resp, node);

    // Lisa say's we need this
    resp.addHeader("MS-Author-Via", "DAV");

    // This probably needs changes

    StringBuilder methods = new StringBuilder();
    for (String name: getNsIntf().getMethodNames()) {
      if (methods.length() > 0) {
        methods.append(", ");
      }

      methods.append(name);
    }

    resp.addHeader("Allow", methods.toString());
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
    int len = req.getContentLength();
    if (len == 0) {
      return null;
    }

    try {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setNamespaceAware(true);

      DocumentBuilder builder = factory.newDocumentBuilder();

      Reader rdr = getNsIntf().getReader(req);

      if (rdr == null) {
        // No content?
        return null;
      }

      return builder.parse(new InputSource(rdr));
    } catch (SAXException e) {
      resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      throw new WebdavException(HttpServletResponse.SC_BAD_REQUEST);
    } catch (Throwable t) {
      resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
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

  /** Build the response for a single node for a propfind request
   *
   * @param node
   * @param props
   * @throws WebdavException
   */
  public void doPropFind(final WebdavNsNode node,
                         final Collection<WebdavProperty> props) throws WebdavException {
    WebdavNsIntf intf = getNsIntf();
    Collection<WebdavProperty> unknowns = new ArrayList<WebdavProperty>();
    boolean open = false;

    for (WebdavProperty pr: props) {
      if (!intf.knownProperty(node, pr)) {
        unknowns.add(pr);
      } else  {
        if (!open) {
          openTag(WebdavTags.propstat);
          openTag(WebdavTags.prop);
          open = true;
        }

        addNs(pr.getTag().getNamespaceURI());
        if (!intf.generatePropValue(node, pr, false)) {
          unknowns.add(pr);
        }
      }
    }

    if (open) {
      closeTag(WebdavTags.prop);
      addStatus(node.getStatus(), null);

      closeTag(WebdavTags.propstat);
    }


    if (!unknowns.isEmpty()) {
      openTag(WebdavTags.propstat);
      openTag(WebdavTags.prop);

      for (WebdavProperty prop: unknowns) {
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
  public void cdataProperty(final QName tag, final String val) throws WebdavException {
    try {
      xml.cdataProperty(tag, val);
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

  /** ===================================================================
   *                   Logging methods
   *  =================================================================== */

  /**
   * @return Logger
   */
  protected Logger getLogger() {
    if (log == null) {
      log = Logger.getLogger(this.getClass());
    }

    return log;
  }

  protected void debugMsg(final String msg) {
    getLogger().debug(msg);
  }

  protected void error(final Throwable t) {
    getLogger().error(this, t);
  }

  protected void error(final String msg) {
    getLogger().error(msg);
  }

  protected void warn(final String msg) {
    getLogger().warn(msg);
  }

  protected void logIt(final String msg) {
    getLogger().info(msg);
  }

  protected void trace(final String msg) {
    getLogger().debug(msg);
  }
}

