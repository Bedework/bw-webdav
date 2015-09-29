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
package org.bedework.webdav.servlet.shared.serverInfo;

import org.bedework.util.xml.XmlEmit;

import javax.xml.namespace.QName;

/** Class to represent a server info feature. The xml
 * equivalent may be no more than a single empty element
 *
 * @author Mike Douglass douglm
 */
public class Feature {
  private QName featureName;

  public Feature(final QName featureName) {
    setFeatureName(featureName);
  }

  /**
   * @param val token
   */
  public void setFeatureName(final QName val) {
    featureName = val;
  }

  /**
   * @return current token
   */
  public QName getFeatureName() {
    return featureName;
  }

  /**
   * @param xml emitter
   * @throws Throwable
   */
  public void toXml(final XmlEmit xml) throws Throwable {
    xml.emptyTag(getFeatureName());
  }

}
