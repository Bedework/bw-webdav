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

/**
 * User: mike Date: 8/13/15 Time: 11:16
 */
public interface WdSysIntf {
  /**
   * @return UrlHandler object to manipulate urls.
   */
  public UrlHandler getUrlHandler();

  /**
   * @param col the collection
   * @return true if this is a collection which allows a sync-report.
   * @throws WebdavException
   */
  public boolean allowsSyncReport(WdCollection<?> col) throws WebdavException;

  /** Return default content type for this service.
   *
   * @return String - never null.
   * @throws WebdavException
   */
  public String getDefaultContentType();

  /** Return notification URL for this principal.
   *
   * @return String - null for no notifications.
   * @throws WebdavException
   */
  public String getNotificationURL() throws WebdavException;
}
