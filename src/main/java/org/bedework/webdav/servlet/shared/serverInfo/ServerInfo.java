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

import org.bedework.util.misc.Util;
import org.bedework.util.xml.XmlEmit;
import org.bedework.util.xml.XmlEmit.NameSpace;
import org.bedework.util.xml.tagdefs.AppleServerTags;
import org.bedework.util.xml.tagdefs.BedeworkServerTags;
import org.bedework.util.xml.tagdefs.CaldavDefs;
import org.bedework.util.xml.tagdefs.WebdavTags;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

/** Class to represent server info
 *
 * @author Mike Douglass douglm
 */
public class ServerInfo {
  private String token;

  private final Features features = new Features();

  private List<Application> applications;

  /**
   * @param val token
   */
  public void setToken(final String val) {
    token = val;
  }

  /**
   * @return current token
   */
  public String getToken() {
    return token;
  }

  public void addFeature(final Feature val) {
    features.addFeature(val);
  }

  public void setApplications(final List<Application> val) {
    applications = val;
  }

  public List<Application> getApplications() {
    return applications;
  }

  public void addApplication(final Application val) {
    if (applications == null) {
      applications = new ArrayList<>();
    }

    applications.add(val);
  }

  /**
   * @return XML version of server info
   * @throws Throwable
   */
  public String toXml() throws Throwable {
    final StringWriter str = new StringWriter();
    final XmlEmit xml = new XmlEmit();

    xml.addNs(new NameSpace(WebdavTags.namespace, "DAV"), false);
    xml.addNs(new NameSpace(CaldavDefs.caldavNamespace, "C"), false);
    xml.addNs(new NameSpace(AppleServerTags.appleCaldavNamespace, "CSS"), false);
    xml.addNs(new NameSpace(BedeworkServerTags.bedeworkCaldavNamespace, "BSS"), false);
    xml.addNs(new NameSpace(BedeworkServerTags.bedeworkSystemNamespace, "BSYS"), false);

    xml.startEmit(str);
    toXml(xml);

    return str.toString();
  }

  /**
   * @param xml emitter
   * @throws Throwable
   */
  public void toXml(final XmlEmit xml) throws Throwable {
    xml.openTag(WebdavTags.serverinfo);

    features.toXml(xml);

    if (!Util.isEmpty(getApplications())) {
      xml.openTag(WebdavTags.applications);

      for (final Application s: getApplications()) {
        s.toXml(xml);
      }

      xml.closeTag(WebdavTags.applications);
    }

    xml.closeTag(WebdavTags.serverinfo);
  }

}
