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

import java.util.HashMap;
import javax.servlet.http.HttpServletResponse;

/** Define webdav status codes.
 *
 *   @author Mike Douglass   douglm@rpi.edu
 */
public class WebdavStatusCode {
  /** multi-status OK response */
  public final static int SC_MULTI_STATUS = 207;

  private final static HashMap msgtext = new HashMap();

  static {
    addmsg(SC_MULTI_STATUS, "Multi-Status");

    // These must be predefined somewhere?
    addmsg(HttpServletResponse.SC_ACCEPTED, "accepted");
    addmsg(HttpServletResponse.SC_BAD_GATEWAY, "bad_gateway");
    addmsg(HttpServletResponse.SC_BAD_REQUEST, "bad_request");
    addmsg(HttpServletResponse.SC_CONFLICT, "conflict");
    addmsg(HttpServletResponse.SC_CONTINUE, "continue");
    addmsg(HttpServletResponse.SC_CREATED, "created");
    addmsg(HttpServletResponse.SC_EXPECTATION_FAILED, "expectation_failed");
    addmsg(HttpServletResponse.SC_FORBIDDEN, "forbidden");
    addmsg(HttpServletResponse.SC_FOUND, "found");
    addmsg(HttpServletResponse.SC_GATEWAY_TIMEOUT, "gateway_timeout");
    addmsg(HttpServletResponse.SC_GONE, "gone");
    addmsg(HttpServletResponse.SC_HTTP_VERSION_NOT_SUPPORTED, "http_version_not_supported");
    addmsg(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "internal_server_error");
    addmsg(HttpServletResponse.SC_LENGTH_REQUIRED, "length_required");
    addmsg(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "method_not_allowed");
    addmsg(HttpServletResponse.SC_MOVED_PERMANENTLY, "moved_permanently");
    addmsg(HttpServletResponse.SC_MOVED_TEMPORARILY, "moved_temporarily");
    addmsg(HttpServletResponse.SC_MULTIPLE_CHOICES, "multiple_choices");
    addmsg(HttpServletResponse.SC_NO_CONTENT, "no_content");
    addmsg(HttpServletResponse.SC_NON_AUTHORITATIVE_INFORMATION, "non_authoritative_information");
    addmsg(HttpServletResponse.SC_NOT_ACCEPTABLE, "not_acceptable");
    addmsg(HttpServletResponse.SC_NOT_FOUND, "not_found");
    addmsg(HttpServletResponse.SC_NOT_IMPLEMENTED, "not_implemented");
    addmsg(HttpServletResponse.SC_NOT_MODIFIED, "not_modified");
    addmsg(HttpServletResponse.SC_OK, "ok");
    addmsg(HttpServletResponse.SC_PARTIAL_CONTENT, "partial_content");
    addmsg(HttpServletResponse.SC_PAYMENT_REQUIRED, "payment_required");
    addmsg(HttpServletResponse.SC_PRECONDITION_FAILED, "precondition_failed");
    addmsg(HttpServletResponse.SC_PROXY_AUTHENTICATION_REQUIRED, "proxy_authentication_required");
    addmsg(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE, "request_entity_too_large");
    addmsg(HttpServletResponse.SC_REQUEST_TIMEOUT, "request_timeout");
    addmsg(HttpServletResponse.SC_REQUEST_URI_TOO_LONG, "request_uri_too_long");
    addmsg(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE, "requested_range_not_satisfiable");
    addmsg(HttpServletResponse.SC_RESET_CONTENT, "reset_content");
    addmsg(HttpServletResponse.SC_SEE_OTHER, "see_other");
    addmsg(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "service_unavailable");
    addmsg(HttpServletResponse.SC_SWITCHING_PROTOCOLS, "switching_protocols");
    addmsg(HttpServletResponse.SC_TEMPORARY_REDIRECT, "temporary_redirect");
    addmsg(HttpServletResponse.SC_UNAUTHORIZED, "unauthorized");
    addmsg(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE, "unsupported_media_type");
    addmsg(HttpServletResponse.SC_USE_PROXY, "use_proxy");
  }

  private static void addmsg(int sc, String txt) {
    msgtext.put(new Integer(sc), txt);
  }

  /** Get a message for a code
   *
   * @param    sc int status
   * @return   String emssage
   */
  public static String getMessage(int sc) {
    String msg = (String)msgtext.get(new Integer(sc));

    if (msg == null) {
      msg = String.valueOf(sc);
    }

    return msg;
  }
}
