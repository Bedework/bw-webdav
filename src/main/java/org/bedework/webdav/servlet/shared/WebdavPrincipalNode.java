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

import org.bedework.access.AccessPrincipal;
import org.bedework.access.Acl.CurrentAccess;
import org.bedework.access.WhoDefs;
import org.bedework.util.xml.XmlEmit;
import org.bedework.util.xml.tagdefs.AppleServerTags;
import org.bedework.util.xml.tagdefs.WebdavTags;

import org.w3c.dom.Element;

import java.io.Writer;
import java.util.Collection;
import java.util.HashMap;

import javax.xml.namespace.QName;

/** Class to represent a principal in webdav.
 *
 *
 *   @author Mike Douglass   douglm   rpi.edu
 */
public class WebdavPrincipalNode extends WebdavNsNode {
  private final AccessPrincipal account;

  private final static HashMap<QName, PropertyTagEntry> propertyNames =
    new HashMap<>();

  static {
    addPropEntry(propertyNames, WebdavTags.groupMemberSet);
    addPropEntry(propertyNames, WebdavTags.groupMembership);
    addPropEntry(propertyNames, WebdavTags.notificationURL);

    addPropEntry(propertyNames, AppleServerTags.notificationURL);
  }

  /**
   * @param sysi system interface
   * @param urlHandler - needed for building hrefs.
   * @param path - resource path
   * @param account - the principal
   * @param collection - true if this is a collection
   * @param uri of request
   * @throws WebdavException
   */
  public WebdavPrincipalNode(final WdSysIntf sysi,
                             final UrlHandler urlHandler,
                             final String path,
                             final AccessPrincipal account,
                             final boolean collection,
                             final String uri) throws WebdavException {
    super(sysi, urlHandler, path, collection, uri);
    this.account = account;
    userPrincipal = account.getKind() == WhoDefs.whoTypeUser;
    groupPrincipal = account.getKind() == WhoDefs.whoTypeGroup;
//    if (displayName.startsWith("/")) {
//      debugMsg(displayName);
//    }
  }

  @Override
  public AccessPrincipal getOwner() throws WebdavException {
    return account;
  }

  @Override
  public void update() throws WebdavException {
  }

  /* ====================================================================
   *                   Abstract methods
   * ==================================================================== */

  @Override
  public CurrentAccess getCurrentAccess() throws WebdavException {
    return null;
  }

  @Override
  public String getEtagValue(final boolean strong) throws WebdavException {
    final String val = "1234567890";

    if (strong) {
      return "\"" + val + "\"";
    }

    return "W/\"" + val + "\"";
  }

  @Override
  public boolean trailSlash() {
    return true;
  }

  @Override
  public Collection<? extends WdEntity> getChildren() throws WebdavException {
    return null;
  }

  @Override
  public WdCollection getCollection(final boolean deref) throws WebdavException {
    return null;
  }

  @Override
  public WdCollection getImmediateTargetCollection()
          throws WebdavException {
    return null;
  }

  @Override
  public boolean allowsSyncReport() throws WebdavException {
    return false;
  }

  @Override
  public boolean getDeleted() throws WebdavException {
    return false;
  }

  @Override
  public String getSyncToken() throws WebdavException {
    return null;
  }

  /* ====================================================================
   *                   Required webdav properties
   * ==================================================================== */

  @Override
  public String writeContent(final XmlEmit xml,
                             final Writer wtr,
                             final String contentType) throws WebdavException {
    return null;
  }

  @Override
  public boolean getContentBinary() throws WebdavException {
    return false;
  }

  @Override
  public String getContentLang() throws WebdavException {
    return null;
  }

  @Override
  public long getContentLen() throws WebdavException {
    return 0;
  }

  @Override
  public String getContentType() throws WebdavException {
    return null;
  }

  @Override
  public String getCreDate() throws WebdavException {
    return null;
  }

  @Override
  public String getDisplayname() throws WebdavException {
    return account.getAccount();
  }

  @Override
  public String getLastmodDate() throws WebdavException {
    return null;
  }

  /* ====================================================================
   *                   Property methods
   * ==================================================================== */

  @Override
  public boolean removeProperty(final Element val,
                                final SetPropertyResult spr) throws WebdavException {
    warn("Unimplemented - removeProperty");

    return false;
  }

  @Override
  public boolean setProperty(final Element val,
                             final SetPropertyResult spr) throws WebdavException {
    if (super.setProperty(val, spr)) {
      return true;
    }

    return false;
  }

  @Override
  public boolean knownProperty(final QName tag) {
    if (propertyNames.get(tag) != null) {
      return true;
    }

    // Not ours
    return super.knownProperty(tag);
  }

  @Override
  public boolean generatePropertyValue(final QName tag,
                                       final WebdavNsIntf intf,
                                       final boolean allProp) throws WebdavException {
    final String ns = tag.getNamespaceURI();
    final XmlEmit xml = intf.getXmlEmit();

    try {
      if (tag.equals(WebdavTags.groupMemberSet)) {
        // PROPTODO
        xml.emptyTag(tag);
        return true;
      }

      if (tag.equals(WebdavTags.groupMembership)) {
        // PROPTODO
        xml.emptyTag(tag);
        return true;
      }

      if (tag.equals(WebdavTags.notificationURL) ||
          tag.equals(AppleServerTags.notificationURL)) {
        if (wdSysIntf.getNotificationURL() == null) {
          return false;
        }

        xml.openTag(tag);
        generateHref(xml,
                     wdSysIntf.getNotificationURL());
        xml.closeTag(tag);

        return true;
      }

      // Not known - try higher
      return super.generatePropertyValue(tag, intf, allProp);
    } catch (final Throwable t) {
      throw new WebdavException(t);
    }
  }

  /* ====================================================================
   *                   Private methods
   * ==================================================================== */
}
