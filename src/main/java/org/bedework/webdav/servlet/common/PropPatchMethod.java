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

import org.bedework.util.xml.XmlUtil;
import org.bedework.util.xml.tagdefs.WebdavTags;
import org.bedework.webdav.servlet.shared.WebdavBadRequest;
import org.bedework.webdav.servlet.shared.WebdavException;
import org.bedework.webdav.servlet.shared.WebdavNsIntf;
import org.bedework.webdav.servlet.shared.WebdavNsNode;
import org.bedework.webdav.servlet.shared.WebdavStatusCode;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.Collection;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.namespace.QName;

/** Class called to handle PROPPATCH
 *
 *   @author Mike Douglass   douglm  rpi.edu
 */
public class PropPatchMethod extends MethodBase {
  @Override
  public void init() {
  }

  @Override
  public void doMethod(final HttpServletRequest req,
                        final HttpServletResponse resp) throws WebdavException {
    if (debug) {
      trace("PropPatchMethod: doMethod");
    }

    /* Parse any content */
    Document doc = parseContent(req, resp);

    /* Create the node */
    String resourceUri = getResourceUri(req);

    WebdavNsNode node = getNsIntf().getNode(resourceUri,
                                            WebdavNsIntf.existanceMust,
                                            WebdavNsIntf.nodeTypeUnknown);

    if ((node == null) || !node.getExists()) {
      resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
      return;
    }

    if (doc != null) {
      processDoc(req, resp, doc, node, WebdavTags.propertyUpdate, false);
      node.update();
    }
  }

  /** List of properties to set
   *
   */
  public static class PropertySetList extends ArrayList<Element> {
  }

  /** List of properties to remove
   *
   */
  public static class PropertyRemoveList extends ArrayList<Element> {
  }

  /* ====================================================================
   *                   Protected methods
   * ==================================================================== */

  protected void processDoc(final HttpServletRequest req,
                            final HttpServletResponse resp,
                            final Document doc,
                            final WebdavNsNode node,
                            final QName expectedRoot,
                            final boolean onlySet) throws WebdavException {
    try {
      Element root = doc.getDocumentElement();

      if (!XmlUtil.nodeMatches(root, expectedRoot)) {
        throw new WebdavBadRequest();
      }

      Collection<? extends Collection<Element>> setRemoveList = processUpdate(root);
      Collection<WebdavNsNode.SetPropertyResult> failures = new ArrayList<WebdavNsNode.SetPropertyResult>();
      Collection<WebdavNsNode.SetPropertyResult> successes = new ArrayList<WebdavNsNode.SetPropertyResult>();

      for (Collection<Element> sr: setRemoveList) {
        boolean setting = sr instanceof PropPatchMethod.PropertySetList;

        // XXX - possibly inadequate
        /* It's possible changes would conflict, so a later change may
         * invalidate an earlier change.
         */

        for (Element prop: sr) {
          WebdavNsNode.SetPropertyResult spr = new WebdavNsNode.SetPropertyResult(prop, expectedRoot);

          boolean recognized;

          if (setting) {
            recognized = node.setProperty(prop, spr);
          } else {
            if (onlySet) {
              throw new WebdavBadRequest();
            }
            recognized = node.removeProperty(prop, spr);
          }

          if (!recognized) {
            spr.status = HttpServletResponse.SC_NOT_FOUND;
          }

          if (spr.status != HttpServletResponse.SC_OK) {
            failures.add(spr);
          } else {
            successes.add(spr);
          }
        }
      }

      /* Fail whole request for any failure */

      resp.setStatus(WebdavStatusCode.SC_MULTI_STATUS);
      resp.setContentType("text/xml; charset=UTF-8");

      startEmit(resp);

      openTag(WebdavTags.multistatus);

      openTag(WebdavTags.response);
      node.generateHref(xml);

      int status = 0;
      String msg = null;

      if (failures.isEmpty()) {
        status = HttpServletResponse.SC_OK;
      } else {
        // The successes are failed because of the failures
        status = WebdavStatusCode.SC_FAILED_DEPENDENCY;
        msg = "Failed Dependency";

        openTag(WebdavTags.propstat);
        for (WebdavNsNode.SetPropertyResult spr: failures) {
          openTag(WebdavTags.prop);
          emptyTag(spr.prop);
          closeTag(WebdavTags.prop);
          status = spr.status;
          msg = spr.message;
        }
        addStatus(status, msg);
        closeTag(WebdavTags.propstat);
      }

      if (!successes.isEmpty()) {
        openTag(WebdavTags.propstat);
        for (WebdavNsNode.SetPropertyResult spr: successes) {
          openTag(WebdavTags.prop);
          emptyTag(spr.prop);
          closeTag(WebdavTags.prop);
        }
        addStatus(status, msg);
        closeTag(WebdavTags.propstat);
      }

      closeTag(WebdavTags.response);
      closeTag(WebdavTags.multistatus);

      flush();
    } catch (WebdavException wde) {
      throw wde;
    } catch (Throwable t) {
      System.err.println(t.getMessage());
      if (debug) {
        t.printStackTrace();
      }

      throw new WebdavException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }
  }

  /** The given node should contain zero or more set or remove child elements.
   *
   * <p>Each set element should contain zero or more property tags with values.
   *
   * <p>Each remove element should contain zero or more empty property tags.
   *
   * <p>The returned Collection contains zero or more PropertySetList or
   * PropertyRemoveList entries.
   *
   * @param node
   * @return Collection
   * @throws WebdavException
   */
  protected Collection<? extends Collection<Element>> processUpdate(final Element node) throws WebdavException {
    ArrayList<Collection<Element>> res = new ArrayList<Collection<Element>>();

    try {
      Element[] children = getChildrenArray(node);

      for (int i = 0; i < children.length; i++) {
        Element srnode = children[i]; // set or remove
        Collection<Element> plist;

        Element propnode = getOnlyChild(srnode);

        if (!XmlUtil.nodeMatches(propnode, WebdavTags.prop)) {
          throw new WebdavBadRequest();
        }

        if (XmlUtil.nodeMatches(srnode, WebdavTags.set)) {
          plist = new PropertySetList();

          processPlist(plist, propnode, false);
        } else if (XmlUtil.nodeMatches(srnode, WebdavTags.remove)) {
          plist = new PropertyRemoveList();

          processPlist(plist, propnode, true);
        } else {
          throw new WebdavBadRequest();
        }

        res.add(plist);
      }
    } catch (WebdavException wde) {
      throw wde;
    } catch (Throwable t) {
      System.err.println(t.getMessage());
      if (debug) {
        t.printStackTrace();
      }

      throw new WebdavException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    return res;
  }

  /* ====================================================================
   *                   Private methods
   * ==================================================================== */

  /* Process the node which should contain either empty elements for remove or
   * elements with or without values for remove==false
   */
  private void processPlist(final Collection<Element> plist, final Element node,
                            final boolean remove) throws WebdavException {
    Element[] props = getChildrenArray(node);

    for (int i = 0; i < props.length; i++) {
      Element prop = props[i];

      if (remove && !isEmpty(prop)) {
        throw new WebdavBadRequest();
      }

      plist.add(prop);

      /* if (debug) {
        trace("reqtype: " + prop.getLocalName() +
              " ns: " + prop.getNamespaceURI() +
              " value: " + value);
      } */
    }
  }
}

