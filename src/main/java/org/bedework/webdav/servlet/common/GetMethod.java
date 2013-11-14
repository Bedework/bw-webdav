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

import org.bedework.webdav.servlet.shared.WebdavException;
import org.bedework.webdav.servlet.shared.WebdavNsIntf;
import org.bedework.webdav.servlet.shared.WebdavNsIntf.Content;
import org.bedework.webdav.servlet.shared.WebdavNsNode;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Class called to handle GET
 *
 * Get the content of a node. Note this is subclassed by HeadMethod which
 * overrides init and sets doContent false.
 *
 *   @author Mike Douglass   douglm   rpi.edu
 */
public class GetMethod extends MethodBase {
  protected boolean doContent;

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

      resp.setHeader("ETag", node.getEtagValue(true));

      if (node.getLastmodDate() != null) {
        resp.addHeader("Last-Modified", node.getLastmodDate().toString());
      }

      Content c = null;

      /** Get the content now to set up length, type etc.
       */

      if (node.getContentBinary()) {
        c = intf.getBinaryContent(node);
        // XXX check accept header
      } else {
        c = intf.getContent(req, resp, null, node);
      }

      if (c == null) {
        if (debug) {
          debugMsg("status: " + HttpServletResponse.SC_NO_CONTENT);
        }

        resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
        return;
      }

      if (c.written) {
        resp.setStatus(HttpServletResponse.SC_OK);
      } else if (c.contentType != null) {
        resp.setContentType(c.contentType);
      }

      if (c.contentLength > Integer.MAX_VALUE) {
        resp.setContentLength(-1);
      } else {
        resp.setContentLength((int)c.contentLength);
      }

      if (c.written || !doContent) {
        return;
      }

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
          intf.streamContent(c.stream, resp.getOutputStream());
        } else {
          intf.writeContent(c.rdr, resp.getWriter());
        }
      }
    } catch (WebdavException we) {
      throw we;
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }
}

