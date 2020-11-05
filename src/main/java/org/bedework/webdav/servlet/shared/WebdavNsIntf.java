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
package org.bedework.webdav.servlet.shared;

import org.bedework.access.Acl;
import org.bedework.util.logging.BwLogger;
import org.bedework.util.logging.Logged;
import org.bedework.util.misc.Util;
import org.bedework.util.servlet.HttpServletUtils;
import org.bedework.util.xml.XmlEmit;
import org.bedework.util.xml.XmlEmit.NameSpace;
import org.bedework.util.xml.XmlUtil;
import org.bedework.util.xml.tagdefs.WebdavTags;
import org.bedework.webdav.servlet.common.AccessUtil;
import org.bedework.webdav.servlet.common.Headers;
import org.bedework.webdav.servlet.common.Headers.IfHeader;
import org.bedework.webdav.servlet.common.Headers.IfHeader.TagOrToken;
import org.bedework.webdav.servlet.common.Headers.IfHeaders;
import org.bedework.webdav.servlet.common.MethodBase;
import org.bedework.webdav.servlet.common.MethodBase.MethodInfo;
import org.bedework.webdav.servlet.common.WebdavServlet;
import org.bedework.webdav.servlet.common.WebdavUtils;
import org.bedework.webdav.servlet.shared.serverInfo.Feature;
import org.bedework.webdav.servlet.shared.serverInfo.ServerInfo;

import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import java.io.FilterReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PushbackReader;
import java.io.Reader;
import java.io.Serializable;
import java.io.Writer;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.StringTokenizer;
import java.util.UUID;
import java.util.function.Supplier;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.namespace.QName;

/** This acts as an interface to the underlying namespace for which this
 * servlet is acting as a gateway. This could be a file system, a set of
 * dynamically created objects or some sort of CMS for example.
 *
 * A namespace consists of a number of nodes which may be containers for
 * other nodes or leaf nodes.
 *
 * All nodes are considered identical in their capabilities, that is, a
 * non-terminal node might contain content.
 *
 * Some nodes are aliases of other nodes (e.g. symlinks in a unix file
 * system). By default these aliases will be followed.
 *
 *   @author Mike Douglass
 */
public abstract class WebdavNsIntf implements Logged, Serializable {
  protected static class SessCt {
    int sessNum;
  }

  /** Mostly to distinguish trace entries */
  protected static final SessCt session = new SessCt();
  protected int sessNum;

  protected WebdavServlet servlet;

  private HttpServletRequest req;

  /* User associated with session */
  protected String account;
  protected boolean anonymous;

  protected boolean dumpContent;

  protected XmlEmit xml;

  /** Table of methods - set by servlet
   */
  protected HashMap<String, MethodInfo> methods;

  /* Table of created methods
  private HashMap<String, MethodBase> createdMethods = new HashMap<String, MethodBase>();
   */

  /** Should we return ok status in multistatus?
   */
  boolean returnMultistatusOk = true;

  private String urlPrefix;

  private static ServerInfo serverInfo;

  /** Called before any other method is called to allow initialisation to
   * take place at the first or subsequent requests
   *
   * @param servlet the WebDAV servlet
   * @param req http request
   * @param methods    HashMap   table of method info
   * @param dumpContent true to provide a debug trace of content
   * @throws WebdavException on error
   */
  public void init(final WebdavServlet servlet,
                   final HttpServletRequest req,
                   final HashMap<String, MethodInfo> methods,
                   final boolean dumpContent) throws WebdavException {
    this.servlet = servlet;
    this.req = req;
    xml = new XmlEmit();
    this.methods = methods;
    this.dumpContent = dumpContent;

    synchronized (session) {
      session.sessNum++;
      sessNum = session.sessNum;
    }

    account = HttpServletUtils.remoteUser(req);
    anonymous = (account == null) || (account.length() == 0);
    urlPrefix = WebdavUtils.getUrlPrefix(req);

    addNamespace(xml);
  }

  /**
   * @return String
   */
  public String getAccount() {
    return account;
  }

  /**
   * @return XmlEmit xmlemitter
   */
  public XmlEmit getXmlEmit() {
    return xml;
  }

  /**
   * @return HttpServletRequest
   */
  public HttpServletRequest getRequest() {
    return req;
  }

  /** Return DAV header
   *
   * @param node may be null for *
   * @return  String
   * @throws WebdavException on error
   */
  public String getDavHeader(final WebdavNsNode node) throws WebdavException {
    if (account == null) {
      return "1, 3";
    }

    return "1, 3, access-control, extended-mkcol";
  }

  /**
   *
   * @return server info populated for basic webdav
   */
  public ServerInfo getServerInfo() {
    if (serverInfo != null) {
      return serverInfo;
    }

    serverInfo = new ServerInfo();

    serverInfo.setToken(String.valueOf(System.currentTimeMillis()));

    serverInfo.addFeature(new Feature(WebdavTags.accessControl));
    serverInfo.addFeature(new Feature(WebdavTags.addMember));
    serverInfo.addFeature(new Feature(WebdavTags.class1));
    serverInfo.addFeature(new Feature(WebdavTags.class2));
    serverInfo.addFeature(new Feature(WebdavTags.extendedMkcol));
    serverInfo.addFeature(new Feature(WebdavTags.syncCollection));

    return serverInfo;
  }

