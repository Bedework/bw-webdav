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

//import org.bedework.carddav.util.User;

import org.bedework.access.Access.AccessCb;
import org.bedework.access.AccessPrincipal;
import org.bedework.access.Ace;
import org.bedework.access.AceWho;
import org.bedework.access.CurrentAccess;
import org.bedework.access.PrivilegeDefs;

import java.io.Serializable;
import java.util.Collection;

/** An access helper interface. This interface makes some assumptions about the
 * classes it deals with but there is no explicit hibernate, or other
 * persistence engine, dependencies.
 *
 * <p>It assumes that it has access to the parent object when needed,
 * continuing on up to the root. For systems which do not allow for a
 * retrieval of the parent on calls to the getCalendar method, the getParent
 * method for this class will need to be overridden. This would presumably
 * take place within the core implementation.
 *
 *
 * @author Mike Douglass   douglm  rpi.edu
 */
public interface AccessHelperI extends PrivilegeDefs, Serializable {
  /** Methods called to obtain system information.
   *
   */
  abstract class CallBack implements AccessCb, Serializable {
    /**
     * @param href for principal
     * @return AccessPrincipal or null for not valid
       */
    public abstract AccessPrincipal getPrincipal(String href);

    /** Returns something like "/user/". This provides what is considered to be
     * the root of the file system for which we are evaluating access.
     *
     * @return String root - must be prefixed and suffixed with "/"
       */
    public abstract String getUserHomeRoot();

    /** Get a collection given the path. No access checks are performed.
     *
     * @param  path          String path of collection
     * @return SharedEntity null for unknown collection
       */
    public abstract SharedEntity getCollection(String path);
  }

  /**
   *
   * @param cb callback
   */
  void init(CallBack cb);

  /** Indicate if we are in superuser mode.
   * @param val true for superuser
   */
  void setSuperUser(boolean val);

  /**
   * @return boolean
   */
  boolean getSuperUser();

  /** Set the current authenticated user.
   *
   * @param val principal
   */
  void setAuthPrincipal(AccessPrincipal val);

  /** Called at request start
   *
   */
  void open();

  /** Called at request end
   *
   */
  void close();

  /** Called to get the parent object for a shared entity.
   *
   * @param val entity
   * @return parent calendar or null.
   */
  SharedEntity getParent(SharedEntity val);

  /* ====================================================================
   *                   Access control
   * ==================================================================== */

  /** Get the default public access
   *
   * @return String value for default access
   */
  String getDefaultPublicAccess();

  /**
   *
   * @return String default user access
   */
  String getDefaultPersonalAccess();

  /** Change the access to the given calendar entity using the supplied aces.
   * We are changing access so we remove all access for each who in the list and
   * then add the new aces.
   *
   * @param ent        DbEntity
   * @param aces       Collection of ace objects
   * @param replaceAll true to replace the entire access list.
   */
  void changeAccess(SharedEntity ent,
                    Collection<Ace> aces,
                    boolean replaceAll);

  /** Remove any explicit access for the given who to the given calendar entity.
  *
  * @param ent      DbEntity
  * @param who      AceWho
  */
 void defaultAccess(SharedEntity ent,
                           AceWho who);

  /** Return a Collection of the objects after checking access
   *
   * @param ents          Collection of DbEntity
   * @param desiredAccess access we want
   * @param alwaysReturn boolean flag behaviour on no access
   * @return Collection   of checked objects
   */
  Collection<? extends SharedEntity>
        checkAccess(Collection<? extends SharedEntity> ents,
                    int desiredAccess,
                    boolean alwaysReturn);

  /** Check access for the given entity. Returns the current access
   *
   * <p>We special case the access to the user root e.g /user and the home
   * directory, e.g. /user/douglm
   * <p>
   * We deny access to /user to anybody without superuser access. This
   * prevents user browsing. This could be made a system property if the
   * organization wants user browsing.
   * <p>
   * Default access to the home directory is read, write-content to the owner
   * only and unlimited to superuser.
   * <p>
   * Specific access should be no more than read, write-content to the home
   * directory.
   *
   * @param ent shred entity
   * @param desiredAccess access
   * @param alwaysReturnResult true to return always
   * @return  CurrentAccess
   */
  CurrentAccess checkAccess(SharedEntity ent, int desiredAccess,
                            boolean alwaysReturnResult);
}
