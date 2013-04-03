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

package edu.rpi.cct.webdav.servlet.shared;

import java.io.Serializable;

import javax.xml.namespace.QName;

/** One of these for each property in a request.
 *
 *   @author Mike Douglass   douglm@rpi.edu
 */
public class WebdavProperty implements Serializable {
  private QName tag;
  private String pval;

  /** Constructor
   *
   * @param tag  QName name
   * @param pval String value
   */
  public WebdavProperty(QName tag,
                        String pval) {
    this.tag = tag;
    this.pval = pval;
  }

  /**
   * @param val
   */
  public void setTag(QName val) {
    tag = val;
  }

  /**
   * @return QName tage name
   */
  public QName getTag() {
    return tag;
  }

  /**
   * @param val
   */
  public void setPval(String val) {
    pval = val;
  }

  /**
   * @return STring value
   */
  public String getPval() {
    return pval;
  }
}