  /**
   * @param ih
   * @return false for a mismatch
   * @throws WebdavException for invalid if header
   */
  public boolean syncTokenMatch(final IfHeader ih) throws WebdavException {
    if (ih.resourceTag == null) {
      throw new WebdavException(HttpServletResponse.SC_PRECONDITION_FAILED,
                                "Bad If header - no resource tag");
    }

    if (ih.tagsAndTokens.size() != 1) {
      throw new WebdavException(HttpServletResponse.SC_PRECONDITION_FAILED,
          "Bad If header - only 1 state-token allowed");
    }

    TagOrToken tt = ih.tagsAndTokens.get(0);

    if (tt.entityTag) {
      throw new WebdavException(HttpServletResponse.SC_PRECONDITION_FAILED,
          "Bad If header - entity-tag not allowed");
    }

    String token = getSyncToken(ih.resourceTag);

    if (token == null) {
      throw new WebdavException(HttpServletResponse.SC_PRECONDITION_FAILED,
                                "Bad If header - no token for resource");
    }

    if (!token.equals(tt.value)) {
      if (debug()) {
        debug("putContent: sync-token mismatch ifheader=" + tt.value +
                 "col-token=" + token);
      }
      return false;
    }

    return true;
  }

  /** Emit some failed precondition tag
   *
   * @param errorTag
   * @param extra
   * @param xml
   */
  public void emitError(final QName errorTag, final String extra,
                        final XmlEmit xml) {
    if (extra == null) {
      xml.emptyTag(errorTag);
    } else {
      xml.property(errorTag, extra);
    }
  }

  /**
   *
   * @return system interface
   */
  public abstract WdSysIntf getSysIntf();

  /** Get an object suitable for use in parsing acls and generating access.
   *
   * @return AccessUtil implementation.
   * @throws WebdavException on error
   */
  public abstract AccessUtil getAccessUtil() throws WebdavException;

  /** Return true if we can PUT this resource/entity
   *
   * @param node
   * @return  boolean
   * @throws WebdavException on error
   */
  public abstract boolean canPut(WebdavNsNode node) throws WebdavException;

  /**
   * @return - null if DAV:add-member not supported
   * @throws WebdavException on error
   */
  public abstract String getAddMemberSuffix() throws WebdavException;

  /**
   * @return Collection of method names.
   */
  public Collection<String> getMethodNames() {
    return methods.keySet();
  }

  /** Return the named initialised method or null if no such method or the
   * method requires authentication and we are anonymous
   *
   * @param name  name
   * @return MethodBase object or null
   * @throws WebdavException on error
   */
  public MethodBase getMethod(String name) throws WebdavException {
    name = name.toUpperCase();

    /*
    MethodBase mb = createdMethods.get(name);
    if (mb != null) {
      return mb;
    }

    MethodInfo mi = methods.get(name);

    if ((mi == null) || getAnonymous() && mi.getRequiresAuth()) {
      return null;
    }

    try {
      mb = (MethodBase)mi.getMethodClass().newInstance();

      mb.init(this, debug, dumpContent);

      createdMethods.put(name, mb);

      return mb;
    } catch (Throwable t) {
      if (debug()) {
        error(t);
      }
      throw new WebdavException(t);
    }
    */
    MethodInfo mi = methods.get(name);

    if ((mi == null) || (getAnonymous() && mi.getRequiresAuth())) {
      return null;
    }

    try {
      MethodBase mb = (MethodBase)mi.getMethodClass().newInstance();

      mb.init(this, dumpContent);

      return mb;
    } catch (Throwable t) {
      if (debug()) {
        error(t);
      }
      throw new WebdavException(t);
    }
  }

  /**
   * @return boolean true for anon access
   */
  public boolean getAnonymous() {
    return anonymous;
  }

  /** Return the part of the href referring to the actual entity, e.g. <br>
   * for http://localhost/ucaldav/user/caluser/calendar/2656-uwcal-demouwcalendar@mysite.edu.ics
   *
   * <br>user/caluser/calendar/2656-uwcal-demouwcalendar@mysite.edu.ics
   *
   * @param href
   * @return String
   * @throws WebdavException on error
   */
  public String getUri(final String href) throws WebdavException {
    try {
      if (href == null) {
        throw new WebdavBadRequest("bad URI " + href);
      }

      String context = req.getContextPath();

      if (href.startsWith(context)) {
        return href.substring(context.length());
      }

      if (href.startsWith("/")) {
        return href;
      }

      URL url = new URL(href);

      String path = url.getPath();

      if ((path == null) || (path.length() <= 1)) {
        return path;
      }

      if (context == null) {
        return path;
      }

      if (path.indexOf(context) != 0){
        return path;
      }

      int pos = context.length();

      if (path.length() == pos) {
        return "";
      }

      if (path.charAt(pos) != '/') {
        throw new WebdavBadRequest("bad URI " + href);
      }

      return path.substring(pos);
    } catch (Throwable t) {
      if (debug()) {
        error(t);
      }
      throw new WebdavBadRequest("bad URI " + href);
    }
  }

  /** Turn val into something which can be used as a name for an entity. This
   * involves removing path delimiters such as "/".
   *
   * @param val
   * @return modified name
   */
  public String makeName(final String val) {
    if ((val == null) || (val.length() == 0)) {
      return "";
    }

    StringBuilder sb = new StringBuilder();

    for (int i = 0; i < val.length(); i++) {
      char ch = val.charAt(i);

      switch (ch) {
      case '"':
      case '/':
      case '\\':
        sb.append('-');
        break;

      case ' ':
        sb.append("-");
        break;

      default:
        if (Character.isISOControl(ch)) {
          sb.append("-");
        } else {
          sb.append(ch);
        }
      }
    }

    return sb.toString();
  }

  /**
   * @return WebdavServlet
   */
  public WebdavServlet getServlet() {
    return servlet;
  }

