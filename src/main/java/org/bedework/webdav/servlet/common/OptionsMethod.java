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

/** Class called to handle OPTIONS. We should determine what the current
 * url refers to and send a response which shows the allowable methods on that
 * resource.
 *
 *   @author Mike Douglass   douglm  rpi.edu
 */
public class OptionsMethod extends MethodBase {
  @Override
  public void init() {
  }

  public void doMethod(final HttpServletRequest req,
                       final HttpServletResponse resp) {
    if (debug()) {
      debug("OptionsMethod: doMethod");
    }

    try {
      final String resourceUri = getResourceUri(req);

      final WebdavNsNode node;
      if ("*".equals(resourceUri)) {
        node = null;
      } else {
        node = getNsIntf().getNode(resourceUri,
                                   WebdavNsIntf.existanceMust,
                                   WebdavNsIntf.nodeTypeUnknown,
                                   false);

        /* Apparently if the node doesn't exist we're supposed to respond
       * not found, rather than indicate if PUT is allowed for example.
       */
        if ((node == null) || !node.getExists()) {
          resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
          return;
        }
      }

      addHeaders(req, resp, node);
    } catch (final WebdavException we) {
      throw we;
    } catch (final Throwable t) {
      throw new WebdavException(t);
    }
  }
}

