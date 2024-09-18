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

import org.bedework.access.AccessException;
import org.bedework.access.AccessPrincipal;
import org.bedework.access.AccessXmlUtil;
import org.bedework.access.CurrentAccess;
import org.bedework.access.PrivilegeSet;
import org.bedework.util.logging.BwLogger;
import org.bedework.util.logging.Logged;
import org.bedework.util.misc.Util;
import org.bedework.util.xml.XmlEmit;
import org.bedework.util.xml.XmlUtil;
import org.bedework.util.xml.tagdefs.WebdavTags;
import org.bedework.webdav.servlet.shared.WebdavNsIntf.Content;

import org.w3c.dom.Element;

import java.io.InputStream;
import java.io.Serializable;
import java.io.StringReader;
import java.io.Writer;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.function.Supplier;

import javax.servlet.http.HttpServletResponse;
import javax.xml.namespace.QName;

/** Represents a node in the underlying namespace for which this
 * servlet is acting as a gateway. This could be a file system, a set of
 * dynamically created objects or some sort of CMS for example.
 *
 *   @author Mike Douglass   douglm   rpi.edu
 */
public abstract class WebdavNsNode implements Serializable, Logged {
  /** Does the resource exist? */
  protected boolean exists = true;

  protected WdSysIntf wdSysIntf;

  /** Uri of the node. These are relative to the root of the namespace this
   * interface represents and should start with a "/". For example, if this
   * namespace is part of a file system starting at uabc in /var/local/uabc
   * and we are referring to a directory x at /var/local/uabc/a/x then the
   * uri should be /a/x/
   */
  protected String uri;

  /** Suitable for display
   */
  //protected String name;

  protected String path;

  /** True if this node is a collection
   */
  protected boolean collection;

  /** True if this node is a user
   */
  protected boolean userPrincipal;

  /** True if this node is a group
   */
  protected boolean groupPrincipal;

  /** True if GET is allowed
   */
  protected boolean allowsGet;

  /** Can be set to indicate some sort of abnormal condition on this node,
   * e.g. no access.
   */
  protected int status = HttpServletResponse.SC_OK;

  private final static HashMap<QName, PropertyTagEntry> propertyNames =
    new HashMap<>();

  private final static Collection<QName> supportedReports = new ArrayList<>();

  /** */
  public static class PropertyTagEntry {
    /** */
    public QName tag;
    /** */
    public boolean inPropAll = false;

    /**
     * @param tag a QName
     */
    public PropertyTagEntry(final QName tag) {
      this.tag = tag;
    }

    /**
     * @param tag a QName
     * @param inPropAll
     */
    public PropertyTagEntry(final QName tag, final boolean inPropAll) {
      this.tag = tag;
      this.inPropAll = inPropAll;
    }
  }

  static {
    addPropEntry(propertyNames, WebdavTags.acl);
    // addPropEntry(propertyNames, WebdavTags.aclRestrictons, false);
    addPropEntry(propertyNames, WebdavTags.addMember, false);
    addPropEntry(propertyNames, WebdavTags.creationdate, true);
    addPropEntry(propertyNames, WebdavTags.currentUserPrincipal, true);
    addPropEntry(propertyNames, WebdavTags.currentUserPrivilegeSet);
    addPropEntry(propertyNames, WebdavTags.displayname, true);
    addPropEntry(propertyNames, WebdavTags.getcontentlanguage, true);
    addPropEntry(propertyNames, WebdavTags.getcontentlength, true);
    addPropEntry(propertyNames, WebdavTags.getcontenttype, true);
    addPropEntry(propertyNames, WebdavTags.getetag, true);
    addPropEntry(propertyNames, WebdavTags.getlastmodified, true);
    //addPropEntry(propertyNames, WebdavTags.group, false);
    //addPropEntry(propertyNames, WebdavTags.inheritedAclSet, false);
    addPropEntry(propertyNames, WebdavTags.owner);
    //addPropEntry(propertyNames, WebdavTags.principalCollectionSet, false);
    addPropEntry(propertyNames, WebdavTags.principalURL);
    addPropEntry(propertyNames, WebdavTags.resourcetype, true);
    addPropEntry(propertyNames, WebdavTags.supportedReportSet);
    addPropEntry(propertyNames, WebdavTags.supportedPrivilegeSet);
    addPropEntry(propertyNames, WebdavTags.syncToken);

    /* Supported reports */

//    supportedReports.add(WebdavTags.expandProperty);          // Version
    supportedReports.add(WebdavTags.aclPrincipalPropSet);     // Acl
    supportedReports.add(WebdavTags.principalMatch);          // Acl
    supportedReports.add(WebdavTags.principalPropertySearch); // Acl
  }