  /**
   * @return boolean
   */
  public boolean getReturnMultistatusOk() {
    return returnMultistatusOk;
  }

  /** Add any namespaces for xml tag names in requests and responses.
   * An abbreviation will be supplied by the servlet.

   * The name should be globally unique in a global sense so don't return
   * something like "RPI:"
   *
   * <p>Something more like "http://ahost.rpi.edu/webdav/"
   *
   * @param xml
   */
  public void addNamespace(final XmlEmit xml) {
      xml.addNs(new NameSpace(WebdavTags.namespace, "DAV"), true);
  }

  /** Return true if the system disallows directory browsing.
   *
   * @return boolean
   * @throws WebdavException on error
   */
  public abstract boolean getDirectoryBrowsingDisallowed() throws WebdavException;

  /** Called on the way out before close if there was an error.
   *
   */
  public abstract void rollback();

  /** Called on the way out to allow resources to be freed.
   *
   * @throws WebdavException on error
   */
  public abstract void close() throws WebdavException;

  /** Returns the supported locks for the supportedlock property.
   *
   * <p>To ensure these will work always provide the full namespace "DAV:"
   * for example, the result for supported exclusive and shared write locks
   * would be the string
   *
   *  "&lt;DAV:lockentry&gt;" +
   *  "  &lt;DAV:lockscope&gt;&lt;DAV:exclusive/&gt;&lt;DAV:/lockscope&gt;" +
   *  "  &lt;DAV:locktype&gt;&lt;DAV:write/&gt;&lt;DAV:/locktype&gt;" +
   *  "&lt;DAV:/lockentry&gt;" +
   *  "&lt;DAV:lockentry&gt;" +
   *  "  &lt;DAV:lockscope&gt;&lt;DAV:shared/&gt;&lt;DAV:/lockscope&gt;" +
   *  "&lt;DAV:/lockentry&gt;"
   *
   * @return String response
   */
  public abstract String getSupportedLocks();

  /** Returns true if the namespace supports access control
   *
   * @return boolean
   */
  public abstract boolean getAccessControl();

  //ENUM

  /** Must not exist */
  public static final int existanceNot = 0;

  /** Must exist. */
  public static final int existanceMust = 1;

  /** We know it exists. */
  public static final int existanceDoesExist = 2;

  /** May exist */
  public static final int existanceMay = 3;

  //ENUM

  /** Must be collection */
  public static final int nodeTypeCollection = 0;

  /** Must be entity. */
  public static final int nodeTypeEntity = 1;

  /** Must be a principal. */
  public static final int nodeTypePrincipal = 2;

  /** Unknown. */
  public static final int nodeTypeUnknown = 3;

  /** Retrieves a node by uri, following any links.
   *
   * @param uri              String decoded uri of the node to retrieve
   * @param existence        Say's something about the state of existence
   * @param nodeType         Say's something about the type of node
   * @param addMember        Called from POST with addMember
   * @return WebdavNsNode    node specified by the URI or the node aliased by
   *                         the node at the URI.
   * @throws WebdavException on error
   */
  public abstract WebdavNsNode getNode(String uri,
                                       int existence,
                                       int nodeType,
                                       boolean addMember)
      throws WebdavException;

  /** Stores/updates an object.
   *
   * @param node             node in question
   * @throws WebdavException on error
   */
  public abstract void putNode(WebdavNsNode node)
      throws WebdavException;

  /** Deletes a node from the namespace.
   *
   * @param node             node in question
   * @throws WebdavException on error
   */
  public abstract void delete(WebdavNsNode node)
      throws WebdavException;

  /** Returns the immediate children of a node.
   *
   * @param node             node in question
   * @return Collection      of WebdavNsNode children
   * @throws WebdavException on error
   */
  public abstract Collection<WebdavNsNode> getChildren(
          WebdavNsNode node,
          Supplier<Object> filterGetter) throws WebdavException;

  /** Returns the parent of a node.
   *
   * @param node             node in question
   * @return WebdavNsNode    node's parent, or null if the specified node
   *                         is the root
   * @throws WebdavException on error
   */
  public abstract WebdavNsNode getParent(WebdavNsNode node)
      throws WebdavException;

  /** Called before fetching in GET processing. ALlows implementations to add
   * some pre-processing, for instance the schedule-tag processing in CalDAV.
   *
   * <p>This method handles etag processing.
   *
   * @param req http request
   * @param resp http response
   * @param node - the node
   * @return true - just proceed otherwise status is set
   * @throws WebdavException on error
   */
  public boolean prefetch(final HttpServletRequest req,
                          final HttpServletResponse resp,
                          final WebdavNsNode node) throws WebdavException {
    String etag = Headers.ifNoneMatch(req);

    if ((etag != null) && (!node.isCollection()) &&
        (etag.equals(node.getEtagValue(true)))) {
      resp.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
      return false;
    }

    return true;
  }

  /** */
  public static class Content {
    /** Content was written directly */
    public boolean written;

    /** A reader for the content */
    public Reader rdr;

    /** For binary */
    public InputStream stream = null;

    /** */
    public String contentType;

    /** */
    public long contentLength = -1;
  }

  /** Returns a Content object for the content.
   *
   * @param req http request
   * @param resp http response
   * @param contentType      if non-null specifies the content we want
   * @param node             node in question
   * @return content.
   * @throws WebdavException on error
   */
  public abstract Content getContent(HttpServletRequest req,
                                     HttpServletResponse resp,
                                     String contentType,
                                     WebdavNsNode node)
      throws WebdavException;

  /** Returns a Content object for the binary content.
   *
   * @param node             node in question
   * @return content.
   * @throws WebdavException on error
   */
  public abstract Content getBinaryContent(WebdavNsNode node)
      throws WebdavException;

