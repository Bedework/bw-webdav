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

import org.bedework.davdefs.WebdavTags;

import edu.rpi.cct.webdav.servlet.shared.WebdavException;
import edu.rpi.cct.webdav.servlet.shared.WebdavNsIntf;
import edu.rpi.cct.webdav.servlet.shared.WebdavNsNode;
import edu.rpi.cct.webdav.servlet.shared.WebdavProperty;
import edu.rpi.cct.webdav.servlet.shared.WebdavStatusCode;
import edu.rpi.sss.util.xml.QName;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Class called to handle PROPFIND
 *
 *   @author Mike Douglass   douglm@rpi.edu
 */
public class PropFindMethod extends MethodBase {
  /**
   */
  public static class PropRequest {
    // ENUM
    //private static final int reqPropNone = 0;
    private static final int reqProp = 1;
    private static final int reqPropName = 2;
    private static final int reqPropAll = 3;

    int reqType;

    PropRequest(int reqType)  {
      this.reqType = reqType;
    }

    /** For the prop element we build a Collection of WebdavProperty
     */
    private Collection props;

    /**
     * @return Iterator
     */
    public Iterator iterateProperties() {
      if (props == null) {
        return new ArrayList().iterator();
      }

      return props.iterator();
    }
  }

  private PropRequest parsedReq;

  /** Called at each request
   */
  public void init() {
  }

  public void doMethod(HttpServletRequest req,
                       HttpServletResponse resp) throws WebdavException {
    if (debug) {
      trace("PropFindMethod: doMethod");
    }

    Document doc = parseContent(req, resp);

    if (doc == null) {
      // Treat as allprop request
      parsedReq = new PropRequest(PropRequest.reqPropAll);
    }

    startEmit(resp);

    if (doc != null) {
      int st = processDoc(doc);

      if (st != HttpServletResponse.SC_OK) {
        resp.setStatus(st);
        throw new WebdavException(st);
      }
    }

    int depth = Headers.depth(req);

    if (debug) {
      trace("PropFindMethod: depth=" + depth);
    }

    processResp(req, resp, depth);
  }

  /* ====================================================================
   *                   Private methods
   * ==================================================================== */

  private int processDoc(Document doc) throws WebdavException {
    try {
      Element root = doc.getDocumentElement();
      Element[] children = getChildren(root);

      for (int i = 0; i < children.length; i++) {
        Element curnode = children[i];

        String nm = curnode.getLocalName();
        String ns = curnode.getNamespaceURI();

        if (debug) {
          trace("reqtype: " + nm + " ns: " + ns);
        }

        parsedReq = tryPropRequest(curnode);
        if (parsedReq != null) {
          break;
        }
      }

      return HttpServletResponse.SC_OK;
    } catch (Throwable t) {
      System.err.println(t.getMessage());
      if (debug) {
        t.printStackTrace();
      }

      return HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
    }
  }

  /** See if the current node represents a valid propfind element
   * and return with a request if so. Otherwise return null.
   *
   * @param nd
   * @return PropRequest
   * @throws WebdavException
   */
  public PropRequest tryPropRequest(Node nd) throws WebdavException {
    if (WebdavTags.allprop.nodeMatches(nd)) {
      return new PropRequest(PropRequest.reqPropAll);
    }

    if (WebdavTags.prop.nodeMatches(nd)) {
      return parseProps(nd);
    }

    if (WebdavTags.propname.nodeMatches(nd)) {
      return new PropRequest(PropRequest.reqPropName);
    }

    return null;
  }

  /** Just a list of property names in any namespace.
   *
   * @param nd
   * @return PropRequest
   * @throws WebdavException
   */
  public PropRequest parseProps(Node nd) throws WebdavException {
    PropRequest pr = new PropRequest(PropRequest.reqProp);
    pr.props = new ArrayList();

    Element[] children = getChildren(nd);

    for (int i = 0; i < children.length; i++) {
      Element propnode = children[i];

      WebdavProperty prop = makeProp(propnode);

      if (debug) {
        trace("prop: " + prop.getTag());
      }
      addNs(prop.getTag().getNamespaceURI());

      pr.props.add(prop);
    }

    return pr;
  }

  /** Override this to create namespace specific property objects.
   *
   * @param propnode
   * @return WebdavProperty
   * @throws WebdavException
   */
  public WebdavProperty makeProp(Element propnode) throws WebdavException {
    return new WebdavProperty(new QName(propnode.getNamespaceURI(),
                                        propnode.getLocalName()),
                                        null);
  }

