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

package edu.rpi.cct.webdav.servlet.common;

import edu.rpi.cct.webdav.servlet.shared.WebdavException;
import edu.rpi.cct.webdav.servlet.shared.WebdavNsIntf;
import edu.rpi.cct.webdav.servlet.shared.WebdavNsNode;
import edu.rpi.cct.webdav.servlet.shared.WebdavNsIntf.Content;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Class called to handle GET
 *
 * Get the content of a node. Note this is subclassed by HeadMethod which
 * overrides init and sets doContent false.
 *
 *   @author Mike Douglass   douglm@rpi.edu
 */
public class GetMethod extends MethodBase {
  protected boolean doContent;

  /** size of buffer used for copying content to response.
   */
  private static final int bufferSize = 4096;

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.common.MethodBase#init()
   */
  @Override
  public void init() {
    doContent = true;
  }

  @Override
  public void doMethod(final HttpServletRequest req,
                       final HttpServletResponse resp) throws WebdavException {
    if (debug) {
      trace("GetMethod: doMethod");
    }

    try {
      WebdavNsIntf intf = getNsIntf();

      if (intf.specialUri(req, resp, getResourceUri(req))) {
        return;
      }

      //String reqContentType = req.getContentType();
      //boolean reqHtml = "text/html".equals(reqContentType);

      WebdavNsNode node = intf.getNode(getResourceUri(req),
                                       WebdavNsIntf.existanceMust,
                                       WebdavNsIntf.nodeTypeUnknown);

      if ((node == null) || !node.getExists()) {
        resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
        return;
      }

      if (!intf.prefetch(req, resp, node)) {
        return;
      }

      Content c = null;

      /** Get the content now to set up length, type etc.
       */

      if (node.getContentBinary()) {
        c = intf.getBinaryContent(node);
        // XXX check accept header
      } else {
        c = intf.getContent(req, resp, node);
      }

      if (c.written) {
        resp.setStatus(HttpServletResponse.SC_OK);
        return;
      }

      if (c == null) {
        if (debug) {
          debugMsg("status: " + HttpServletResponse.SC_NO_CONTENT);
        }

        resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
        return;
      }

      resp.setHeader("ETag", node.getEtagValue(true));

      if (node.getLastmodDate() != null) {
        resp.addHeader("Last-Modified", node.getLastmodDate().toString());
      }

      resp.setContentType(c.contentType);

      if (c.contentLength > Integer.MAX_VALUE) {
        resp.setContentLength(-1);
      } else {
        resp.setContentLength((int)c.contentLength);
      }

      if (doContent) {
        if ((c.stream == null) && (c.rdr == null)) {
          if (debug) {
            debugMsg("status: " + HttpServletResponse.SC_NO_CONTENT);
          }

          resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
        } else {
          if (debug) {
            debugMsg("send content - length=" + c.contentLength);
          }

          if (c.stream != null) {
            streamContent(c.stream, resp.getOutputStream());
          } else {
            writeContent(c.rdr, resp.getWriter());
          }
        }
      }
    } catch (WebdavException we) {
      throw we;
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  private void writeContent(final Reader in, final Writer out)
      throws WebdavException {
    try {
      char[] buff = new char[bufferSize];
      int len;

      while (true) {
        len = in.read(buff);

        if (len < 0) {
          break;
        }

        out.write(buff, 0, len);
      }
    } catch (Throwable t) {
      throw new WebdavException(t);
    } finally {
      try {
        in.close();
      } catch (Throwable t) {}
      try {
        out.close();
      } catch (Throwable t) {}
    }
  }

  private void streamContent(final InputStream in, final OutputStream out)
      throws WebdavException {
    try {
      byte[] buff = new byte[bufferSize];
      int len;

      while (true) {
        len = in.read(buff);

        if (len < 0) {
          break;
        }

        out.write(buff, 0, len);
      }
    } catch (Throwable t) {
      throw new WebdavException(t);
    } finally {
      try {
        in.close();
      } catch (Throwable t) {}
      try {
        out.close();
      } catch (Throwable t) {}
    }
  }
}

