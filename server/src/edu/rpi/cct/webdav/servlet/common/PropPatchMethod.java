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

import edu.rpi.cct.webdav.servlet.shared.WebdavBadRequest;
import edu.rpi.cct.webdav.servlet.shared.WebdavException;
import edu.rpi.cct.webdav.servlet.shared.WebdavNsIntf;
import edu.rpi.cct.webdav.servlet.shared.WebdavNsNode;

import java.util.ArrayList;
import java.util.Collection;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/** Class called to handle PROPPATCH
 *
 *   @author Mike Douglass   douglm@rpi.edu
 */
public class PropPatchMethod extends MethodBase {
  /** Called at each request
   */
  public void init() {
  }

  public void doMethod(HttpServletRequest req,
                        HttpServletResponse resp) throws WebdavException {
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

    if (doc != null) {
      processDoc(req, doc, node);
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

  protected void processDoc(HttpServletRequest req,
                            Document doc,
                            WebdavNsNode node) throws WebdavException {
    try {
      Element root = doc.getDocumentElement();

      if (!WebdavTags.propertyUpdate.nodeMatches(root)) {
        throw new WebdavBadRequest();
      }

      Collection<? extends Collection<Element>> setRemoveList = processUpdate(root);

      for (Collection<Element> sr: setRemoveList) {
        boolean setting = sr instanceof PropPatchMethod.PropertySetList;

        for (Element prop: sr) {
          if (setting) {
            node.setProperty(prop);
          } else {
            node.removeProperty(prop);
          }
        }
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
  protected Collection<? extends Collection<Element>> processUpdate(Element node) throws WebdavException {
    ArrayList<Collection<Element>> res = new ArrayList<Collection<Element>>();

    try {
      Element[] children = getChildren(node);

      for (int i = 0; i < children.length; i++) {
        Element curnode = children[i];
        PropertySetList plist;

        if (WebdavTags.set.nodeMatches(curnode)) {
          plist = new PropertySetList();

          processPlist(plist, curnode, false);
        } else if (WebdavTags.remove.nodeMatches(curnode)) {
          plist = new PropertySetList();

          processPlist(plist, curnode, true);
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
  private void processPlist(Collection<Element> plist, Element node,
                            boolean remove) throws WebdavException {
    Element[] props = getChildren(node);

    for (int i = 0; i < props.length; i++) {
      Element prop = props[i];

      String value = getElementContent(prop);

      if (remove && (value != null)) {
        throw new WebdavBadRequest();
      }

      plist.add(prop);

      if (debug) {
        trace("reqtype: " + prop.getLocalName() +
              " ns: " + prop.getNamespaceURI() +
              " value: " + value);
      }
    }
  }
}