  /* ....................................................................
   *                   Alias fields
   * .................................................................... */

  /** true if this is an alias
   */
  protected boolean alias;

  protected String targetUri;

  protected UrlHandler urlHandler;

  /** Constructor
   *
   * @param sysi system interface
   * @param urlHandler - needed for building hrefs.
   * @param path - resource path
   * @param collection - true if this is a collection
   * @param uri - the uri (XXX is that the same as the path?)
   */
  public WebdavNsNode(final WdSysIntf sysi,
                      final UrlHandler urlHandler,
                      final String path,
                      final boolean collection, final String uri) {
    wdSysIntf = sysi;
    this.urlHandler = urlHandler;
    this.path = path;
    this.collection = collection;
    this.uri = uri;
  }

  /* ====================================================================
   *                   Abstract methods
   * ==================================================================== */

  /** Get the current access granted to this principal for this node.
   *
   * @return CurrentAccess
   */
  public abstract CurrentAccess getCurrentAccess();

  /** Update this node after changes.
   *
   */
  public abstract void update();

  /** Result from setting or removing property
   *
   */
  public static class SetPropertyResult {
    /** */
    public Element prop;
    /** */
    public int status = HttpServletResponse.SC_OK;
    /** */
    public String message;

    /** */
    public QName rootElement;

    /**
     * @param prop the property element
     * @param rootElement allow nodes to determine what is trying to set things
     */
    public SetPropertyResult(final Element prop,
                             final QName rootElement) {
      this.prop = prop;
      this.rootElement = rootElement;
    }
  }

  /** Trailing "/" on uri?
   *
   * @return boolean
   */
  public abstract boolean trailSlash();

  /** Return a collection of children objects. For example, this is object
   * represents a folder, they may be file objects or a mix of file and folder
   * objects. These are not node objects.
   *
   * <p>Default is to return null
   *
   * @param filterGetter non-null if there is a filter
   * @return Collection of children
   */
  public abstract Collection<? extends WdEntity<?>> getChildren(
          Supplier<Object> filterGetter);

  /**
   * @return String
   */
  public String getPath() {
    return path;
  }

  /* ====================================================================
   *                   Property methods
   * ==================================================================== */

