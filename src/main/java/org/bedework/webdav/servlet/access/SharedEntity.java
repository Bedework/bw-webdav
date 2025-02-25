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


/** Entities implementing this support access control as implemented by the access
 * utilities.
 *
 * <p>The owner href (ownerHref), encoded acl (access) and parent path must be
 * persisted.
 *
 *  <p>isCollection is usually derived.
 *
 * <p>path may be persisted or derived.
 *
 * <p>accessState is a transient object allowing us to calculate the access and
 * possibly cache that calculation.
 *
 *
 * @author Mike Douglass
 * @version 1.0
 */
public interface SharedEntity {
  /**
   *
   * @param val  Href of owner
   */
  void setOwnerHref(String val);

  /** Href of owner
   *
   * @return String  Href of owner
   */
  String getOwnerHref();

  /**
   *
   * @param val Encoded acl
   */
  void setAccess(String val);

  /** Encoded acl
   *
   * @return String
   */
  String getAccess();

  /** Access is inherited from the parent
   *
   * @param val parent path
   */
  void setParentPath(String val);

  /** Access is inherited from the parent
   *
   * @return parentPath.
   */
  String getParentPath();

  /**
   * @return the full path of this entity (parentPath + "/" + name)
   */
  String getPath();

  /** We only try to preserve access state in collections. There will be many more
   * entity objects (events, cards etc) than collections. In addition, most
   * entity objects don't have specific access set on them so the access is
   * essentially the parent collection access.
   *
   * @return boolean true for this entity being a collection
   */
  boolean isCollection();

  /** Set the access state
   *
   * @param val the access state
   */
  void setAccessState(AccessState val);

  /**
   * @return current AccessState object or null
   */
  AccessState getAccessState();
}