  /**
   * @param req
   * @param resp
   * @param depth
   * @throws WebdavException
   */
  public void processResp(HttpServletRequest req,
                          HttpServletResponse resp,
                          int depth) throws WebdavException {
    resp.setStatus(WebdavStatusCode.SC_MULTI_STATUS);
    resp.setContentType("text/xml; charset=UTF-8");

    String resourceUri = getResourceUri(req);
    if (debug) {
      trace("About to get node at " + resourceUri);
    }

    WebdavNsNode node = getNsIntf().getNode(resourceUri);

    openTag(WebdavTags.multistatus);

    if (node != null) {
      doNodeAndChildren(node, 0, depth);
    }

    closeTag(WebdavTags.multistatus);

    flush();
  }

  /** Generate response for a PROPFIND for the current node, then for the children.
   *
   * @param node
   * @param pr
   * @throws WebdavException
   */
  public void doNodeProperties(WebdavNsNode node,
                                PropRequest pr) throws WebdavException {
    addHref(node);

    if ((pr != null) && (node.getExists())) {
      if (pr.reqType == PropRequest.reqProp) {
        doPropFind(node, pr);
      } else if (pr.reqType == PropRequest.reqPropName) {
        doPropNames(node);
      } else if (pr.reqType == PropRequest.reqPropAll) {
        doPropAll(node);
      }
    }
  }

  private void doNodeAndChildren(WebdavNsNode node,
                                 int curDepth,
                                 int maxDepth) throws WebdavException {
    openTag(WebdavTags.response);

    doNodeProperties(node, parsedReq);

    closeTag(WebdavTags.response);

    flush();

    curDepth++;

    if (curDepth > maxDepth) {
      return;
    }

    Iterator children = getNsIntf().getChildren(node);

    while (children.hasNext()) {
      WebdavNsNode child = (WebdavNsNode)children.next();

      doNodeAndChildren(child, curDepth, maxDepth);
    }
  }

  /** Build the response for a single node for a propfind request
   *
   * @param node
   * @param preq
   * @throws WebdavException
   */
  private void doPropFind(WebdavNsNode node, PropRequest preq) throws WebdavException {
    Iterator it = preq.props.iterator();
    WebdavNsIntf intf = getNsIntf();

    while (it.hasNext()) {
      WebdavProperty pr = (WebdavProperty)it.next();
      intf.generatePropValue(node, pr);
    }
  }

  /* Build the response for a single node for a propnames request
   */
  private int doPropNames(WebdavNsNode node) throws WebdavException {
    return HttpServletResponse.SC_OK;
  }

  /* Build the response for a single node for an allprop request
   */
  private int doPropAll(WebdavNsNode node) throws WebdavException {
    doLockDiscovery(node);

    String sl = getNsIntf().getSupportedLocks();

    if (sl != null) {
      property(WebdavTags.supportedlock, sl);
    }

    doNodeNsProperties(node);

    property(WebdavTags.creationdate, node.getCreDate());

    property(WebdavTags.displayname, node.getName());

    if (node.getCollection()) {
      getNsIntf().generatePropResourcetype(node);
//      propertyTagVal(WebdavTags.resourcetype,
//                     WebdavTags.collection);
    }

    if (node.getAllowsGet()) {
      property(WebdavTags.getcontentlength,
               String.valueOf(node.getContentLen()));
      property(WebdavTags.getcontenttype, node.getContentType());
    }

    property(WebdavTags.getlastmodified, node.getLastmodDate());

    return HttpServletResponse.SC_OK;
  }

  /* Build the lockdiscovery response for a single node
   */
  private void doLockDiscovery(WebdavNsNode node) throws WebdavException {
  }

  /* Does all the properties special to the underlying namespace
   */
  private void doNodeNsProperties(WebdavNsNode node) throws WebdavException {
    Iterator it = getNsIntf().iterateProperties(node);
    WebdavNsIntf intf = getNsIntf();

    while (it.hasNext()) {
      WebdavProperty prop = (WebdavProperty)it.next();

      intf.generatePropValue(node, prop);
    }
  }

  private void addHref(WebdavNsNode node) throws WebdavException {
    try {
      if (debug) {
        trace("Adding href " + getUrlPrefix() + node.getEncodedUri());
      }

      String url = getUrlPrefix() + new URI(node.getEncodedUri()).toASCIIString();
      property(WebdavTags.href, url);
    } catch (WebdavException wde) {
      throw wde;
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }
}

