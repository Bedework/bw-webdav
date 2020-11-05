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
import org.bedework.util.xml.tagdefs.WebdavTags;

import java.util.ArrayList;
import java.util.List;

/** Class to represent server info
 *
 * @author Mike Douglass douglm
 */
public class Features {
  private List<Feature> features;

  public void setFeatures(final List<Feature> val) {
    features = val;
  }

  public List<Feature> getFeatures() {
    return features;
  }

  public void addFeature(final Feature val) {
    if (features == null) {
      features = new ArrayList<>();
    }

    features.add(val);
  }

  /**
   * @param xml emitter
   */
  public void toXml(final XmlEmit xml) {
    xml.openTag(WebdavTags.features);

    if (!Util.isEmpty(getFeatures())) {
      for (final Feature f: getFeatures()) {
        f.toXml(xml);
      }
    }

    xml.closeTag(WebdavTags.features);
  }

}
