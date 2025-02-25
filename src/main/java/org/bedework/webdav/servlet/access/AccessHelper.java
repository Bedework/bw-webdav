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
package org.bedework.webdav.servlet.access;

import org.bedework.access.Access;
import org.bedework.access.AccessPrincipal;
import org.bedework.access.Ace;
import org.bedework.access.AceWho;
import org.bedework.access.Acl;
import org.bedework.access.CurrentAccess;
import org.bedework.access.PrivilegeSet;
import org.bedework.util.logging.BwLogger;
import org.bedework.util.logging.Logged;
import org.bedework.webdav.servlet.shared.WebdavException;
import org.bedework.webdav.servlet.shared.WebdavForbidden;

import java.util.Collection;
import java.util.TreeSet;

/** An access helper class. This class makes some assumptions about the
 * classes it deals with but there are no explicit hibernate, or other
 * persistence engine, dependencies.
 *
 * <p>It assumes access to the parent object when needed,
 * continuing on up to the root. For systems which do not allow for a
 * retrieval of the parent on calls to the getCalendar method, the getParent
 * method for this class will need to be overridden. This would presumably
 * take place within the core implementation.
 *
 * @author Mike Douglass
 */
public class AccessHelper implements Logged, AccessHelperI {
  /** For evaluating access control
   */
  private Access access;

  private boolean superUser;

  private AccessPrincipal authPrincipal;

  private CallBack cb;

  /* Null allows all accesses according to user - otherwise restricted to this. */
  private PrivilegeSet maxAllowedPrivs;

  @Override
  public void init(final CallBack cb) {
    this.cb = cb;
    access = new Access();
  }

  @Override
  public void setSuperUser(final boolean val) {
    superUser = val;
  }

  @Override
  public boolean getSuperUser() {
    return superUser;
  }

  /**
   * @param val priv set
   */
  @SuppressWarnings("unused")
  public void setMaximumAllowedPrivs(final PrivilegeSet val) {
    maxAllowedPrivs = val;
  }

  @Override
  public void setAuthPrincipal(final AccessPrincipal val) {
    authPrincipal = val;
  }

  @Override
  public void open() {
  }

  @Override
  public void close() {
    //pathInfoMap.flush();
  }

  @Override
  public SharedEntity getParent(final SharedEntity val) {
    if (val.getParentPath() == null) {
      return null;
    }

    return cb.getCollection(val.getParentPath());
  }

  /* ====================================================================
   *                   Access control
   * ==================================================================== */

  @Override
  public String getDefaultPublicAccess() {
    return Access.getDefaultPublicAccess();
  }

  @Override
  public String getDefaultPersonalAccess() {
    return Access.getDefaultPersonalAccess();
  }

  @Override
  public void changeAccess(final SharedEntity ent,
                           final Collection<Ace> aces,
                           final boolean replaceAll) {
    try {
      final Acl acl = checkAccess(ent, privWriteAcl, false).getAcl();

      final Collection<Ace> allAces;
      if (replaceAll) {
        allAces = aces;
      } else {
        allAces = acl.getAces();
        allAces.addAll(aces);
      }


      ent.setAccess(new Acl(allAces).encodeStr());

//      pathInfoMap.flush();
    } catch (final WebdavException cfe) {
      throw cfe;
    } catch (final Throwable t) {
      throw new WebdavException(t);
    }
  }

  @Override
  public void defaultAccess(final SharedEntity ent,
                            final AceWho who) {
    try {
      final Acl acl = checkAccess(ent, privWriteAcl, false).getAcl();

      /* Now remove any access */

      if (acl.removeWho(who) != null) {
        ent.setAccess(acl.encodeStr());

//        pathInfoMap.flush();
      }
    } catch (final WebdavException cfe) {
      throw cfe;
    } catch (final Throwable t) {
      throw new WebdavException(t);
    }
  }

  @Override
  public Collection<? extends SharedEntity>
                checkAccess(final Collection<? extends SharedEntity> ents,
                                final int desiredAccess,
                                final boolean alwaysReturn) {
    final TreeSet<SharedEntity> out = new TreeSet<>();

    for (final SharedEntity sdbe: ents) {
      if (checkAccess(sdbe, desiredAccess, alwaysReturn).getAccessAllowed()) {
        out.add(sdbe);
      }
    }

    return out;
  }

