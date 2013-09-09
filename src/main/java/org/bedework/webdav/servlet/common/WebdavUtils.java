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

import java.util.Collection;
import javax.servlet.http.HttpServletRequest;
import org.apache.log4j.Logger;

/** Place for utility methods
 *
 *   @author Mike Douglass   douglm@bedework.edu
 */
public class WebdavUtils {
  /** Get the prefix from the request
   *
   * @param req
   * @return String prefix
   */
  public static String getUrlPrefix(HttpServletRequest req) {
    try {
      String url = req.getRequestURL().toString();

      int pos = url.indexOf(req.getContextPath());

      if (pos > 0) {
        url = url.substring(0, pos);
      }

      String context = req.getContextPath();
      if ((context == null) || (context.equals("."))) {
        context = "";
      }

      return url + context;
    } catch (Throwable t) {
      Logger.getLogger(WebdavUtils.class).warn(
          "Unable to get url from " + req);
      return "BogusURL.this.is.probably.a.portal";
    }
  }

  /**
   * @param c
   * @return boolean true for empty or null Collection
   */
  public static boolean emptyCollection(Collection c) {
    return (c == null) || (c.size() == 0);
  }
}

