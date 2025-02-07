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

package org.bedework.webdav.servlet.shared;

import java.util.HashMap;
import jakarta.servlet.http.HttpServletResponse;

/** Define webdav status codes.
 *
 *   @author Mike Douglass   douglm   rpi.edu
 */
public class WebdavStatusCode {
  /** multi-status OK response */
  public final static int SC_MULTI_STATUS = 207;

  /** */
  public final static int SC_FAILED_DEPENDENCY = 424;

  private final static HashMap<Integer, String> msgtext = new HashMap<Integer, String>();

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
    msgtext.put(sc, txt);
  }

  /** Get a message for a code
   *
   * @param    sc int status
   * @return   String emssage
   */
  public static String getMessage(int sc) {
    String msg = msgtext.get(sc);

    if (msg == null) {
      msg = String.valueOf(sc);
    }

    return msg;
  }
}
