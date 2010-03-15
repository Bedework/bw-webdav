/* **********************************************************************
    Copyright 2010 Rensselaer Polytechnic Institute. All worldwide rights reserved.

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

import edu.rpi.cct.webdav.servlet.common.WebdavUtils;

import org.apache.log4j.Logger;

import java.net.URI;

import javax.servlet.http.HttpServletRequest;

/** Prefix or unprefix urls - or not depending on internal state
*
* @author douglm
*/
public class UrlHandler {
  private String urlPrefix;

  private boolean relative;

  private String context;

  /** If relative we assume urls are relative to the host + port.
   * Internally we need to strip off the host + port + context.
   *
   * @param req
   * @param relative
   * @throws WebdavException
   */
  public UrlHandler(final HttpServletRequest req,
                    final boolean relative) throws WebdavException {
    this.relative = relative;

    try {
      context = req.getContextPath();
      if ((context == null) || (context.equals("."))) {
        /* XXX Not at all sure why I did this.
         * Context is "" for the root context anyway.
         */
        context = "";
      }

      urlPrefix = req.getRequestURL().toString();
      int pos;

      if (context.length() > 0) {
        pos = urlPrefix.indexOf(context);
      } else {
        pos = urlPrefix.indexOf(req.getRequestURI());
      }

      if (pos > 0) {
        urlPrefix = urlPrefix.substring(0, pos);
      }
    } catch (Throwable t) {
      Logger.getLogger(WebdavUtils.class).warn(
          "Unable to get url from " + req);
      throw new WebdavException(t);
    }
  }

  /** Return an appropriately prefixed url. The parameter url will be
   * absolute or relative. If relative it may be prefixed with the context
   * path which we need to remove.
   *
   * <p>We're doing this because some clients don't handle absolute urls
   * (a violation of the spec)
   *
   * @param val
   * @return String
   * @throws WebdavException
   */
  public String prefix(final String val) throws WebdavException {
    try {
      if (val.toLowerCase().startsWith("mailto:")) {
        return val;
      }

      String enc = new URI(null, null, val, null).toString();
      enc = new URI(enc).toASCIIString();  // XXX ???????

      StringBuilder sb = new StringBuilder();

      if (!relative) {
        sb.append(getUrlPrefix());
      }

      if (!val.startsWith(context + "/")) {
        append(sb, context);
      }
      append(sb, enc);

      return sb.toString();
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  /** Remove any vestige of the host, port or context
   *
   * @param val
   * @return String
   * @throws WebdavException
   */
  public String unprefix(String val) throws WebdavException {
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
    if (sb.length() == 0) {
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
