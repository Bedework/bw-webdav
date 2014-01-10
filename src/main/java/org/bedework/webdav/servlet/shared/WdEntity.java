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
import org.bedework.util.misc.ToString;
import org.bedework.util.misc.Util;

import javax.xml.namespace.QName;

/** Class to represent an entity in WebDAV
 *
 * @author douglm
 *
 * @param <T>
 */
public abstract class WdEntity <T> implements Comparable<WdEntity> {
  /** The internal name of the entity
   */
  private String name;

  private String displayName;

  /* The path up to and including this object */
  private String path;

  /* The path up to this object */
  private String parentPath;

  private AccessPrincipal owner;

  /** UTC datetime */
  private String created;

  /** UTC datetime */
  private String lastmod;

  private String description;

  /** Constructor
   *
   * @throws WebdavException
   */
  public WdEntity() throws WebdavException {
    super();

//    Date dt = new Date();
//    setLastmod(DateTimeUtil.isoDateTimeUTC(dt));
//    setCreated(DateTimeUtil.isoDateTimeUTC(dt));
  }

  /* ====================================================================
   *                      Abstract methods
   * ==================================================================== */

  /**
   * @return true if this can be shared.
   * @throws WebdavException
   */
  public abstract boolean getCanShare() throws WebdavException;

  /**
   * @return true if this can be published.
   * @throws WebdavException
   */
  public abstract boolean getCanPublish() throws WebdavException;

  /**
   * @return true if this is an alias for another entity.
   * @throws WebdavException
   */
  public abstract boolean isAlias() throws WebdavException;

  /**
   * @return null if this is not an alias otherwise the uri of the target
   * @throws WebdavException
   */
  public abstract String getAliasUri() throws WebdavException;

  /** If isAlias() then resolves the alias. Otherwise just returns the parameter.
   *
   * @return WdEntity or null.
   * @param resolveSubAlias - if true and the alias points to an alias, resolve
   *                  down to a non-alias.
   * @throws WebdavException
   */
  public abstract T resolveAlias(final boolean resolveSubAlias) throws WebdavException;

  /** set/replace properties with the given name and value
   *
   * @param name
   * @param val
   * @throws WebdavException
   */
  public abstract void setProperty(QName name, String val) throws WebdavException;

  /**
   * @param name
   * @return null if not set otherwise value of first property found with name
   * @throws WebdavException
   */
  public abstract String getProperty(QName name) throws WebdavException;

  /* ====================================================================
   *                      Bean methods
   * ==================================================================== */

  /** Set the name
   *
   * @param val    String name
   * @throws WebdavException
   */
  public void setName(final String val) throws WebdavException {
    name = val;
  }

  /** Get the name
   *
   * @return String   name
   * @throws WebdavException
   */
  public String getName() throws WebdavException {
    return name;
  }

  /** Set the display name
   *
   * @param val    String display name
   * @throws WebdavException
   */
  public void setDisplayName(final String val) throws WebdavException {
    displayName = val;
  }

  /** Get the display name
   *
   * @return String   display name
   * @throws WebdavException
   */
  public String getDisplayName() throws WebdavException {
    return displayName;
  }

  /** Set the path to this collection
   *
   * @param val    String path
   * @throws WebdavException
   */
  public void setPath(final String val) throws WebdavException {
    path = val;
  }

  /** Get the path
   *
   * @return String   path
   * @throws WebdavException
   */
  public String getPath() throws WebdavException {
    return path;
  }

  /** Set the path to this collection
   *
   * @param val    String path
   * @throws WebdavException
   */
  public void setParentPath(final String val) throws WebdavException {
    parentPath = val;
  }

  /** Get the path
   *
   * @return String   path
   * @throws WebdavException
   */
  public String getParentPath() throws WebdavException {
    return parentPath;
  }

  /**
   * @param val
   * @throws WebdavException
   */
  public void setOwner(final AccessPrincipal val) throws WebdavException {
    owner = val;
  }

  /**
   * @return AccessPrincipal
   * @throws WebdavException
   */
  public AccessPrincipal getOwner() throws WebdavException {
    return owner;
  }

  /**
   * @param val
   * @throws WebdavException
   */
  public void setCreated(final String val) throws WebdavException {
    created = val;
  }

  /**
   * @return String created
   * @throws WebdavException
   */
  public String getCreated() throws WebdavException {
    return created;
  }

  /**
   * @param val
   * @throws WebdavException
   */
  public void setLastmod(final String val) throws WebdavException {
    lastmod = val;
  }

  /**
   * @return String lastmod
   * @throws WebdavException
   */
  public String getLastmod() throws WebdavException {
    return lastmod;
  }

  /** Get the current etag value
   *
   * @return String    the etag
   * @throws WebdavException
   */
  public abstract String getEtag() throws WebdavException;

  /** Get the etag value before any changes were applied
   *
   * @return String    the etag
   * @throws WebdavException
   */
  public abstract String getPreviousEtag() throws WebdavException;

  /** Set the description
   *
   * @param val    String description
   * @throws WebdavException
   */
  public void setDescription(final String val) throws WebdavException {
    description = val;
  }

  /** Get the description
   *
   * @return String   description
   * @throws WebdavException
   */
  public String getDescription() throws WebdavException {
    return description;
  }

  /**
   * @param ts
   */
  public void toStringSegment(final ToString ts) {
    try {
      ts.append("name", getName());
      ts.append("displayName", getDisplayName());
      ts.append("path", getPath());
      ts.append("parentPath", getParentPath());
      ts.append("owner", getOwner());
      ts.append("created", getCreated());
      ts.append("lastmod", getLastmod());
      ts.append("etag", getEtag());
      ts.append("previousEtag", getPreviousEtag());
      ts.append("description", getDescription());
    } catch (Throwable t) {
      ts.append(t);
    }
  }

  /* ====================================================================
   *                      Object methods
   * ==================================================================== */

  @Override
  public int hashCode() {
    try {
      return getPath().hashCode() * getName().hashCode();
    } catch (Throwable t) {
      throw new RuntimeException(t);
    }
  }

  @Override
  public int compareTo(final WdEntity that)  {
    try {
      if (this == that) {
        return 0;
      }

      return Util.cmpObjval(getPath(), that.getPath());
    } catch (Throwable t) {
      throw new RuntimeException(t);
    }
  }

  @Override
  public String toString() {
    ToString ts = new ToString(this);

    toStringSegment(ts);

    return ts.toString();
  }
}