  /** Result for putContent
   */
  public static class PutContentResult {
    /** Same node or new node for creation */
    public WebdavNsNode node;

    /** True if created */
    public boolean created;

    /** True if we can emit Etag. This implies a subsequent GET will return
     * an EXACT byte-for-byte representation of the entity we just PUT.
     *
     * <p>If that is not the case this MUST be left FALSE
     */
    public boolean emitEtag = true;
  }

  /**
   *
   * @return a valid content type string
   */
  public abstract String getAcceptContentType(HttpServletRequest req) throws WebdavException;

  /** Put content for the PUT or POST methods
   *
   * @param req the request
   * @param resourceUri if not null use this otherwise obtain from request
   * @param resp the response
   * @param fromPost          POST style - create
   * @param ifHeaders         info from headers
   * @return PutContentResult result of creating
   * @throws WebdavException on error
   */
  public PutContentResult putContent(final HttpServletRequest req,
                                     final String resourceUri,
                                     final HttpServletResponse resp,
                                     final boolean fromPost,
                                     final IfHeaders ifHeaders) throws WebdavException {
    try {
      /* We get a node to represent the entity we are creating or updating. */
      final int existence;
      boolean addMember = false;

      if (ifHeaders.create) {
        existence = existanceNot;
      } else if (!fromPost) {
        existence = existanceMay;
      } else {
        /* POST is targeted at the collection */
        existence = existanceNot;
        addMember = true;
      }

      String ruri;

      if (resourceUri != null) {
        ruri = resourceUri;
      } else {
        ruri = getResourceUri(req);
      }

      if (addMember) {
        ruri = Util.buildPath(false, ruri, "/",
                              UUID.randomUUID().toString(), ".ics");
      }
      
      final WebdavNsNode node = getNode(ruri,
                                        existence,
                                        nodeTypeEntity,
                                        addMember);

      if (node == null) {
        resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
        return null;
      }

      if (!node.getAllowsGet() || !canPut(node)) {
        // If we can't GET - we can't PUT
        resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
        return null;
      }

      final PutContentResult pcr;

      String[] contentTypePars = null;
      final String contentType = req.getContentType();
      final boolean returnRep = Headers.returnRepresentation(req);
      Content c = null;

      if (contentType != null) {
        contentTypePars = contentType.split(";");
      }

      if (node.getContentBinary()) {
        pcr = putBinaryContent(req, node,
                               contentTypePars,
                               req.getInputStream(),
                               ifHeaders);
      } else {
        final Reader rdr = getReader(req);

        if (rdr == null) {
          // No content?
          resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
          return null;
        }

        pcr = putContent(req, resp, node,
                         contentTypePars,
                         rdr,
                         ifHeaders);
      }

      /* Headers have to go out before content */

      if (pcr.emitEtag) {
        resp.setHeader("ETag", node.getEtagValue(true));
      }

      if (fromPost && pcr.created) {
        resp.setHeader("Location", getLocation(pcr.node));
      }

      if (!node.getContentBinary() &&
          returnRep) {
        final String ctype = getAcceptContentType(req);

        // Try to get the content
        resp.setContentType(ctype);

        if (!pcr.emitEtag) {
          /* Do it now */
          resp.setHeader("ETag", node.getEtagValue(true));
        }

        c = getContent(req, resp, ctype, node);
      }

      if (c == null) {
        if (pcr.created) {
          resp.setStatus(HttpServletResponse.SC_CREATED);
        } else {
          resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
        }

        resp.setContentLength(0);
      } else {
        if (pcr.created) {
          resp.setStatus(HttpServletResponse.SC_CREATED);
        } else {
          resp.setStatus(HttpServletResponse.SC_OK);
        }

        if (c.contentType != null) {
          resp.setContentType(c.contentType);
        }

        if (node.getLastmodDate() != null) {
          resp.addHeader("Last-Modified", node.getLastmodDate());
        }


        if (!c.written) {
          if ((c.stream == null) && (c.rdr == null)) {
            if (debug()) {
              debug("status: " + HttpServletResponse.SC_NO_CONTENT);
            }

            resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
          } else {
            if (debug()) {
              debug("send content - length=" + c.contentLength);
            }

            if (c.stream != null) {
              streamContent(c.stream, resp.getOutputStream());
            } else {
              writeContent(c.rdr, resp.getWriter());
            }
          }
        }
      }

      return pcr;
    } catch (final WebdavForbidden wdf) {
      resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
      throw wdf;
    } catch (final WebdavException we) {
      if (debug()) {
        error(we);
      }
      throw we;
    } catch (final Throwable t) {
      if (debug()) {
        error(t);
      }
      throw new WebdavException(t);
    }
  }

  /** size of buffer used for copying content to response.
   */
  private static final int bufferSize = 4096;

  /**
   * @param in
   * @param out
   * @throws WebdavException on error
   */
  public void writeContent(final Reader in,
                           final Writer out) throws WebdavException {
    try {
      char[] buff = new char[bufferSize];
      int len;

      while (true) {
        len = in.read(buff);

        if (len < 0) {
          break;
        }

        out.write(buff, 0, len);
      }
    } catch (Throwable t) {
      throw new WebdavException(t);
    } finally {
      try {
        in.close();
      } catch (final Throwable ignored) {}
      try {
        out.close();
      } catch (final Throwable ignored) {}
    }
  }

