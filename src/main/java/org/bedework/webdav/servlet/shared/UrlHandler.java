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

import org.bedework.util.misc.Util;

import java.net.URI;

import jakarta.servlet.http.HttpServletRequest;

/** Prefix or unprefix urls - or not depending on internal state
*
* @author douglm
*/
public class UrlHandler implements UrlPrefixer, UrlUnprefixer {
  private String urlPrefix;

  private final boolean relative;

  private String context;

  /** If relative we assume urls are relative to the host + port.
   * Internally we need to strip off the host + port + context.
   *
   * @param req the incoming request
   * @param relative true for relative urls
   */
  public UrlHandler(final HttpServletRequest req,
                    final boolean relative) {
    this.relative = relative;

    String contextPath = req.getContextPath();
    if ((contextPath == null) || (contextPath.equals("."))) {
      contextPath = "/";
    }

    String sp = req.getServletPath();
    if ((sp == null) || (sp.equals("."))) {
      sp = "/";
    }

    context = Util.buildPath(false, contextPath, "/", sp);
    if (context.equals("/")) {
      context = "";
    }

    urlPrefix = req.getRequestURL().toString();
    final int pos;

    if (!context.isEmpty()) {
      pos = urlPrefix.indexOf(context);
    } else {
      pos = urlPrefix.indexOf(req.getRequestURI());
    }

    if (pos > 0) {
      urlPrefix = urlPrefix.substring(0, pos);
    }
  }

  /**
   * @param urlPrefix Usually host + port + "/" e.g. example.com:8080/
   * @param context null, zero length or the servlet context, e.g. caldav
   * @param relative true if we want relative (to the server) urls
   */
  @SuppressWarnings("unused")
  public UrlHandler(final String urlPrefix,
                    final String context,
                    final boolean relative) {
    this.relative = relative;

    if ((context == null) ||
        context.equals("/")) {
      this.context = "";
    } else if (context.endsWith("/")) {
      this.context = context.substring(0, context.length() - 1);
    } else {
      this.context = context;
    }

    this.urlPrefix = urlPrefix;
  }

  @Override
  public String prefix(final String val) {
    if (val == null) {
      return null;
    }

    if (val.toLowerCase().startsWith("mailto:")) {
      return val;
    }

    String enc;
    try {
      enc = new URI(null, null, val, null).toString();
      enc = new URI(enc).toASCIIString();  // XXX ???????
    } catch (final Throwable t) {
      throw new RuntimeException(t);
    }

    final StringBuilder sb = new StringBuilder();

    if (!relative) {
      sb.append(getUrlPrefix());
    }

    if (!val.startsWith(context + "/")) {
      append(sb, context);
    }
    append(sb, enc);

    return sb.toString();
  }

  @Override
  public String unprefix(String val) {
    if (val == null) {
      return null;
    }

    if (val.startsWith(getUrlPrefix())) {
      val = val.substring(getUrlPrefix().length());
    }

    if (val.startsWith(context)) {
      val = val.substring(context.length());
    }

    return val;
  }

  /**
   * @return String url prefix (host + port, no context)
   */
  public String getUrlPrefix() {
    return urlPrefix;
  }

  private boolean endsWithSlash(final StringBuilder sb) {
    if (sb.isEmpty()) {
      return false;
    }

    return sb.charAt(sb.length() - 1) == '/';
  }

  private void append(final StringBuilder sb, final String val) {
    if (val.startsWith("/")) {
      if (!endsWithSlash(sb)) {
        sb.append(val);
      } else {
        sb.append(val.substring(1));
      }
    } else {
      if (!endsWithSlash(sb)) {
        sb.append("/");
      }

      sb.append(val);
    }
  }
}