  @Override
  public CurrentAccess checkAccess(final SharedEntity ent,
                                   final int desiredAccess,
                        final boolean alwaysReturnResult) {
    if (ent == null) {
      return null;
    }

    AccessState as = ent.getAccessState();

    if (as != null) {
      final CurrentAccess ca = as.getCurrentAccess(desiredAccess);

      if (ca != null) {
        // Checked already

        if (!ca.getAccessAllowed() && !alwaysReturnResult) {
          throw new WebdavForbidden();
        }

        return ca;
      }
    }

    /*
    if (debug()) {
      String cname = ent.getClass().getName();
      String ident;
      if (ent instanceof DbCollection) {
        ident = ((DbCollection)ent).getPath();
      } else {
        ident = String.valueOf(ent.getId());
      }
      getLog().debug("Check access for object " +
                     cname.substring(cname.lastIndexOf(".") + 1) +
                     " ident=" + ident +
                     " desiredAccess = " + desiredAccess);
    }
    */

    try {
      CurrentAccess ca = null;

      final AccessPrincipal owner = cb.getPrincipal(ent.getOwnerHref());
      PrivilegeSet maxPrivs = null;

      final char[] aclChars;

      if (ent.isCollection()) {
        final String path = ent.getPath();

        /* Special case the access to the user root e.g /user and
         * the 'home' directory, e.g. /user/douglm
         */

        if (!getSuperUser()) {
          if (cb.getUserHomeRoot().equals(path)) {
            //ca = new CurrentAccess();

            ca = Acl.defaultNonOwnerAccess;
          } else if (path.equals(cb.getUserHomeRoot() + owner.getAccount() + "/")){
            // Accessing user home directory
            // Set the maximumn access

            maxPrivs = PrivilegeSet.userHomeMaxPrivileges;
          }
        }
      }

      if (maxPrivs == null) {
        maxPrivs = maxAllowedPrivs;
      } else if (maxAllowedPrivs != null) {
        maxPrivs = PrivilegeSet.filterPrivileges(maxPrivs, maxAllowedPrivs);
      }

      if (ca == null) {
        /* Not special. getAclChars provides merged access for the current
         * entity.
         */
        aclChars = getAclChars(ent);

        if (debug()) {
          debug("aclChars = " + new String(aclChars));
        }

        if (desiredAccess == privAny) {
          ca = access.checkAny(cb, authPrincipal, owner, aclChars, maxPrivs);
        } else if (desiredAccess == privRead) {
          ca = access.checkRead(cb, authPrincipal, owner, aclChars, maxPrivs);
        } else if (desiredAccess == privWrite) {
          ca = access.checkReadWrite(cb, authPrincipal, owner, aclChars, maxPrivs);
        } else {
          ca = access.evaluateAccess(cb, authPrincipal, owner, desiredAccess, aclChars,
                                     maxPrivs);
        }
      }

      if ((authPrincipal != null) && superUser) {
        // Nobody can stop us - BWAAA HAA HAA

        /* Override rather than just create a readable access as code further
         * up expects a valid filled in object.
         */
        if (debug() && !ca.getAccessAllowed()) {
          debug("Override for superuser");
        }
        ca = Acl.forceAccessAllowed(ca);
      }

      if (ent.isCollection()) {
        if (as == null) {
          as = new AccessState(ent);
          ent.setAccessState(as);
        }

        as.setCurrentAccess(ca, desiredAccess);
      }

      if (!ca.getAccessAllowed() && !alwaysReturnResult) {
        throw new WebdavForbidden();
      }

      return ca;
    } catch (final WebdavException cfe) {
      throw cfe;
    } catch (final Throwable t) {
      throw new WebdavException(t);
    }
  }

  /* ==============================================================
   *                   Private methods
   * ============================================================== */

  /* If the entity is not a collection we merge the access in with the container
   * access then return the merged aces. We do this because we call getParentPathInfo
   * with a collection entity. That method will recurse up to the root.
   *
   * For a collection we just use the access for the collection itself.
   *
   * The collection access might be cached in the pathInfoTable.
   */
  private char[] getAclChars(final SharedEntity ent) {
    final SharedEntity container;

    if (ent.isCollection()) {
      container = ent;
    } else {
      container = getParent(ent);
    }

    final String path = container.getPath();

    final String aclStr;

    /* Get access for the parent first if we have one */
    final SharedEntity parent = getParent(container);

    if (parent != null) {
      aclStr = new String(merged(getAclChars(parent),
                                 parent.getPath(),
                                 container.getAccess()));
    } else if (container.getAccess() != null) {
      aclStr = container.getAccess();
    } else {
      // At root
      throw new WebdavException("Collections must have default access set at root");
    }

    final char[] aclChars = aclStr.toCharArray();

    if (ent.isCollection()) {
      return aclChars;
    }

    /* Create a merged access string from the entity access and the
     * container access
     */

    return merged(aclChars, path, ent.getAccess());
  }

  private char[] merged(final char[] parentAccess,
                        final String path,
                        final String access) {
    try {
      Acl acl = null;

      if (access != null) {
        acl = Acl.decode(access.toCharArray());
      }

      if (acl == null) {
        acl = Acl.decode(parentAccess, path);
      } else {
        acl = acl.merge(parentAccess, path);
      }

      return acl.encodeAll();
    } catch (final Throwable t) {
      throw new WebdavException(t);
    }
  }

  /* ==============================================================
   *                   Logged methods
   * ============================================================== */

  private final BwLogger logger = new BwLogger();

  @Override
  public BwLogger getLogger() {
    if ((logger.getLoggedClass() == null) && (logger.getLoggedName() == null)) {
      logger.setLoggedClass(getClass());
    }

    return logger;
  }
}

