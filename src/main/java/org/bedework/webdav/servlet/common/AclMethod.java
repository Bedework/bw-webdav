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

import org.bedework.access.AccessException;
import org.bedework.util.xml.tagdefs.WebdavTags;
import org.bedework.webdav.servlet.shared.WebdavBadRequest;
import org.bedework.webdav.servlet.shared.WebdavException;
import org.bedework.webdav.servlet.shared.WebdavNsIntf;
import org.bedework.webdav.servlet.shared.WebdavServerError;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Class to handle WebDav ACLs
 *
 *  @author Mike Douglass   douglm  rpi.edu
 */
public class AclMethod extends MethodBase {
  /** Called at each request
   */
  @Override
  public void init() {
  }

  @Override
  public void doMethod(final HttpServletRequest req,
                       final HttpServletResponse resp) throws WebdavException {
    if (debug) {
      debug("AclMethod: doMethod");
    }

    Document doc = parseContent(req, resp);

    if (doc == null) {
      return;
    }

    WebdavNsIntf.AclInfo ainfo = processDoc(doc, getResourceUri(req));

    processResp(req, resp, ainfo);
  }

  /* ====================================================================
   *                   Private methods
   * ==================================================================== */

  /* We process the parsed document and produce a Collection of request
   * objects to process.
   */
  private WebdavNsIntf.AclInfo processDoc(final Document doc, final String uri) throws WebdavException {
    try {
      WebdavNsIntf intf = getNsIntf();

      WebdavNsIntf.AclInfo ainfo = new WebdavNsIntf.AclInfo(uri);

      Element root = doc.getDocumentElement();

      AccessUtil autil = intf.getAccessUtil();

      ainfo.acl = autil.getAcl(root, true);

      if (autil.getErrorTag() != null) {
        ainfo.errorTag = autil.getErrorTag();
      }

      return ainfo;
    } catch (WebdavException wde) {
      throw wde;
    } catch (AccessException ae) {
      throw new WebdavBadRequest(ae.getMessage());
    } catch (Throwable t) {
      error(t.getMessage());
      if (debug) {
        t.printStackTrace();
      }

      throw new WebdavServerError();
    }
  }

  private void processResp(final HttpServletRequest req,
                          final HttpServletResponse resp,
                          final WebdavNsIntf.AclInfo ainfo) throws WebdavException {
    WebdavNsIntf intf = getNsIntf();

    if (ainfo.errorTag == null) {
      intf.updateAccess(ainfo);
      return;
    }

    startEmit(resp);
    resp.setStatus(HttpServletResponse.SC_FORBIDDEN);

    openTag(WebdavTags.error);
    emptyTag(ainfo.errorTag);
    closeTag(WebdavTags.error);

    flush();
  }
}

