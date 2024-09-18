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

import org.bedework.util.logging.BwLogger;
import org.bedework.util.logging.Logged;
import org.bedework.util.xml.XmlEmit;
import org.bedework.util.xml.XmlUtil;
import org.bedework.util.xml.tagdefs.WebdavTags;
import org.bedework.webdav.servlet.shared.WebdavBadRequest;
import org.bedework.webdav.servlet.shared.WebdavException;
import org.bedework.webdav.servlet.shared.WebdavNsIntf;
import org.bedework.webdav.servlet.shared.WebdavNsNode;
import org.bedework.webdav.servlet.shared.WebdavProperty;
import org.bedework.webdav.servlet.shared.WebdavStatusCode;

import org.w3c.dom.Element;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author Mike Douglass
 */
public class PrincipalMatchReport implements Logged {
  private final MethodBase mb;

  private final WebdavNsIntf intf;

  /** Match a resource which identifies the current user
   */
  public boolean self;

  /** Match a resource for which the current user is the owner
   */
  public boolean owner;

  /** Match a resource which identifies the current user
   */
  public boolean whoami;

  /** Property we're supposed to match on */
  public Element principalProperty;

  /** Properties to return (none for empty collection)
   */
  public Collection<WebdavProperty> props = new ArrayList<WebdavProperty>();

  /** Constructor
   *
   * @param mb method base
   * @param intf interface
   */
  public PrincipalMatchReport(final MethodBase mb, final WebdavNsIntf intf) {
    this.mb = mb;
    this.intf = intf;
  }

  /** Parse the principal match request.
   *
   *    <!ELEMENT principal-match ((principal-property | self), prop?)>
   *
   *    <!ELEMENT principal-property ANY>
   *
   *    ANY value: an element whose value identifies a property. The
   *    expectation is the value of the named property typically contains
   *    an href element that contains the URI of a principal
   *    <!ELEMENT self EMPTY>
   *
   * @param root of request
   * @param depth how far down
   */
  public void parse(final Element root,
                    final int depth) {
    try {
      if (debug()) {
        debug("ReportMethod: parsePrincipalMatch");
      }

      final Element[] children = intf.getChildren(root);

      final int numch = children.length;

      if ((numch < 1) || (numch > 2)) {
        throw new WebdavBadRequest();
      }

      Element curnode = children[0];

      if (XmlUtil.nodeMatches(curnode, WebdavTags.principalProperty)) {
        /* Only match owner for the moment */
        final Element[] ppchildren = intf.getChildren(curnode);

        if (ppchildren.length != 1) {
          throw new WebdavBadRequest();
        }

        if (XmlUtil.nodeMatches(ppchildren[0], WebdavTags.owner)) {
          owner = true;
        } else if (XmlUtil.nodeMatches(ppchildren[0], WebdavTags.whoami)) {
          // XXX probably wrong - we should just store property name and
          // use when processing.
          whoami = true;
        } else {
          principalProperty = ppchildren[0];
        }
      } else if (XmlUtil.nodeMatches(curnode, WebdavTags.self)) {
        if (debug()) {
          debug("ReportMethod: self");
        }

        self = true;
      } else {
        throw new WebdavBadRequest();
      }

      if (numch == 1) {
        return;
      }

      curnode = children[1];

      if (!XmlUtil.nodeMatches(curnode, WebdavTags.prop)) {
        throw new WebdavBadRequest();
      }

      if (debug()) {
        debug("ReportMethod: do prop");
      }

      props = intf.parseProp(curnode);
    } catch (final WebdavException wde) {
      throw wde;
    } catch (final Throwable t) {
      warn(t.getMessage());
      if (debug()) {
        t.printStackTrace();
      }

      throw new WebdavException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * @param req http request
   * @param resp http response
   * @param depth for search
   */
  public void process(final HttpServletRequest req,
                      final HttpServletResponse resp,
                      final int depth) {
      resp.setStatus(WebdavStatusCode.SC_MULTI_STATUS);
      resp.setContentType("text/xml; charset=UTF-8");

      final XmlEmit xml = intf.getXmlEmit();

    try {
      xml.startEmit(resp.getWriter());
    } catch (final IOException e) {
      throw new WebdavException(e);
    }

    xml.openTag(WebdavTags.multistatus);

      final String resourceUri = mb.getResourceUri(req);
      final Collection<WebdavNsNode> wdnodes;

      if (self) {
        /* Return all groups of which this account is a member
         */
        wdnodes = intf.getGroups(resourceUri, null);
      } else {
        // Search for nodes matching the principal-property element.
        wdnodes = doNodeAndChildren(intf.getNode(resourceUri,
                                                 WebdavNsIntf.existanceMust,
                                                 WebdavNsIntf.nodeTypeUnknown,
                                                 false));
      }

      if (wdnodes != null) {
        for (final WebdavNsNode nd: wdnodes) {
          xml.openTag(WebdavTags.response);
          nd.generateHref(xml);

          mb.doPropFind(nd, props);

          xml.closeTag(WebdavTags.response);
        }
      }

      xml.closeTag(WebdavTags.multistatus);

      xml.flush();
  }

  private Collection<WebdavNsNode> doNodeAndChildren(final WebdavNsNode node) {
    final Collection<WebdavNsNode> nodes = new ArrayList<>();

    if (!nodeMatches(node)) {
      // Stop here?
      return nodes;
    }

    if (!node.isCollection()) {
      nodes.add(node);
      return nodes;
    }

    for (final WebdavNsNode child: intf.getChildren(node, null)) {
      nodes.addAll(doNodeAndChildren(child));
    }

    return nodes;
  }

  private boolean nodeMatches(final WebdavNsNode node) {
    if (owner) {
      final String account = intf.getAccount();

      if (account == null) {
        return false;
      }
      return account.equals(node.getOwner().getAccount());
    }

    return false;
  }

  /* ====================================================================
   *                   Logged methods
   * ==================================================================== */

  private BwLogger logger = new BwLogger();

  @Override
  public BwLogger getLogger() {
    if ((logger.getLoggedClass() == null) && (logger.getLoggedName() == null)) {
      logger.setLoggedClass(getClass());
    }

    return logger;
  }
}