  /**
   * @param xml emitter
   */
  public void generateHref(final XmlEmit xml) {
    try {
      generateUrl(xml, WebdavTags.href, uri, getExists());
//      String url = getUrlPrefix() + new URI(getEncodedUri()).toASCIIString();
//      xml.property(WebdavTags.href, url);
    } catch (WebdavException wde) {
      throw wde;
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  /**
   * @param xml emitter
   * @param uri
   */
  public void generateHref(final XmlEmit xml, final String uri) {
    generateUrl(xml, WebdavTags.href, uri, false);
  }

  /**
   * @return this nodes fully prefixed uri
   */
  public String getPrefixedUri() {
    return urlHandler.prefix(uri);
  }

  /**
   * @param uri
   * @return fully prefixed uri
   */
  public String getPrefixedUri(final String uri) {
    return urlHandler.prefix(uri);
  }

  /**
   * @param xml emitter
   * @param tag
   * @param uri
   * @param exists - true if we KNOW it exists
   */
  public void generateUrl(final XmlEmit xml,
                          final QName tag,
                          final String uri,
                          final boolean exists) {
    try {
      /*
      String enc = new URI(null, null, uri, null).toString();
      enc = new URI(enc).toASCIIString();  // XXX ???????

      StringBuilder sb = new StringBuilder();

      if (!relativeUrls) {
        sb.append(getUrlPrefix());
      }

//      if (!enc.startsWith("/")) {
//        sb.append("/");
//      }
      if (getExists()) {
        if (enc.endsWith("/")) {
          if (!trailSlash()) {
            enc = enc.substring(0, enc.length() - 1);
          }
        } else {
          if (trailSlash()) {
            enc = enc + "/";
          }
        }
      }

      if (!enc.startsWith("/")) {
        if ((sb.length() == 0) || (sb.charAt(sb.length() - 1) != '/')) {
          sb.append("/");
        }
      }

      sb.append(enc);
      xml.property(tag, sb.toString());
      */
      String prefixed = getPrefixedUri(uri);

      if (exists) {
        if (prefixed.endsWith("/")) {
          if (!trailSlash()) {
            prefixed = prefixed.substring(0, prefixed.length() - 1);
          }
        } else {
          if (trailSlash()) {
            prefixed = prefixed + "/";
          }
        }
      }

      xml.property(tag, prefixed);
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  /**
   */
  public static class PropVal {
    /**
     */
    public boolean notFound;

    /**
     */
    public String val;
  }

  /** Remove the given property for this node.
   *
   * @param val   Element defining property to remove
   * @param spr   Holds reult of removing property
   * @return boolean  true if property recognized.
   */
  public boolean removeProperty(final Element val,
                                final SetPropertyResult spr) {
    try {
      if (XmlUtil.nodeMatches(val, WebdavTags.getetag)) {
        spr.status = HttpServletResponse.SC_FORBIDDEN;
        return true;
      }

      if (XmlUtil.nodeMatches(val, WebdavTags.getlastmodified)) {
        spr.status = HttpServletResponse.SC_FORBIDDEN;
        return true;
      }

      return false;
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  /** Set the given property for this node.
   *
   * @param val   Element defining property to set
   * @param spr   Holds result of setting property
   * @return boolean  true if property recognized and processed.
   */
  public boolean setProperty(final Element val,
                             final SetPropertyResult spr) {
    try {
      QName tag = new QName(val.getNamespaceURI(),
                            val.getLocalName());

      if (tag.equals(WebdavTags.getetag)) {
        spr.status = HttpServletResponse.SC_FORBIDDEN;
        return true;
      }

      if (tag.equals(WebdavTags.getlastmodified)) {
        spr.status = HttpServletResponse.SC_FORBIDDEN;
        return true;
      }

      return false;
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  /** Return true if a call to generatePropertyValue will return a value.
   *
   * @param tag
   * @return boolean
   */
  public boolean knownProperty(final QName tag) {
    return propertyNames.get(tag) != null;
  }

  /** Emit the property indicated by the tag.
   *
   * @param tag  QName defining property
   * @param intf WebdavNsIntf
   * @param allProp    true if we're doing allprop
   * @return boolean   true if emitted
   */
  public boolean generatePropertyValue(final QName tag,
                                       final WebdavNsIntf intf,
                                       final boolean allProp) {
    final String ns = tag.getNamespaceURI();
    final XmlEmit xml = intf.getXmlEmit();

    if (!ns.equals(WebdavTags.namespace)) {
      // Not ours

      return false;
    }

    try {
      if (tag.equals(WebdavTags.acl)) {
        // access 5.4
        intf.emitAcl(this);
        return true;
      }

      if (tag.equals(WebdavTags.addMember)) {
        xml.openTag(tag);

        generateHref(xml,
                     Util.buildPath(false, uri, "/",
                                    intf.getAddMemberSuffix()));

        xml.closeTag(tag);

        return true;
      }

      if (tag.equals(WebdavTags.creationdate)) {
        // dav 13.1

        final String val = getCreDate();
        if (val == null) {
          return true;
        }

        xml.property(tag, val);
        return true;
      }

      if (tag.equals(WebdavTags.currentUserPrincipal)) {
        // draft-sanchez-webdav-current-principal-01

        xml.openTag(tag);
        if (intf.getAccount() == null) {
          xml.emptyTag(WebdavTags.unauthenticated);
        } else {
          String href = intf.makeUserHref(intf.getAccount());
          if (!href.endsWith("/")) {
            href += "/";
          }
          xml.property(WebdavTags.href, href);
        }
        xml.closeTag(tag);

        return true;
      }

      if (tag.equals(WebdavTags.currentUserPrivilegeSet)) {
        // access 5.3
        final CurrentAccess ca = getCurrentAccess();
        if (ca == null) {
          xml.emptyTag(tag);
          return true;
        }

        final PrivilegeSet ps = ca.getPrivileges();
        final char[] privileges = ps.getPrivileges();

        AccessXmlUtil.emitCurrentPrivSet(xml,
                                         intf.getAccessUtil().getPrivTags(),
                                         privileges);

        return true;
      }

      if (tag.equals(WebdavTags.displayname)) {
        // dav 13.2
        xml.property(tag, getDisplayname());

        return true;
      }

      if (tag.equals(WebdavTags.getcontentlanguage)) {
        // dav 13.3
        if (!getAllowsGet()) {
          return true;
        }
        xml.property(tag, String.valueOf(getContentLang()));
        return true;
      }

      if (tag.equals(WebdavTags.getcontentlength)) {
        // dav 13.4
        if (!getAllowsGet()) {
          xml.property(tag, "0");
          return true;
        }
        xml.property(tag, String.valueOf(getContentLen()));
        return true;
      }

      if (tag.equals(WebdavTags.getcontenttype)) {
        // dav 13.5
        if (!getAllowsGet()) {
          return true;
        }

        final String val = getContentType();
        if (val == null) {
          return true;
        }

        xml.property(tag, val);
        return true;
      }

      if (tag.equals(WebdavTags.getetag)) {
        // dav 13.6
        xml.property(tag, getEtagValue(true));
        return true;
      }

      if (tag.equals(WebdavTags.getlastmodified)) {
        // dav 13.7
        final String val = getLastmodDate();
        if (val == null) {
          return true;
        }

        xml.property(tag, val);
        return true;
      }

      if (tag.equals(WebdavTags.owner)) {
        // access 5.1
        xml.openTag(tag);
        String href = intf.makeUserHref(getOwner().getPrincipalRef());
        if (!href.endsWith("/")) {
          href += "/";
        }
        xml.property(WebdavTags.href, href);
        xml.closeTag(tag);

        return true;
      }

      if (tag.equals(WebdavTags.principalURL)) {
        xml.openTag(tag);
        generateUrl(xml, WebdavTags.href, getEncodedUri(), getExists());
        xml.closeTag(tag);

        return true;
      }

      if (tag.equals(WebdavTags.resourcetype)) {
        // dav 13.9
        if (!isPrincipal() && !isCollection()) {
          xml.emptyTag(tag);
          return true;
        }

        xml.openTag(tag);

        if (isPrincipal()) {
          xml.emptyTag(WebdavTags.principal);
        }

        if (isCollection()) {
          xml.emptyTag(WebdavTags.collection);
        }

        xml.closeTag(tag);
        return true;
      }

      if (tag.equals(WebdavTags.supportedPrivilegeSet)) {
        // access 5.2
        intf.getAccessUtil().emitSupportedPrivSet();
        return true;
      }

      if (tag.equals(WebdavTags.supportedReportSet)) {
        // versioning
        intf.emitSupportedReportSet(this);
        return true;
      }

      if (tag.equals(WebdavTags.syncToken)) {
        if (!wdSysIntf.allowsSyncReport(getCollection(false))) {
          return false;
        }
        xml.property(tag, getSyncToken());
        return true;
      }

      // Not known
      return false;
    } catch (AccessException ae) {
      throw new WebdavException(ae);
    }
  }

  /** This method is called before each setter/getter takes any action.
   * It allows the concrete implementation to defer some expensive
   * operation to just before the first call.
   *
   * @param content     boolean flag indicating if this is a content related
   *                    property - that is a property which requires fetching
   *                    and/or rendering the content
   */
  public void init(final boolean content) {
  }

  /** Return true if this represents a principal
   *
   * @return boolean
   */
  public boolean isPrincipal() {
    return userPrincipal || groupPrincipal;
  }

  /** Return a set of PropertyTagEntry defining properties this node supports.
   *
   * @return Collection of PropertyTagEntry
   */
  public Collection<PropertyTagEntry> getPropertyNames() {
    if (!isPrincipal()) {
      return propertyNames.values();
    }

    return new ArrayList<>(propertyNames.values());
  }

  /** Return a set of QName defining reports this node supports.
   *
   * @return Collection of QName
   */
  public Collection<QName> getSupportedReports() {
    final Collection<QName> res = new ArrayList<>(supportedReports);

    if (wdSysIntf.allowsSyncReport(getCollection(false))) {
      res.add(WebdavTags.syncCollection);
    }

    return res;
  }

  /**
   * @param val  boolean true if node exists
   */
  public void setExists(final boolean val) {
    exists = val;
  }

  /**
   * @return boolean true if node exists
   */
  public boolean getExists() {
    return exists;
  }

  /** Set uri
   *
   * @param val
   */
  public void setUri(final String val) {
    init(false);
    uri = val;
  }

  /** Get uri
   *
   * @return String uri
   */
  public String getUri() {
    init(false);
    return uri;
  }

  /**
   * @return String encoded uri
   */
  public String getEncodedUri() {
    return getEncodedUri(getUri());
  }

  /**
   * @param uri to be encoded
   * @return String encoded uri
   */
  public String getEncodedUri(final String uri) {
    try {
      return new URI(null, null, uri, null).toString();
    } catch (final Throwable t) {
      if (debug()) {
        error(t);
      }
      throw new WebdavBadRequest();
    }
  }

  /**
   * @return boolean true for a collection
   */
  public boolean isCollection() {
    return collection;
  }

  /**
   * @param val boolean true if node allows get
   */
  public void setAllowsGet(final boolean val) {
    allowsGet = val;
  }

  /**
   * @return true if node allows get
   */
  public boolean getAllowsGet() {
    return allowsGet;
  }

  /**
   * @param val in status
   */
  public void setStatus(final int val) {
    status = val;
  }

  /**
   * @return int sttaus
   */
  public int getStatus() {
    return status;
  }

  /**
   * @param val
   */
  public void setAlias(final boolean val) {
    init(false);
    alias = val;
  }

  /**
   * @return boolean true if an alias
   */
  public boolean getAlias() {
    init(false);
    return alias;
  }

  /**
   * @param val
   */
  public void setTargetUri(final String val) {
    init(false);
    targetUri = val;
  }

  /**
   * @return String uri
   */
  public String getTargetUri() {
    init(false);
    return targetUri;
  }

  /* UNUSED* Return a collection of property objects
   *
   * <p>Default is to return an empty Collection
   *
   * @param ns      String interface namespace.
   * @return Collection (possibly empty) of WebdavProperty objects
   * /
  public Collection<WebdavProperty> getProperties(final String ns) {
    return new ArrayList<WebdavProperty>();
  }
  */

  /** Returns the content.
   *
   * @param contentType
   * @return Content object
   */
  public Content getContent(final String contentType) {
    String cont = getContentString(contentType);

    if (cont == null) {
      return null;
    }

    Content c = new Content();

    c.rdr = new StringReader(cont);
    c.contentType = getContentType();
    c.contentLength = getContentLen();

    return c;
  }

  /** Returns an InputStream for the content.
   *
   * @return InputStream       A reader for the content.
   */
  public InputStream getContentStream() {
    return null;
  }

  /** Return string content
   *
   * @return String       content.
   */
  public String getContentString(String contentType) {
    return null;
  }

  /**
   * @param methodTag - acts as a flag for the method type
   */
  public void setDefaults(final QName methodTag) {
  }

  /* ====================================================================
   *                   Required webdav properties
   * ==================================================================== */

  /** Called during xml emission to write the content for the node.
   *
   * @param xml - if this is embedded in an xml stream
   * @param wtr - if standalone output or no xml stream initialized.
   * @param contentType desired content type or null for default.
   * @return actual contentType
   */
  public abstract String writeContent(XmlEmit xml,
                                      Writer wtr,
                                      String contentType);

  /**
   * @return boolean true if this is binary content
   */
  public abstract boolean getContentBinary();

  /**
   * @return String lang
   */
  public abstract String getContentLang();

  /**
   * @return long content length
   */
  public abstract long getContentLen();

  /** A content type of null implies no content (or we don't know)
   *
   * @return String content type
   */
  public abstract String getContentType();

  /**
   * @return String credate
   */
  public abstract String getCreDate();

  /**
   * @return String name
   */
  public abstract String getDisplayname();

  /** Entity tags are defined in RFC2068 - they are supposed to provide some
   * sort of indication the data has changed - e.g. a checksum.
   * <p>There are weak and strong tags
   *
   * <p>This methods should return a suitable value for that tag.
   *
   * @param strong
   * @return String
   */
  public abstract String getEtagValue(boolean strong);

  /**
   * @return String last mod date
   */
  public abstract String getLastmodDate();

  /** Should return a value suitable for WebdavNsIntf.makeUserHref
   *
   * @return AccessPrincipal owner
   */
  public abstract AccessPrincipal getOwner();

  /** The node may refer to a collection object which may in fact be an alias to
   * another. For deletions we want to remove the alias itself.
   *
   * <p>Move and rename are also targetted at the alias.
   *
   * <p>Other operations are probably intended to work on the underlying target
   * of the alias.
   *
   * @param deref true if we want to act upon the target of an alias.
   * @return Collection this node represents
   */
  public abstract WdCollection<?> getCollection(boolean deref);

  public abstract WdCollection<?> getImmediateTargetCollection();

  /**
   * @return true if this node allows a sync-report.
   */
  public abstract boolean allowsSyncReport();

  /**
   * @return true if this represents a deleted resource.
   */
  public abstract boolean getDeleted();

  /**
   * @return String sync-token, a URI
   */
  public abstract String getSyncToken();

  /* ********************************************************************
   *                        Protected methods
   * ******************************************************************** */

  protected static void addPropEntry(final HashMap<QName, PropertyTagEntry> propertyNames,
                                     final QName tag) {
    propertyNames.put(tag, new PropertyTagEntry(tag));
  }

  protected static void addPropEntry(final HashMap<QName, PropertyTagEntry> propertyNames,
                                     final QName tag, final boolean inAllProp) {
    propertyNames.put(tag, new PropertyTagEntry(tag, inAllProp));
  }

  /* ********************************************************************
   *                        Object methods
   * ******************************************************************** */

  @Override
  public int hashCode() {
    return uri.hashCode();
  }

  @Override
  public boolean equals(final Object o) {
    if (o == this) {
      return true;
    }

    if (!(o instanceof WebdavNsNode)) {
      return false;
    }

    WebdavNsNode that = (WebdavNsNode)o;

    return uri.equals(that.uri);
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
