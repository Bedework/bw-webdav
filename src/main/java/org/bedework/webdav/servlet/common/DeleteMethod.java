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
import org.bedework.webdav.servlet.shared.WebdavNsNode;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** Class called to handle DELETE
 *
 *   @author Mike Douglass   douglm  rpi.edu
 */
public class DeleteMethod extends MethodBase {
  @Override
  public void init() {
  }

  @Override
  public void doMethod(final HttpServletRequest req,
                        final HttpServletResponse resp) {
    if (debug()) {
      debug("DeleteMethod: doMethod");
    }

    try {
      final WebdavNsIntf intf = getNsIntf();

      final Headers.IfHeaders ifHeaders = Headers.processIfHeaders(req);
      if ((ifHeaders.ifHeader != null) &&
          !intf.syncTokenMatch(ifHeaders.ifHeader)) {
        intf.rollback();
        throw new WebdavException(HttpServletResponse.SC_PRECONDITION_FAILED);
      }

      final String resourceUri = getResourceUri(req);
      final int nodeType;
      if (resourceUri.endsWith("/")) {
        nodeType = WebdavNsIntf.nodeTypeCollection;
      } else {
        nodeType = WebdavNsIntf.nodeTypeUnknown;
      }

      final WebdavNsNode node = intf.getNode(getResourceUri(req),
                                             WebdavNsIntf.existanceMust,
                                             nodeType,
                                             false);

      if ((node == null) || !node.getExists()) {
        resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
        return;
      }

      intf.delete(node);

      resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
    } catch (final WebdavException we) {
      throw we;
    } catch (final Throwable t) {
      throw new WebdavException(t);
    }
  }
}

