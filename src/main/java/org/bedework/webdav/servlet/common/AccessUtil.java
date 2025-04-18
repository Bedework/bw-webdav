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
package org.bedework.webdav.servlet.common;

import org.bedework.access.AccessXmlUtil;
import org.bedework.util.xml.XmlEmit;

import javax.xml.namespace.QName;

/**
 * @author douglm
 *
 */
public class AccessUtil extends AccessXmlUtil {
  private final String namespacePrefix;

  /** Acls use tags in the webdav and caldav namespace. For use over caldav
   * we should supply the uris. Otherwise, a null namespace will be used.
   *
   * @param namespacePrefix String prefix
   * @param xml   XmlEmit
   * @param cb callback
   */
  public AccessUtil(final String namespacePrefix,
                    final XmlEmit xml,
                    final AccessXmlCb cb) {
    super(caldavPrivTags, xml, cb);

    this.namespacePrefix = namespacePrefix;
  }

  /** Override this to construct urls from the parameter
   *
   * @param who String
   * @return String href
   */
  public String makeUserHref(final String who) {
    return namespacePrefix + "/principals/users/" + who;
  }

  /** Override this to construct urls from the parameter
   *
   * @param who String
   * @return String href
   */
  public String makeGroupHref(final String who) {
    return namespacePrefix + "/principals/groups/" + who;
  }

  /**
   * @return QName[]
   */
  public QName[] getPrivTags() {
    return caldavPrivTags;
  }
}
