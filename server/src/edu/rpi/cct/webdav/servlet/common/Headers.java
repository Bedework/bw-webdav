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
package edu.rpi.cct.webdav.servlet.common;

import edu.rpi.cct.webdav.servlet.shared.WebdavBadRequest;
import edu.rpi.cct.webdav.servlet.shared.WebdavException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Retrieve and process Webdav header values
 *
 *   @author Mike Douglass   douglm@rpi.edu
 */
public class Headers {
  /** */
  public final static int depthInfinity = Integer.MAX_VALUE;
  /** */
  public final static int depthNone = Integer.MIN_VALUE;

  /** Get the depth header
   *
   * @param req    HttpServletRequest
   * @return int   depth - depthInfinity if absent
   * @throws WebdavException
   */
  public static int depth(final HttpServletRequest req) throws WebdavException {
    return depth(req, depthNone);
  }

  /** Get the depth header
   *
   * @param req    HttpServletRequest
   * @param def    int default if no header
   * @return int   depth -
   * @throws WebdavException
   */
  public static int depth(final HttpServletRequest req,
                          final int def) throws WebdavException {
    String depthStr = req.getHeader("Depth");

    if (depthStr == null) {
      return def;
    }

    if (depthStr.equals("infinity")) {
      return depthInfinity;
    }

    if (depthStr.equals("0")) {
      return 0;
    }

    if (depthStr.equals("1")) {
      return 1;
    }

    throw new WebdavBadRequest();
  }

  /** Create a location header
   *
   * @param resp
   * @param url
   */
  public static void makeLocation(final HttpServletResponse resp,
                                  final String url) {
    resp.setHeader("Location", url);
  }

  /** Look for the If-None-Match * header
   *
   * @param req    HttpServletRequest
   * @return boolean true if present
   * @throws WebdavException
   */
  public static boolean ifNoneMatchAny(final HttpServletRequest req)
          throws WebdavException {
    String hdrStr = req.getHeader("If-None-Match");

    return "*".equals(hdrStr);
  }

  /** Look for the If-None-Match header
   *
   * @param req    HttpServletRequest
   * @return String null if not present
   * @throws WebdavException
   */
  public static String ifNoneMatch(final HttpServletRequest req)
          throws WebdavException {
    return req.getHeader("If-None-Match");
  }

  /** Look for the If-Match header
   *
   * @param req    HttpServletRequest
   * @return String null if not present
   * @throws WebdavException
   */
  public static String ifMatch(final HttpServletRequest req)
          throws WebdavException {
    return req.getHeader("If-Match");
  }

  /** Look for the If-Schedule-Tag-Match header
   *
   * @param req    HttpServletRequest
   * @return String null if not present
   * @throws WebdavException
   */
  public static String ifScheduleTagMatch(final HttpServletRequest req)
          throws WebdavException {
    return req.getHeader("If-Schedule-Tag-Match");
  }

  /** The following is instantiated for If headers
   */
  public static class IfHeaders {
    /* Only If-Match or If-None-Match are allowed not both
     */

  }
}

