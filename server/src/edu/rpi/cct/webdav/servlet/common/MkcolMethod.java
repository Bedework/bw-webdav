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
package edu.rpi.cct.webdav.servlet.common;

import edu.rpi.cct.webdav.servlet.common.Headers.IfHeaders;
import edu.rpi.cct.webdav.servlet.shared.WebdavException;
import edu.rpi.cct.webdav.servlet.shared.WebdavNsIntf;
import edu.rpi.cct.webdav.servlet.shared.WebdavNsNode;
import edu.rpi.sss.util.xml.tagdefs.WebdavTags;

import org.w3c.dom.Document;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Class called to handle MKCOL
 *
 *   @author Mike Douglass   douglm@rpi.edu
 */
public class MkcolMethod extends PropPatchMethod {
  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.common.MethodBase#init()
   */
  @Override
  public void init() {
  }

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.common.MethodBase#doMethod(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
   */
  @Override
  public void doMethod(final HttpServletRequest req,
                        final HttpServletResponse resp) throws WebdavException {
    if (debug) {
      trace("MkcolMethod: doMethod");
    }

    WebdavNsIntf intf = getNsIntf();

    IfHeaders ifHeaders = Headers.processIfHeaders(req);
    if ((ifHeaders.ifHeader != null) &&
        !intf.syncTokenMatch(ifHeaders.ifHeader)) {
      intf.rollback();
      throw new WebdavException(HttpServletResponse.SC_PRECONDITION_FAILED);
    }

    /* Parse any content */
    Document doc = parseContent(req, resp);

    /* Create the node */
    String resourceUri = getResourceUri(req);

    WebdavNsNode node = intf.getNode(resourceUri,
                                     WebdavNsIntf.existanceNot,
                                     WebdavNsIntf.nodeTypeCollection);

    node.setDefaults(WebdavTags.mkcol);

    if (doc != null) {
      processDoc(req, resp, doc, node, WebdavTags.mkcol, true);
    }

    // Make collection using properties sent in request
    intf.makeCollection(req, resp, node);
  }
}