  /**
   * @param in
   * @param out
   * @throws WebdavException on error
   */
  public void streamContent(final InputStream in,
                            final OutputStream out) throws WebdavException {
    try {
      byte[] buff = new byte[bufferSize];
      int len;

      while (true) {
        len = in.read(buff);

        if (len < 0) {
          break;
        }

        out.write(buff, 0, len);
      }
    } catch (Throwable t) {
      throw new WebdavException(t);
    } finally {
      try {
        in.close();
      } catch (Throwable t) {}
      try {
        out.close();
      } catch (Throwable t) {}
    }
  }

  /**
   * @param uri to check
   * @return normalized form
   * @throws WebdavException on error
   */
  public String normalizeUri(String uri) throws WebdavException {
    /*Remove all "." and ".." components */
    try {
      uri = new URI(null, null, uri, null).toString();

      uri = new URI(URLEncoder.encode(uri, StandardCharsets.UTF_8))
              .normalize().getPath();

      uri = URLDecoder.decode(uri, StandardCharsets.UTF_8);

      if ((uri.length() > 1) && uri.endsWith("/")) {
        uri = uri.substring(0, uri.length() - 1);
      }

      if (debug()) {
        debug("Normalized uri=" + uri);
      }

      return uri;
    } catch (final Throwable t) {
      if (debug()) {
        error(t);
      }
      throw new WebdavBadRequest("Bad uri: " + uri);
    }
  }

  /** Set the content from a Reader
   *
   * @param req
   * @param resp              For any additional headers
   * @param node              node in question.
   * @param contentTypePars   null or values from content-type header
   * @param contentRdr        Reader for content
   * @param ifHeaders         info from headers
   * @return PutContentResult result of creating
   * @throws WebdavException on error
   */
  public abstract PutContentResult putContent(HttpServletRequest req,
                                              HttpServletResponse resp,
                                              WebdavNsNode node,
                                              String[] contentTypePars,
                                              Reader contentRdr,
                                              IfHeaders ifHeaders)
      throws WebdavException;

  /** Set the content from a Stream
   *
   * @param req
   * @param node              node in question.
   * @param contentTypePars   null or values from content-type header
   * @param contentStream     Stream for content
   * @param ifHeaders         info from headers
   * @return PutContentResult result of creating
   * @throws WebdavException on error
   */
  public abstract PutContentResult putBinaryContent(HttpServletRequest req,
                                                    WebdavNsNode node,
                                                    String[] contentTypePars,
                                                    InputStream contentStream,
                                                    IfHeaders ifHeaders)
      throws WebdavException;

  /** Create a new node.
   *
   * @param node             node to create with new uri set
   * @throws WebdavException on error
   */
  public abstract void create(WebdavNsNode node)
      throws WebdavException;

  /** Creates an alias to another node.
   *
   * @param alias       alias node that should be created with uri and
   *                    targetUri set
   * @throws WebdavException on error
   */
  public abstract void createAlias(WebdavNsNode alias)
      throws WebdavException;

  /** Throw an exception if we don't want the content for mkcol.
   *
   * @param req       HttpServletRequest
   * @throws WebdavException on error
   */
  public abstract void acceptMkcolContent(HttpServletRequest req)
      throws WebdavException;

  /** Create an empty collection at the given location. Status is set on return
   *
   * @param req       HttpServletRequest
   * @param resp      HttpServletResponse
   * @param node      node to create
   * @throws WebdavException on error
   */
  public abstract void makeCollection(HttpServletRequest req,
                                      HttpServletResponse resp,
                                      WebdavNsNode node)
      throws WebdavException;

  /** Copy or move a resource at the given location to another location.
   * Status is set on return
   *
   * @param req       HttpServletRequest
   * @param resp      HttpServletResponse
   * @param from      Source
   * @param to        Destination
   * @param copy      true for copying
   * @param overwrite true to overwrite destination
   * @param depth     0 for entity, infinity for collection.
   * @throws WebdavException on error
   */
  public abstract void copyMove(HttpServletRequest req,
                                HttpServletResponse resp,
                                WebdavNsNode from,
                                WebdavNsNode to,
                                boolean copy,
                                boolean overwrite,
                                int depth) throws WebdavException;

  /** Handle a special resource uri for GET.
   * Status is set on return
   *
   * @param req       HttpServletRequest
   * @param resp      HttpServletResponse
   * @param resourceUri
   * @return boolean true if it was a special uri and is processed
   * @throws WebdavException on error
   */
  public abstract boolean specialUri(HttpServletRequest req,
                                     HttpServletResponse resp,
                                     String resourceUri) throws WebdavException;

  /**
   * @param path to collection
   * @param token sync-token or null
   * @param limit - negative for no limit on result set size
   * @param recurse - true for infinite depth
   * @return report
   * @throws WebdavException on error
   */
  public abstract WdSynchReport getSynchReport(String path,
                                               String token,
                                               int limit,
                                               boolean recurse) throws WebdavException;

  /** Used to match tokens in If header
   * @param path for collection
   * @return sync token or null
   * @throws WebdavException on error
   */
  public abstract String getSyncToken(String path) throws WebdavException;

  /* ====================================================================
   *                  Access methods
   * ==================================================================== */

  /** Given a PrincipalMatchReport returns a Collection of matching nodes.
   *
   * @param resourceUri - url to base search on.
   * @param principalUrl - url of principal or null for current user
   * @return Collection of WebdavNsNode
   * @throws WebdavException on error
   */
  public abstract Collection<WebdavNsNode> getGroups(String resourceUri,
                                                     String principalUrl)
          throws WebdavException;

  /** Given a uri returns a Collection of uris that allow search operations on
   * principals for that resource.
   *
   * @param resourceUri
   * @return Collection of String
   * @throws WebdavException on error
   */
  public abstract Collection<String> getPrincipalCollectionSet(String resourceUri)
         throws WebdavException;

