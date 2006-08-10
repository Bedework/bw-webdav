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

package edu.rpi.cct.webdav.servlet.common;

import edu.rpi.cct.webdav.servlet.shared.WebdavException;
import edu.rpi.cct.webdav.servlet.shared.WebdavNsNode;

import java.io.Reader;
import java.io.Writer;
import java.util.Vector;
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

  private Vector ranges;

  /** size of buffer used for copying content to response.
   */
  private static final int bufferSize = 4096;

  /** Called at each request
   */
  public void init() {
    doContent = true;
  }

  public void doMethod(HttpServletRequest req,
                       HttpServletResponse resp) throws WebdavException {
    if (debug) {
      trace("GetMethod: doMethod");
    }

    try {
      WebdavNsNode node = getNsIntf().getNode(getResourceUri(req));

      if ((node == null) || !node.getExists()) {
        resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
        return;
      }

      Writer out;

      /** Get the content now to set up length, type etc.
       */
      Reader in = getNsIntf().getContent(node);

      if (doContent) {
        out = resp.getWriter();
      } else {
        out = null;
      }

      resp.setHeader("ETag", getEntityTag(node, true));

      if (node.getLastmodDate() != null) {
        resp.addHeader("Last-Modified", node.getLastmodDate().toString());
      }

      if (ranges == null) {
        /* Entire content
         */
        resp.setContentType(node.getContentType());
        resp.setContentLength(node.getContentLen());

        if (doContent) {
          if (in == null) {
            if (debug) {
              debugMsg("status: " + HttpServletResponse.SC_NO_CONTENT);
            }

            resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
          } else {
            if (debug) {
              debugMsg("send content - length=" + node.getContentLen());
            }

            writeContent(in, out);
          }
        }
      } else if (doContent && (in == null)) {
        if (debug) {
          debugMsg("status-2: " + HttpServletResponse.SC_NO_CONTENT);
        }

        resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
      } else {
        //FIXME not fully implemented
        if (debug) {
          debugMsg("status: " + HttpServletResponse.SC_PARTIAL_CONTENT);
        }

        resp.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);

        if (ranges.size() == 1) {
          // send a single chunk
          if (debug) {
            debugMsg("send multi-part - single element");
          }
        } else {
          // need to send it as some sort of multi-part thing
          if (debug) {
            debugMsg("send multi-part");
          }
        }
      }
    } catch (WebdavException we) {
      throw we;
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  /* Process the range headers in the request.
   *
   * <p>We can ignore these and for the moment we will
   * /
  private void parseRanges(HttpServletRequest req) throws WebdavException {
    ranges = null;
  }*/

  private void writeContent(Reader in, Writer out)
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

}

