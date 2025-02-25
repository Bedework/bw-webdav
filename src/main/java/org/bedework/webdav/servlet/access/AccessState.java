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

import org.bedework.access.CurrentAccess;
import org.bedework.webdav.servlet.shared.WebdavException;

import java.util.HashMap;
import java.util.Map;

/** An object to preserve the current state of access calculations. Embedding
 * this in an entity instance avoids recalculation.
 *
 * @author Mike Douglass douglm   rpi.edu
 * @version 1.0
 */
public class AccessState {
  private final SharedEntity entity;

  /* Current access for the current principal.
   */
  private CurrentAccess currentAccess;

  private final Map<Integer, CurrentAccess> caMap =
          new HashMap<>(20);

  private int lastDesiredAccess;

  /** Constructor
   *
   * @param entity current shared resource
   */
  public AccessState(final SharedEntity entity) {
    this.entity = entity;
  }

  /**
   * @return the entity
   */
  public SharedEntity fetchEntity() {
    return entity;
  }

  /* ==============================================================
   *                   Wrapper object methods
   * ============================================================== */

  /**
   */
  public void clearCurrentAccess() {
    caMap.clear();
  }

  /**
   * @return current access object
   */
  public CurrentAccess getCurrentAccess() {
    if (currentAccess != null) {
      return currentAccess;
    }

    return getCurrentAccess(AccessHelperI.privAny);
  }

  /**
   * @param desiredAccess key
   * @return currentAccess;
   */
  public CurrentAccess getCurrentAccess(final int desiredAccess) {
    if ((desiredAccess == lastDesiredAccess) &&
        (currentAccess != null)) {
      return currentAccess;
    }

    currentAccess = caMap.get(desiredAccess);
    lastDesiredAccess = desiredAccess;

    return currentAccess;
  }

  /**
   * @param ca CurrentAccess
   * @param desiredAccess used as key
   */
  public void setCurrentAccess(final CurrentAccess ca,
                               final int desiredAccess) {
    currentAccess = ca;
    lastDesiredAccess = desiredAccess;
    caMap.put(desiredAccess , ca);
  }

  /**
   * @return int last desiredAccess
   */
  public int getLastDesiredAccess() {
    return lastDesiredAccess;
  }

  /* ==============================================================
   *                   Object methods
   * ============================================================== */

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("AccessState{");

    sb.append(entity.toString());

    try {
      if (getCurrentAccess() != null) {
        sb.append(", currentAccess=");
        sb.append(getCurrentAccess());
      }
    } catch (final WebdavException cfe) {
      sb.append("exception");
      sb.append(cfe.getMessage());
    }
    sb.append("}");

    return sb.toString();
  }
}
