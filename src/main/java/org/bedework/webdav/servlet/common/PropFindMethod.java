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

import org.bedework.util.misc.Util;
import org.bedework.util.misc.response.GetEntityResponse;
import org.bedework.util.xml.XmlUtil;
import org.bedework.util.xml.tagdefs.WebdavTags;
import org.bedework.webdav.servlet.shared.WebdavBadRequest;
import org.bedework.webdav.servlet.shared.WebdavException;
import org.bedework.webdav.servlet.shared.WebdavNsIntf;
import org.bedework.webdav.servlet.shared.WebdavNsNode;
import org.bedework.webdav.servlet.shared.WebdavNsNode.PropertyTagEntry;
import org.bedework.webdav.servlet.shared.WebdavProperty;
import org.bedework.webdav.servlet.shared.WebdavStatusCode;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Class called to handle PROPFIND
 *
 *   @author Mike Douglass   douglm   rpi.edu
 */
public class PropFindMethod extends MethodBase {
  /**
   */
  public static class PropRequest {
    /** */
    public enum ReqType {
      /** */
      prop,
      /** */
      propName,
      /** */
      propAll
    }

    /** */
    public ReqType reqType;

    PropRequest(final ReqType reqType)  {
      this.reqType = reqType;
    }

    /** For the prop element we build a Collection of WebdavProperty
     */
    public List<WebdavProperty> props;
  }

  private PropRequest parsedReq;

  @Override
  public void init() {
  }

  @Override
  public void doMethod(final HttpServletRequest req,
                       final HttpServletResponse resp) {
    if (debug()) {
      debug("PropFindMethod: doMethod");
    }

    final Document doc = parseContent(req, resp);

    if (doc == null) {
      // Treat as allprop request
      parsedReq = new PropRequest(PropRequest.ReqType.propAll);
    } else {
      processDoc(doc);
    }

    int depth = Headers.depth(req);
    if (depth == Headers.depthNone) {
      depth = Headers.depthInfinity;
    }
    
    if (parsedReq == null) {
      throw new WebdavBadRequest("PROPFIND: unexpected element");
    }

    if (debug()) {
      debug("PropFindMethod: depth=" + depth);
      debug("                type=" + parsedReq.reqType);
    }

    processResp(req, resp, depth);
  }

  /* ====================================================================
   *                   Private methods
   * ==================================================================== */

  private void processDoc(final Document doc) {
    try {
      final Element root = doc.getDocumentElement();

      if (!XmlUtil.nodeMatches(root, WebdavTags.propfind)) {
        throw new WebdavBadRequest();
      }

      final GetEntityResponse<Element> childResp = getOnlyChild(root);
      if (!childResp.isOk()) {
        throw new WebdavBadRequest(childResp.getMessage());
      }

      final var curnode = childResp.getEntity();

      final String ns = curnode.getNamespaceURI();

      addNs(ns);

      if (debug()) {
        final String nm = curnode.getLocalName();

        debug("reqtype: " + nm + " ns: " + ns);
      }

      parsedReq = tryPropRequest(curnode);
    } catch (final WebdavException wde) {
      throw wde;
    } catch (final Throwable t) {
      System.err.println(t.getMessage());
      if (debug()) {
        t.printStackTrace();
      }

      throw new WebdavException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }
  }

  /** See if the current node represents a valid propfind element
   * and return with a request if so. Otherwise return null.
   *
   * @param nd propfind element node
   * @return PropRequest
   */
  public PropRequest tryPropRequest(final Node nd) {
    if (XmlUtil.nodeMatches(nd, WebdavTags.allprop)) {
      return new PropRequest(PropRequest.ReqType.propAll);
    }

    if (XmlUtil.nodeMatches(nd, WebdavTags.prop)) {
      return parseProps(nd);
    }

    if (XmlUtil.nodeMatches(nd, WebdavTags.propname)) {
      return new PropRequest(PropRequest.ReqType.propName);
    }

    return null;
  }

  /** Just a list of property names in any namespace.
   *
   * @param nd dav:prop node
   * @return PropRequest
   */
  public PropRequest parseProps(final Node nd) {
    final PropRequest pr = new PropRequest(PropRequest.ReqType.prop);
    pr.props = getNsIntf().parseProp(nd);

    return pr;
  }