  /** Given a PrincipalPropertySearch returns a Collection of matching nodes.
   *
   * @param resourceUri
   * @param pps Collection of PrincipalPropertySearch
   * @return Collection of WebdavNsNode
   * @throws WebdavException on error
   */
  public abstract Collection<? extends WebdavNsNode> getPrincipals(String resourceUri,
                                           PrincipalPropertySearch pps)
          throws WebdavException;

  /**
   * @param id
   * @return String href
   * @throws WebdavException on error
   */
  public abstract String makeUserHref(String id) throws WebdavException;

  /** TODO - make the url value configurable
   *
   * @param req http request
   * @return
   * @throws WebdavException on error
   */
  public String makeServerInfoUrl(final HttpServletRequest req)
          throws WebdavException {
    final UrlHandler uh = new UrlHandler(req, false);

    return uh.prefix("serverinfo/serverinfo.xml");
  }

  /** Object class passed around as we parse access.
   */
  public static class AclInfo {
    /** uri of object */
    public String what;

    /** Set non-null if error occurred -- see Acl 8.1.1 */
    public QName errorTag;

    /** The resulting Acl */
    public Acl acl;

    /** Constructor
     *
     * @param uri
     */
    public AclInfo(final String uri) {
      what = uri;
    }
  }

  /**
   * @param ainfo
   * @throws WebdavException on error
   */
  public abstract void updateAccess(AclInfo ainfo) throws WebdavException;

  /**
   * @param node
   * @throws WebdavException on error
   */
  public abstract void emitAcl(WebdavNsNode node) throws WebdavException;

  /** Return all the hrefs found in the access for th egiven node.
   *
   * @param node
   * @return Collection of hrefs.
   * @throws WebdavException on error
   */
  public abstract Collection<String> getAclPrincipalInfo(WebdavNsNode node)
          throws WebdavException;

  /* ====================================================================
   *                Property value methods
   * ==================================================================== */

  /**
   * @param node
   * @throws WebdavException on error
   */
  public void emitSupportedReportSet(final WebdavNsNode node) throws WebdavException {
    xml.openTag(WebdavTags.supportedReportSet);

    final Collection<QName> supportedReports = node.getSupportedReports();

    for (final QName qn: supportedReports) {
      xml.openTag(WebdavTags.supportedReport);
      xml.openTag(WebdavTags.report);
      xml.emptyTag(qn);
      xml.closeTag(WebdavTags.report);
      xml.closeTag(WebdavTags.supportedReport);
    }
    xml.closeTag(WebdavTags.supportedReportSet);
  }

  /** Open a propstat response.
   *
   */
  public void openPropstat() {
    xml.openTag(WebdavTags.propstat);
    xml.openTag(WebdavTags.prop);
  }

  /** Close a propstat response with given result.
   *
   * @param status int value
   */
  public void closePropstat(final int status) {
    xml.closeTag(WebdavTags.prop);

    if ((status != HttpServletResponse.SC_OK) ||
            getReturnMultistatusOk()) {
      xml.property(WebdavTags.status, "HTTP/1.1 " + status + " " +
              WebdavStatusCode.getMessage(status));
    }

    xml.closeTag(WebdavTags.propstat);
  }

  /** Close a propstat response with an ok result.
   *
   * @throws WebdavException on error
   */
  public void closePropstat() throws WebdavException {
    closePropstat(HttpServletResponse.SC_OK);
  }

  /** Parse a DAV:prop list of property names in any namespace.
   *
   * @param nd the props node
   * @return Collection
   * @throws WebdavException on error
   */
  public List<WebdavProperty> parseProp(final Node nd) throws WebdavException {
    final List<WebdavProperty> props = new ArrayList<>();

    final Element[] children = getChildren(nd);

    for (final Element propnode : children) {
      final String ns = propnode.getNamespaceURI();

      if (xml.getNameSpace(ns) == null) {
        xml.addNs(new NameSpace(ns, null), false);
      }

      final WebdavProperty prop = makeProp(propnode);

      if (debug()) {
        debug("prop: " + prop.getTag());
      }

      props.add(prop);
    }

    return props;
  }

  /** Override this to create namespace specific property objects.
   *
   * @param propnode node specifying proeprty
   * @return WebdavProperty
   * @throws WebdavException on error
   */
  public WebdavProperty makeProp(final Element propnode) throws WebdavException {
    final WebdavProperty wd =
            new WebdavProperty(new QName(propnode.getNamespaceURI(),
                                         propnode.getLocalName()),
                               null);
    final NamedNodeMap nnm = propnode.getAttributes();

    for (int i = 0; i < nnm.getLength(); i++) {
      final Node n = nnm.item(i);

      wd.addAttr(n.getLocalName(),
                 n.getNodeValue());
    }

    return wd;
  }

  /** Properties we can process */
  private static final QName[] knownProperties = {
    //    WebdavTags.lockdiscovery,
    //    WebdavTags.source,
    //    WebdavTags.supportedlock,
    //    WebdavTags.aclRestrictions,
    //    WebdavTags.inheritedAclSet,
    WebdavTags.principalCollectionSet,
  };

  /** Return true if a call to generatePropValue will return a value.
   *
   * @param node
   * @param pr
   * @return boolean
   */
  public boolean knownProperty(final WebdavNsNode node,
                               final WebdavProperty pr) {
    final QName tag = pr.getTag();

    for (final QName knownProperty : knownProperties) {
      if (tag.equals(knownProperty)) {
        return true;
      }
    }

    /* Try the node for a value */

    return node.knownProperty(tag);
  }

