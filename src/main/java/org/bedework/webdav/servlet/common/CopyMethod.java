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
import org.bedework.webdav.servlet.shared.WebdavForbidden;
import org.bedework.webdav.servlet.shared.WebdavNotFound;
import org.bedework.webdav.servlet.shared.WebdavNsIntf;
import org.bedework.webdav.servlet.shared.WebdavNsNode;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** Class to handle COPY
 *
 *   @author Mike Douglass   douglm@rpi.edu
 */
public class CopyMethod extends MethodBase {
  /** Called at each request
   */
  public void init() {
  }

  @Override
  public void doMethod(final HttpServletRequest req,
                       final HttpServletResponse resp) {
    process(req, resp, true);
  }

  protected void process(final HttpServletRequest req,
                         final HttpServletResponse resp,
                         final boolean copy) {
    if (debug()) {
      if (copy) {
        debug("CopyMethod: doMethod");
      } else {
        debug("MoveMethod: doMethod");
      }
    }

    try {
      final String dest = req.getHeader("Destination");
      if (dest == null) {
        if (debug()) {
          debug("No Destination");
        }
        throw new WebdavNotFound("No Destination");
      }

      final int depth = Headers.depth(req);
      /*
      if (depth == Headers.depthNone) {
        depth = Headers.depthInfinity;
      }
      */

      final String ow = req.getHeader("Overwrite");
      final boolean overwrite;
      if (ow == null) {
        overwrite = true;
      } else if ("T".equals(ow)) {
        overwrite = true;
      } else if ("F".equals(ow)) {
        overwrite = false;
      } else {
        resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        return;
      }

      final WebdavNsIntf intf = getNsIntf();
      final WebdavNsNode from = intf.getNode(getResourceUri(req),
                                             WebdavNsIntf.existanceMust,
                                             WebdavNsIntf.nodeTypeUnknown,
                                             false);

      if ((from == null) || !from.getExists()) {
        resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
        return;
      }

      final int toNodeType;
      if (from.isCollection()) {
        toNodeType = WebdavNsIntf.nodeTypeCollection;
      } else {
        toNodeType = WebdavNsIntf.nodeTypeEntity;
      }

      final WebdavNsNode to = intf.getNode(intf.getUri(dest),
                                           WebdavNsIntf.existanceMay,
                                           toNodeType,
                                           false);

      if (from.equals(to)) {
        throw new WebdavForbidden("source and destination equal");
      }

      intf.copyMove(req, resp, from, to, copy, overwrite, depth);
    } catch (final WebdavException we) {
      throw we;
    } catch (final Throwable t) {
      throw new WebdavException(t);
    }
  }
}