  /**
   * @param req http request
   * @param resp http response
   * @param depth from depth header
   */
  public void processResp(final HttpServletRequest req,
                          final HttpServletResponse resp,
                          final int depth) {
    final String resourceUri = getResourceUri(req);
    if (debug()) {
      debug("About to get node at " + resourceUri);
    }

    final int nodeType;
    if (resourceUri.endsWith("/")) {
      nodeType = WebdavNsIntf.nodeTypeCollection;
    } else {
      nodeType = WebdavNsIntf.nodeTypeUnknown;
    }

    final WebdavNsNode node = getNsIntf().getNode(resourceUri,
                                                  WebdavNsIntf.existanceMust,
                                                  nodeType,
                                                  false);

    if (node == null) {
      resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
      return;
    }

    addHeaders(req, resp, node);

    resp.setStatus(WebdavStatusCode.SC_MULTI_STATUS);
    resp.setContentType("text/xml; charset=UTF-8");

    startEmit(resp);

    openTag(WebdavTags.multistatus);

    doNodeAndChildren(node, 0, depth);

    closeTag(WebdavTags.multistatus);

    flush();
  }

  /** Generate response for a PROPFIND for the current node, then for the children.
   *
   * @param node current node
   * @param pr the prop request
   */
  public void doNodeProperties(final WebdavNsNode node,
                                final PropRequest pr) {
    node.generateHref(xml);

    final var propNameOrAll = (pr.reqType == PropRequest.ReqType.propName) ||
            (pr.reqType == PropRequest.ReqType.propAll);

    if ((!propNameOrAll && Util.isEmpty(pr.props)) ||
            !node.getExists()) {
      openTag(WebdavTags.propstat);
      if (node.getStatus() == HttpServletResponse.SC_OK) {
        emptyTag(WebdavTags.prop);
      }
      addStatus(node.getStatus(), null);
      closeTag(WebdavTags.propstat);
      return;
    }

    if (propNameOrAll) {
      openTag(WebdavTags.propstat);

      if (debug()) {
        debug("doNodeProperties type=" + pr.reqType);
      }

      if (pr.reqType == PropRequest.ReqType.propName) {
        doPropNames(node);
      } else if (pr.reqType == PropRequest.ReqType.propAll) {
        doPropAll(node);
      }
      addStatus(node.getStatus(), null);

      closeTag(WebdavTags.propstat);

      return;
    }

    if (pr.reqType != PropRequest.ReqType.prop) {
      throw new WebdavBadRequest();
    }

    // Named properties

    doPropFind(node, pr.props);
  }

  private void doNodeAndChildren(final WebdavNsNode node,
                                 int curDepth,
                                 final int maxDepth) {
    openTag(WebdavTags.response);

    doNodeProperties(node, parsedReq);

    closeTag(WebdavTags.response);

    flush();

    curDepth++;

    if (curDepth > maxDepth) {
      return;
    }

    for (final WebdavNsNode child: getNsIntf().getChildren(node, null)) {
      doNodeAndChildren(child, curDepth, maxDepth);
    }
  }

  /* Build the response for a single node for a propnames request
   */
  private void doPropNames(final WebdavNsNode node) {
    openTag(WebdavTags.prop);

    for (final PropertyTagEntry pte: node.getPropertyNames()) {
      if (pte.inPropAll) {
        emptyTag(pte.tag);
      }
    }

    closeTag(WebdavTags.prop);
  }

  /* Build the response for a single node for an allprop request
   */
  private int doPropAll(final WebdavNsNode node) {
    final WebdavNsIntf intf = getNsIntf();

    openTag(WebdavTags.prop);

    doLockDiscovery(node);

    final String sl = getNsIntf().getSupportedLocks();

    if (sl != null) {
      property(WebdavTags.supportedlock, sl);
    }

    for (final PropertyTagEntry pte: node.getPropertyNames()) {
      if (pte.inPropAll) {
        intf.generatePropValue(node, new WebdavProperty(pte.tag, null), true);
      }
    }

    closeTag(WebdavTags.prop);

    return HttpServletResponse.SC_OK;
  }

  /* Build the lockdiscovery response for a single node
   */
  private void doLockDiscovery(final WebdavNsNode node) {
  }
}