  /** Generate a response for a single webdav property. This should be overrriden
   * to handle other namespaces.
   *
   * @param node
   * @param pr
   * @param allProp   true if we're doing allprop
   * @return boolean false for unknown (or unset)
   * @throws WebdavException on error
   */
  public boolean generatePropValue(final WebdavNsNode node,
                                   final WebdavProperty pr,
                                   final boolean allProp) throws WebdavException {
    final QName tag = pr.getTag();
    final String ns = tag.getNamespaceURI();

    /* Deal with webdav properties */
    if (!ns.equals(WebdavTags.namespace)) {
      // Not ours
      //xml.emptyTag(tag);
      return false;
    }

    if (tag.equals(WebdavTags.lockdiscovery)) {
      // dav 13.8
      //xml.emptyTag(tag);
      return false;
    }

    if (tag.equals(WebdavTags.source)) {
      // dav 13.10
      //xml.emptyTag(tag);
      return false;
    }

    if (tag.equals(WebdavTags.supportedlock)) {
      // dav 13.11
      //xml.emptyTag(tag);
      return false;
    }

    if (tag.equals(WebdavTags.aclRestrictions)) {
      // access 5.5
      return false;
    }

    if (tag.equals(WebdavTags.inheritedAclSet)) {
      // access 5.6
      return false;
    }

    if (tag.equals(WebdavTags.principalCollectionSet)) {
      // access 5.7
      xml.openTag(WebdavTags.principalCollectionSet);

      for (String s: getPrincipalCollectionSet(node.getUri())) {
        xml.property(WebdavTags.href, s);
      }

      xml.closeTag(WebdavTags.principalCollectionSet);
      return true;
    }

    /* Try the node for a value */

    if (node.generatePropertyValue(tag, this, allProp)) {
      // Generated by node
      return true;
    }

    // Not known
    return false;
  }

