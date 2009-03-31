/* **********************************************************************
    Copyright 2009 Rensselaer Polytechnic Institute. All worldwide rights reserved.

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
package edu.rpi.cct.webdav.servlet.shared;

import edu.rpi.cmt.access.AccessPrincipal;
import edu.rpi.sss.util.DateTimeUtil;
import edu.rpi.sss.util.Util;

import java.util.Date;

/** Class to represent an entity in WebDAV
 *
 * @author douglm
 *
 */
public abstract class WdEntity implements Comparable<WdEntity> {
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

  /** Ensure uniqueness - lastmod only down to second.
   */
  private int sequence;

  /** UTC datetime */
  private String prevLastmod;

  /** Ensure uniqueness - lastmod only down to second.
   */
  private int prevSequence;

  private String description;

  /** Constructor
   *
   * @throws WebdavException
   */
  public WdEntity() throws WebdavException {
    super();

    Date dt = new Date();
    setLastmod(DateTimeUtil.isoDateTimeUTC(dt));
    setCreated(DateTimeUtil.isoDateTimeUTC(dt));
  }

  /* ====================================================================
   *                      Abstract methods
   * ==================================================================== */

  /**
   * @return true if this is an alias for another entity.
   * @throws WebdavException
   */
  public abstract boolean isAlias() throws WebdavException;

  /** If isAlias() and this is null system may have to resolve the alias in some
   * way.
   *
   * @return WdEntity or null.
   * @throws WebdavException
   */
  public abstract WdEntity getAliasTarget() throws WebdavException;

  /* ====================================================================
   *                      Bean methods
   * ==================================================================== */

  /** Set the name
   *
   * @param val    String name
   * @throws WebdavException
   */
  public void setName(String val) throws WebdavException {
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
  public void setDisplayName(String val) throws WebdavException {
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
  public void setPath(String val) throws WebdavException {
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
  public void setParentPath(String val) throws WebdavException {
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
  public void setOwner(AccessPrincipal val) throws WebdavException {
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
  public void setCreated(String val) throws WebdavException {
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
  public void setLastmod(String val) throws WebdavException {
    lastmod = val;
  }

  /**
   * @return String lastmod
   * @throws WebdavException
   */
  public String getLastmod() throws WebdavException {
    return lastmod;
  }

  /** Set the sequence
   *
   * @param val    sequence number
   * @throws WebdavException
   */
  public void setSequence(int val) throws WebdavException {
    sequence = val;
  }

  /** Get the sequence
   *
   * @return int    the sequence
   * @throws WebdavException
   */
  public int getSequence() throws WebdavException {
    return sequence;
  }

  /** Prev lastmod is the saved lastmod before any changes.
   *
   * @param val
   * @throws WebdavException
   */
  public void setPrevLastmod(String val) throws WebdavException {
    prevLastmod = val;
  }

  /**
   * @return String lastmod
   * @throws WebdavException
   */
  public String getPrevLastmod() throws WebdavException {
    return prevLastmod;
  }

  /** Set the sequence
   *
   * @param val    sequence number
   * @throws WebdavException
   */
  public void setPrevSequence(int val) throws WebdavException {
    prevSequence = val;
  }

  /** Get the sequence
   *
   * @return int    the sequence
   * @throws WebdavException
   */
  public int getPrevSequence() throws WebdavException {
    return prevSequence;
  }

  /** Set the description
   *
   * @param val    String description
   * @throws WebdavException
   */
  public void setDescription(String val) throws WebdavException {
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
   * @return a value to be used for etags or ctags
   * @throws WebdavException
   */
  public String getTagValue() throws WebdavException {
    return getLastmod() + "-" + getSequence();
  }

  /**
   * @return a value to be used for etags or ctags
   * @throws WebdavException
   */
  public String getPrevTagValue() throws WebdavException {
    return getPrevLastmod() + "-" + getPrevSequence();
  }

  /**
   * @param sb
   */
  public void toStringSegment(StringBuilder sb) {
    sb.append("name=");
    sb.append(name);

    sb.append(", displayName");
    sb.append(displayName);

    sb.append(", path");
    sb.append(path);

    sb.append(", parentPath");
    sb.append(parentPath);

    sb.append(", owner");
    sb.append(owner);

    sb.append(", created");
    sb.append(created);

    sb.append(", lastmod");
    sb.append(lastmod);

    sb.append(", sequence");
    sb.append(sequence);

    sb.append(", description");
    sb.append(description);
  }

  /* ====================================================================
   *                      Object methods
   * ==================================================================== */

  public int hashCode() {
    try {
      return getPath().hashCode() * getName().hashCode();
    } catch (Throwable t) {
      throw new RuntimeException(t);
    }
  }

  public int compareTo(WdEntity that)  {
    try {
      if (this == that) {
        return 0;
      }

      return Util.cmpObjval(getPath(), that.getPath());
    } catch (Throwable t) {
      throw new RuntimeException(t);
    }
  }

  public String toString() {
    StringBuilder sb = new StringBuilder("WdEntity{");

    toStringSegment(sb);

    sb.append("}");

    return sb.toString();
  }
}
