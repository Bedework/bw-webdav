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

import edu.rpi.cct.webdav.servlet.shared.WebdavBadRequest;
import edu.rpi.cct.webdav.servlet.shared.WebdavException;
import edu.rpi.cct.webdav.servlet.shared.WebdavNsIntf;
import edu.rpi.cct.webdav.servlet.shared.WebdavNsNode;
import edu.rpi.cct.webdav.servlet.shared.WebdavProperty;
import edu.rpi.cct.webdav.servlet.shared.WebdavStatusCode;
import edu.rpi.sss.util.xml.XmlEmit;
import edu.rpi.sss.util.xml.XmlUtil;
import edu.rpi.sss.util.xml.tagdefs.WebdavTags;

import java.util.ArrayList;
import java.util.Collection;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.w3c.dom.Element;

/**
 * @author Mike Douglass
 */
public class PrincipalMatchReport {
  private MethodBase mb;

  private WebdavNsIntf intf;

  private boolean debug;

  protected transient Logger log;

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
   * @param mb
   * @param intf
   * @param debug
   */
  public PrincipalMatchReport(MethodBase mb, WebdavNsIntf intf, boolean debug) {
    this.mb = mb;
    this.intf = intf;
    this.debug = debug;
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
   * @param root
   * @param depth
   * @throws WebdavException
   */
  public void parse(Element root,
                    int depth) throws WebdavException {
    try {
      if (debug) {
        trace("ReportMethod: parsePrincipalMatch");
      }

      Element[] children = intf.getChildren(root);

      int numch = children.length;

      if ((numch < 1) || (numch > 2)) {
        throw new WebdavBadRequest();
      }

      Element curnode = children[0];

      if (XmlUtil.nodeMatches(curnode, WebdavTags.principalProperty)) {
        /* Only match owner for the moment */
        Element[] ppchildren = intf.getChildren(curnode);

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
        if (debug) {
          trace("ReportMethod: self");
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

      if (debug) {
        trace("ReportMethod: do prop");
      }

      props = intf.parseProp(curnode);
    } catch (WebdavException wde) {
      throw wde;
    } catch (Throwable t) {
      warn(t.getMessage());
      if (debug) {
        t.printStackTrace();
      }

      throw new WebdavException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * @param req
   * @param resp
   * @param depth
   * @throws WebdavException
   */
  public void process(HttpServletRequest req,
                      HttpServletResponse resp,
                      int depth) throws WebdavException {
    try {
      resp.setStatus(WebdavStatusCode.SC_MULTI_STATUS);
      resp.setContentType("text/xml; charset=UTF-8");

      XmlEmit xml = intf.getXmlEmit();

      xml.startEmit(resp.getWriter());

      xml.openTag(WebdavTags.multistatus);

      String resourceUri = mb.getResourceUri(req);
      Collection<WebdavNsNode> wdnodes = null;

      if (self) {
        /* Return all groups of which this account is a member
         */
        wdnodes = intf.getGroups(resourceUri, null);
      } else {
        // Search for nodes matching the principal-property element.
        wdnodes = doNodeAndChildren(intf.getNode(resourceUri,
                                                 WebdavNsIntf.existanceMust,
                                                 WebdavNsIntf.nodeTypeUnknown));
      }

      if (wdnodes != null) {
        for (WebdavNsNode nd: wdnodes) {
          xml.openTag(WebdavTags.response);
          nd.generateHref(xml);

          mb.doPropFind(nd, props);

          xml.closeTag(WebdavTags.response);
        }
      }

      xml.closeTag(WebdavTags.multistatus);

      xml.flush();
    } catch (WebdavException wde) {
      throw wde;
    } catch (Throwable t) {
      warn(t.getMessage());
      if (debug) {
        t.printStackTrace();
      }

      throw new WebdavException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }
  }

  private Collection<WebdavNsNode> doNodeAndChildren(WebdavNsNode node)
          throws WebdavException {
    Collection<WebdavNsNode> nodes = new ArrayList<WebdavNsNode>();

    if (!nodeMatches(node)) {
      // Stop here?
      return nodes;
    }

    if (!node.isCollection()) {
      nodes.add(node);
      return nodes;
    }

    for (WebdavNsNode child: intf.getChildren(node)) {
      nodes.addAll(doNodeAndChildren(child));
    }

    return nodes;
  }

  private boolean nodeMatches(WebdavNsNode node) throws WebdavException {
    if (owner) {
      String account = intf.getAccount();

      if (account == null) {
        return false;
      }
      return account.equals(node.getOwner());
    }

    return false;
  }

  protected Logger getLogger() {
    if (log == null) {
      log = Logger.getLogger(this.getClass());
    }

    return log;
  }

  protected void trace(String msg) {
    getLogger().debug(msg);
  }

  protected void warn(String msg) {
    getLogger().warn(msg);
  }
}
