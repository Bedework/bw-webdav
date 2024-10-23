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
public abstract class WdEntity <T> implements Comparable<WdEntity<?>> {
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
   */
  public WdEntity() {
    super();

//    Date dt = new Date();
//    setLastmod(DateTimeUtil.isoDateTimeUTC(dt));
//    setCreated(DateTimeUtil.isoDateTimeUTC(dt));
  }

  /* ==============================================================
   *                      Abstract methods
   * ============================================================== */

  /**
   * @return true if this can be shared.
   */
  public abstract boolean getCanShare();

  /**
   * @return true if this can be published.
   */
  public abstract boolean getCanPublish();

  /**
   * @return true if this is an alias for another entity.
   */
  public abstract boolean isAlias();

  /**
   * @return null if this is not an alias otherwise the uri of the target
   */
  public abstract String getAliasUri();

  /** If isAlias() then resolves the alias. Otherwise, just returns
   * the parameter.
   *
   * @return WdEntity or null.
   * @param resolveSubAlias - if true and the alias points to an alias, resolve
   *                  down to a non-alias.
   */
  public abstract T resolveAlias(final boolean resolveSubAlias);

  /** set/replace properties with the given name and value
   *
   * @param name of property
   * @param val of property
   */
  public abstract void setProperty(QName name, String val);

  /**
   * @param name of property
   * @return null if not set otherwise value of first property found with name
   */
  public abstract String getProperty(QName name);

  /* ==============================================================
   *                      Bean methods
   * ============================================================== */

  /** Set the name
   *
   * @param val    String name
   */
  public void setName(final String val) {
    name = val;
  }

  /** Get the name
   *
   * @return String   name
   */
  public String getName() {
    return name;
  }

  /** Set the display name
   *
   * @param val    String display name
   */
  public void setDisplayName(final String val) {
    displayName = val;
  }

  /** Get the display name
   *
   * @return String   display name
   */
  public String getDisplayName() {
    return displayName;
  }

  /** Set the path to this collection
   *
   * @param val    String path
   */
  public void setPath(final String val) {
    path = val;
  }

  /** Get the path
   *
   * @return String   path
   */
  public String getPath() {
    return path;
  }

  /** Set the path to this collection
   *
   * @param val    String path
   */
  public void setParentPath(final String val) {
    parentPath = val;
  }

  /** Get the path
   *
   * @return String   path
   */
  public String getParentPath() {
    return parentPath;
  }

  /**
   * @param val AccessPrincipal
   */
  public void setOwner(final AccessPrincipal val) {
    owner = val;
  }

  /**
   * @return AccessPrincipal
   */
  public AccessPrincipal getOwner() {
    return owner;
  }

  /**
   * @param val create date
   */
  public void setCreated(final String val) {
    created = val;
  }

  /**
   * @return String created
   */
  public String getCreated() {
    return created;
  }

  /**
   * @param val lastmod
   */
  public void setLastmod(final String val) {
    lastmod = val;
  }

  /**
   * @return String lastmod
   */
  public String getLastmod() {
    return lastmod;
  }

  /** Get the current etag value
   *
   * @return String    the etag
   */
  public abstract String getEtag();

  /** Get the etag value before any changes were applied
   *
   * @return String    the etag
   */
  public abstract String getPreviousEtag();

  /** Set the description
   *
   * @param val    String description
   */
  public void setDescription(final String val) {
    description = val;
  }

  /** Get the description
   *
   * @return String   description
   */
  public String getDescription() {
    return description;
  }

  /**
   * @param ts ToString object
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

  /* ==============================================================
   *                      Object methods
   * ============================================================== */

  @Override
  public int hashCode() {
    try {
      return getPath().hashCode() * getName().hashCode();
    } catch (final Throwable t) {
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
    final ToString ts = new ToString(this);

    toStringSegment(ts);

    return ts.toString();
  }
}