  /** Return the complete URL describing the location of the object
   * represented by the node
   *
   * @param node             node in question
   * @return String      url
   * @throws WebdavException on error
   */
  public String getLocation(final WebdavNsNode node) throws WebdavException {
    try {
      if (debug()) {
        debug("Get url " + urlPrefix + node.getEncodedUri());
      }

      String url = urlPrefix + new URI(node.getEncodedUri()).toASCIIString();

      if (url.endsWith("/")) {
        if (!node.trailSlash()) {
          url = url.substring(0, url.length() - 1);
        }
      } else {
        if (node.trailSlash()) {
          url = url + "/";
        }
      }
      return url;
    } catch (WebdavException wde) {
      throw wde;
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  /**
   * @param status int value
   */
  public void addStatus(final int status) {
    xml.property(WebdavTags.status, "HTTP/1.1 " + status + " " +
            WebdavStatusCode.getMessage(status));
  }

  /** Get the decoded and fixed resource URI. This calls getServletPath() to
   * obtain the path information. The description of that method is a little
   * obscure in it's meaning. In a request of this form:<br><br>
   * "GET /ucaldav/user/douglm/calendar/1302064354993-g.ics HTTP/1.1[\r][\n]"<br><br>
   * getServletPath() will return <br><br>
   * /user/douglm/calendar/1302064354993-g.ics<br><br>
   * that is the context has been removed. In addition this method will URL
   * decode the path. getRequestUrl() does neither.
   *
   * @param req      Servlet request object
   * @return String  fixed up uri
   * @throws WebdavException on error
   */
  public String getResourceUri(final HttpServletRequest req)
      throws WebdavException {
    String uri = req.getPathInfo();

    if ((uri == null) || (uri.length() == 0)) {
      /* No path specified - set it to root. */
      uri = "/";
    }

    // This may not be needed.
    return fixPath(uri);
  }

  /** Return a path, beginning with a "/", after "." and ".." are removed.
   * If the parameter path attempts to go above the root we return null.
   *
   * Other than the backslash thing why not use URI?
   *
   * @param path      String path to be fixed
   * @return String   fixed path
   * @throws WebdavException on error
   */
  public static String fixPath(final String path) throws WebdavException {
    if (path == null) {
      return null;
    }

    String decoded;
    try {
      decoded = URLDecoder.decode(path, StandardCharsets.UTF_8);
    } catch (Throwable t) {
      throw new WebdavBadRequest("bad path: " + path);
    }

    if (decoded == null) {
      return (null);
    }

    /* Make any backslashes into forward slashes.
     */
    if (decoded.indexOf('\\') >= 0) {
      decoded = decoded.replace('\\', '/');
    }

    /* Ensure a leading '/'
     */
    if (!decoded.startsWith("/")) {
      decoded = "/" + decoded;
    }

    /* Remove all instances of '//'.
     */
    while (decoded.contains("//")) {
      decoded = decoded.replaceAll("//", "/");
    }

    if (!decoded.contains("/.")) {
      return decoded;
    }

    /* Somewhere we may have /./ or /../
     */

    StringTokenizer st = new StringTokenizer(decoded, "/");

    ArrayList<String> al = new ArrayList<>();
    while (st.hasMoreTokens()) {
      String s = st.nextToken();

      if (s.equals(".")) {
        continue;
      }

      if (s.equals("..")) {
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

    /* Reconstruct */
    StringBuilder sb = new StringBuilder();
    for (String s: al) {
      sb.append('/');
      sb.append(s);
    }

    return sb.toString();
  }

  private class DebugReader extends FilterReader {
    StringBuilder sb = new StringBuilder();

    /** Constructor
     * @param rdr reader
     */
    public DebugReader(final Reader rdr) {
      super(rdr);
    }

    @Override
    public void close() throws IOException {
      if (sb != null) {
        debug(sb.toString());
      }

      super.close();
    }

    @Override
    public int read() throws IOException {
      int c = super.read();

      if (c == -1) {
        if (sb != null) {
          debug(sb.toString());
          sb = null;
        }
        return c;
      }

      if (sb != null) {
        char ch = (char)c;
        if (ch == '\n') {
          debug(sb.toString());
          sb = new StringBuilder();
        } else {
          sb.append(ch);
        }
      }

      return c;
    }

    @Override
    public int read(final char[] cbuf, final int off, final int len) throws IOException {
      int res = super.read(cbuf, off, len);
      if ((res > 0) && (sb != null)) {
        sb.append(cbuf, off, res);
      }

      return res;
    }
  }

  /**
   * @param req http request
   * @return possibly wrapped reader
   * @throws WebdavException on fatal error
   */
  public Reader getReader(final HttpServletRequest req) throws WebdavException {
    try {
      Reader rdr;

      if (debug()) {
        rdr = new DebugReader(req.getReader());
      } else {
        rdr = req.getReader();
      }

      /* Wrap with a pushback reader and see if there is anything there - some
       * people are not setting the content length to 0 when there is no body */

      PushbackReader pbr = new PushbackReader(rdr);

      int c = pbr.read();

      if (c == -1) {
        // No input
        return null;
      }

      pbr.unread(c);

      return pbr;
    } catch (final IOException ie) {
      throw new WebdavException(ie);
    }
  }

  /* ====================================================================
   *                   XmlUtil wrappers
   * ==================================================================== */

  /** Get all the children if any
   *
   * @param nd
   * @return array of Element
   * @throws WebdavException on error
   */
  public Element[] getChildren(final Node nd) throws WebdavException {
    try {
      return XmlUtil.getElementsArray(nd);
    } catch (Throwable t) {
      if (debug()) {
        getLogger().error(t);
      }

      throw new WebdavBadRequest();
    }
  }

  /** We expect a single child
   *
   * @param nd
   * @return Element
   * @throws WebdavException on error
   */
  public Element getOnlyChild(final Node nd) throws WebdavException {
    try {
      return XmlUtil.getOnlyElement(nd);
    } catch (Throwable t) {
      if (debug()) {
        getLogger().error(t);
      }

      throw new WebdavBadRequest();
    }
  }

  /**
   * @param el
   * @return String
   * @throws WebdavException on error
   */
  public String getElementContent(final Element el) throws WebdavException {
    try {
      return XmlUtil.getElementContent(el);
    } catch (Throwable t) {
      if (debug()) {
        getLogger().error(t);
      }

      throw new WebdavBadRequest();
    }
  }

  /* ====================================================================
   *                        Protected methods
   * ==================================================================== */

  /** Return a String giving an HTML representation of the directory.
   *
   * TODO
   *
   * <p>Use some form of template to generate an internationalized form of the
   * listing. We don't need a great deal to start with. It will also allow us to
   * provide stylesheets, images etc. Probably place it in the resources directory.
   *
   * @param req http request
   * @param node  WebdavNsNode
   * @return Reader
   * @throws WebdavException on error
   */
  protected String generateHtml(final HttpServletRequest req,
                                final WebdavNsNode node) throws WebdavException {
    try {
      final Sbuff sb = new Sbuff();

      sb.lines(new String[] {"<html>",
                             "  <head>"});
      /* Need some styles I guess */
      sb.append("    <title>");
      sb.append(node.getDisplayname());
      sb.line("</title>");

      sb.lines(new String[] {"</head>",
                             "<body>"});

      sb.append("    <h1>");
      sb.append(node.getDisplayname());
      sb.line("</h1>");

      sb.line("  <hr>");

      sb.line("  <table width=\"100%\" " +
              "cellspacing=\"0\"" +
              " cellpadding=\"4\">");

      for (final WebdavNsNode child: getChildren(node, null)) {
        /* icon would be nice */

        sb.line("<tr>");

        if (node.isCollection()) {
          /* folder */
        } else {
          /* calendar? */
        }

        sb.line("  <td align=\"left\">");
        sb.append("<a href=\"");
        sb.append(req.getContextPath());
        sb.append(child.getUri());
        sb.append("\">");
        sb.append(child.getDisplayname());
        sb.line("</a>");
        sb.line("</td>");

        sb.line("  <td align=\"left\">");

        String lastMod = child.getLastmodDate();

        if (lastMod != null) {
          sb.line(lastMod);
        } else {
          sb.line("&nbsp;");
        }
        sb.line("</td>");
        sb.append("</tr>\r\n");
      }

      sb.line("</table>");

      /* Could use a footer */
      sb.line("</body>");
      sb.line("</html>");

      return sb.toString();
    } catch (WebdavException we) {
      throw we;
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  private static class Sbuff {
    StringBuilder sb = new StringBuilder();

    /**
     * @param ss
     */
    public void lines(final String[] ss) {
      for (int i = 0; i < ss.length; i++) {
        line(ss[i]);
      }
    }

    /**
     * @param s
     */
    public void line(final String s) {
      sb.append(s);
      sb.append("\r\n");
    }

    /**
     * @param s
     */
    public void append(final String s) {
      sb.append(s);
    }

    @Override
    public String toString() {
      return sb.toString();
    }
  }

  /* ====================================================================
   *                   Logged methods
   * ==================================================================== */

  private BwLogger logger = new BwLogger();

  @Override
  public BwLogger getLogger() {
    if ((logger.getLoggedClass() == null) && (logger.getLoggedName() == null)) {
      logger.setLoggedClass(getClass());
    }

    return logger;
  }
}
