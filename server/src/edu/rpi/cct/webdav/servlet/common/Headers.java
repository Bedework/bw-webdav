/* **********************************************************************
    Copyright 2005 Rensselaer Polytechnic Institute. All worldwide rights reserved.

    Redistribution and use of this distribution in source and binary forms,
    with or without modification, are permitted provided that:
       The above copyright notice and this permission notice appear in all
        copies and supporting documentation;

        The name, identifiers, and trademarks of Rensselaer Polytechnic
        Institute are not used in advertising or publicity without the
        express prior written permission of Rensselaer Polytechnic Institute;

    DISCLAIMER: The software is distributed" AS IS" without any express or
    implied warranty, including but not limited to, any implied warranties
    of merchantability or fitness for a particular purpose or any warrant)'
    of non-infringement of any current or pending patent rights. The authors
    of the software make no representations about the suitability of this
    software for any particular purpose. The entire risk as to the quality
    and performance of the software is with the user. Should the software
    prove defective, the user assumes the cost of all necessary servicing,
    repair or correction. In particular, neither Rensselaer Polytechnic
    Institute, nor the authors of the software are liable for any indirect,
    special, consequential, or incidental damages related to the software,
    to the maximum extent the law permits.
*/

package edu.rpi.cct.webdav.servlet.common;

import edu.rpi.cct.webdav.servlet.shared.WebdavBadRequest;
import edu.rpi.cct.webdav.servlet.shared.WebdavException;

import org.apache.log4j.Logger;

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
   * @param debug
   */
  public static void makeLocation(final HttpServletResponse resp,
                                  final String url, final boolean debug) {
    if (debug) {
      Logger.getLogger(Headers.class).debug("Location:" + url);
    }
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

