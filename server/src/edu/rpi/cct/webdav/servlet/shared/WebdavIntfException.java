/*
 Copyright (c) 2000-2005 University of Washington.  All rights reserved.

 Redistribution and use of this distribution in source and binary forms,
 with or without modification, are permitted provided that:

   The above copyright notice and this permission notice appear in
   all copies and supporting documentation;

   The name, identifiers, and trademarks of the University of Washington
   are not used in advertising or publicity without the express prior
   written permission of the University of Washington;

   Recipients acknowledge that this distribution is made available as a
   research courtesy, "as is", potentially with defects, without
   any obligation on the part of the University of Washington to
   provide support, services, or repair;

   THE UNIVERSITY OF WASHINGTON DISCLAIMS ALL WARRANTIES, EXPRESS OR
   IMPLIED, WITH REGARD TO THIS SOFTWARE, INCLUDING WITHOUT LIMITATION
   ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
   PARTICULAR PURPOSE, AND IN NO EVENT SHALL THE UNIVERSITY OF
   WASHINGTON BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL
   DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR
   PROFITS, WHETHER IN AN ACTION OF CONTRACT, TORT (INCLUDING
   NEGLIGENCE) OR STRICT LIABILITY, ARISING OUT OF OR IN CONNECTION WITH
   THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */
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

package edu.rpi.cct.webdav.servlet.shared;

import javax.servlet.http.HttpServletResponse;

/** Exception thrown by classes which implement WebdavNsIntf
 *
 *   @author Mike Douglass   douglm@rpi.edu
 */
public class WebdavIntfException extends WebdavException {
  /** Constructor
   *
   * @param s
   */
  public WebdavIntfException(String s) {
    super(s);
  }

  /** Constructor
   *
   * @param t
   */
  public WebdavIntfException(Throwable t) {
    super(t);
  }

  /** Constructor
   *
   * @param st
   */
  public WebdavIntfException(int st) {
    super(st);
  }

  /** Constructor
   *
   * @param st
   * @param msg
   */
  public WebdavIntfException(int st, String msg) {
    super(st, msg);
  }

  /**
   * @return WebdavIntfException
   */
  public static WebdavIntfException badRequest() {
    return new WebdavIntfException(HttpServletResponse.SC_BAD_REQUEST);
  }

  /**
   * @param msg
   * @return WebdavIntfException
   */
  public static WebdavIntfException badRequest(String msg) {
    return new WebdavIntfException(HttpServletResponse.SC_BAD_REQUEST, msg);
  }

  /**
   * @return WebdavIntfException
   */
  public static WebdavIntfException forbidden() {
    return new WebdavIntfException(HttpServletResponse.SC_FORBIDDEN);
  }

  /**
   * @return WebdavIntfException
   */
  public static WebdavIntfException notFound() {
    return new WebdavIntfException(HttpServletResponse.SC_NOT_FOUND);
  }

  /**
   * @return WebdavIntfException
   */
  public static WebdavIntfException serverError() {
    return new WebdavIntfException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
  }

  /**
   * @return WebdavIntfException
   */
  public static WebdavIntfException unauthorized() {
    return new WebdavIntfException(HttpServletResponse.SC_UNAUTHORIZED);
  }

  /**
   * @return WebdavIntfException
   */
  public static WebdavIntfException unsupportedMediaType() {
    return new WebdavIntfException(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
  }
}
