/* **********************************************************************
    Copyright 2006 Rensselaer Polytechnic Institute. All worldwide rights reserved.

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

import java.util.ArrayList;
import java.util.Collection;

import edu.rpi.cct.webdav.servlet.shared.PrincipalPropertySearch;
import edu.rpi.cct.webdav.servlet.shared.WebdavBadRequest;
import edu.rpi.cct.webdav.servlet.shared.WebdavException;
import edu.rpi.cct.webdav.servlet.shared.WebdavNsIntf;
import edu.rpi.cct.webdav.servlet.shared.WebdavStatusCode;
import edu.rpi.cct.webdav.servlet.shared.PrincipalPropertySearch.PropertySearch;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.bedework.davdefs.WebdavTags;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/** Class called to handle POST
 *
 *   @author Mike Douglass   douglm@rpi.edu
 */
public class ReportMethod extends MethodBase {
  private final static int reportTypeExpandProperty = 0;
  private final static int reportTypePrincipalPropertySearch = 1;

  private int reportType;

  PrincipalPropertySearch pps;

  /** Called at each request
   */
  public void init() {
  }

  public void doMethod(HttpServletRequest req,
                       HttpServletResponse resp) throws WebdavException {
    if (debug) {
      trace("ReportMethod: doMethod");
    }

    Document doc = parseContent(req, resp);

    if (doc == null) {
      return;
    }

    int depth = Headers.depth(req);

    if (debug) {
      trace("ReportMethod: depth=" + depth);
    }

    process(doc, depth, req, resp);
  }

  /* We process the parsed document and produce a response
   *
   * @param doc
   * @throws WebdavException
   */
  protected void process(Document doc,
                         int depth,
                         HttpServletRequest req,
                         HttpServletResponse resp) throws WebdavException {
    reportType = getReportType(doc);

    if (reportType < 0) {
      throw new WebdavBadRequest();
    }

    processDoc(doc);

    processResp(req, resp, depth);
  }

  /* ====================================================================
   *                   Private methods
   * ==================================================================== */

  /* We process the parsed document and produce a Collection of request
   * objects to process.
   *
   * @param doc
   * @throws WebdavException
   */
  private void processDoc(Document doc) throws WebdavException {
    try {
      WebdavNsIntf intf = getNsIntf();

      Element root = doc.getDocumentElement();

      Element[] children = getChildren(root);

      if (reportType == reportTypeExpandProperty) {
        return;
      }

      /*
       <!ELEMENT principal-property-search
       ((property-search+), prop?, apply-to-principal-collection-set?) >

       <!ELEMENT property-search (prop, match) >
       prop: see RFC 2518, Section 12.11

       <!ELEMENT match #PCDATA >

       */

      pps = new PrincipalPropertySearch();

      for (int i = 0; i < children.length; i++) {
        Element curnode = children[i];

        if (WebdavTags.propertySearch.nodeMatches(curnode)) {
          PropertySearch ps = new PropertySearch();

          pps.propertySearches.add(ps);

          Element[] pschildren = getChildren(curnode);

          if (pschildren.length != 2) {
            throw new WebdavBadRequest();
          }

          ps.props = intf.parseProp(pschildren[0]);
          ps.match = pschildren[1];
        } else {
          i++;

          if (i < children.length) {
            if (WebdavTags.prop.nodeMatches(curnode)) {
              pps.returnProps = intf.parseProp(children[i]);
              i++;
            }

            if (i < children.length) {
              if (!WebdavTags.applyToPrincipalCollectionSet.nodeMatches(curnode)) {
                throw new WebdavBadRequest();
              }

              pps.applyToPrincipalCollectionSet = true;
              i++;
            }
          }

          if (i < children.length) {
            throw new WebdavBadRequest();
          }

          break;
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

  /**
   * @param req
   * @param resp
   * @param depth
   * @throws WebdavException
   */
  private void processResp(HttpServletRequest req,
                           HttpServletResponse resp,
                           int depth) throws WebdavException {

    if (reportType == reportTypeExpandProperty) {
      return;
    }

    // reportTypePrincipalPropertySearch

    startEmit(resp);

    if (pps != null) {
      if (depth != 0) {
        throw new WebdavBadRequest();
      }
    }

    resp.setStatus(WebdavStatusCode.SC_MULTI_STATUS);
    resp.setContentType("text/xml; charset=UTF-8");

    String resourceUri = getResourceUri(req);

    openTag(WebdavTags.multistatus);

    closeTag(WebdavTags.multistatus);

    flush();
  }

  /** See if we recognize this report type and return an index.
   *
   * @param doc
   * @return index or <0 for unknown.
   * @throws WebdavException
   */
  private int getReportType(Document doc) throws WebdavException {
    try {
      Element root = doc.getDocumentElement();

      if (WebdavTags.expandProperty.nodeMatches(root)) {
        return reportTypeExpandProperty;
      }
      if (WebdavTags.principalPropertySearch.nodeMatches(root)) {
        return reportTypePrincipalPropertySearch;
      }

      return -1;
    } catch (Throwable t) {
      System.err.println(t.getMessage());
      if (debug) {
        t.printStackTrace();
      }

      throw new WebdavException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }
  }
}

