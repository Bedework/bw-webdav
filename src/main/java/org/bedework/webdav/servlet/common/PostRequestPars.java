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
import org.bedework.webdav.servlet.shared.WebdavBadRequest;
import org.bedework.webdav.servlet.shared.WebdavException;
import org.bedework.webdav.servlet.shared.WebdavNsIntf;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.Reader;

import javax.servlet.http.HttpServletRequest;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

/**
 */
public class PostRequestPars {
  private final HttpServletRequest req;

  private final WebdavNsIntf intf;

  private final String method;

  private final String resourceUri;

  private String noPrefixResourceUri;

  private final String acceptType;

  private String contentType;

  private String[] contentTypePars;

  private Reader reqRdr;

  /** Set if the content type is xml */
  private Document xmlDoc;

  protected boolean addMember;

  protected boolean getTheReader = true;

  /**
   * @param req - the request
   * @param intf service interface
   * @param resourceUri the uri
   * @throws WebdavException
   */
  public PostRequestPars(final HttpServletRequest req,
                         final WebdavNsIntf intf,
                         final String resourceUri) throws WebdavException {
    this.req = req;
    this.intf = intf;
    this.resourceUri = resourceUri;

    method = req.getMethod();

    acceptType = req.getHeader("ACCEPT");

    contentType = req.getContentType();

    if (contentType != null) {
      contentTypePars = contentType.split(";");
    }
  }

  /**
   * @return true if we recognized the request as a particular type
   * @throws WebdavException
   */
  public boolean processRequest() throws WebdavException {
    final String addMemberSuffix = intf.getAddMemberSuffix();

    if (addMemberSuffix == null) {
      return false;
    }

    final String reqUri = req.getRequestURI();

    if (reqUri == null) {
      return false;
    }

    final int pos = reqUri.lastIndexOf("/");

    if ((pos > 0) && reqUri.regionMatches(pos + 1,
                                          addMemberSuffix,
                                          0,
                                          addMemberSuffix.length())) {
      addMember = true;
      return true;
    }

    return false;
  }

  public boolean processXml() throws WebdavException {
    if (!isAppXml()) {
      return false;
    }

    try {
      reqRdr = req.getReader();
    } catch (final Throwable t) {
      throw new WebdavException(t);
    }

    xmlDoc = parseXml(reqRdr);
    getTheReader = false;
    return true;
  }

  /**
   * @return Reader if we have a usable reader
   * @throws WebdavException
   */
  public Reader getReader() throws WebdavException {
    if (!getTheReader) {
      // Reader already processed
      return null;
    }

    if (reqRdr != null) {
      return reqRdr;
    }

    try {
      reqRdr = req.getReader();
    } catch (final Throwable t) {
      throw new WebdavException(t);
    }

    return reqRdr;
  }

  /**
   * @return current request
   */
  public HttpServletRequest getReq() {
    return req;
  }

  public String getMethod() {
    return method;
  }

  /**
   * @return the resource uri
   */
  public String getResourceUri() {
    return resourceUri;
  }

  /** If this request is for a special URI this is the resource URI without
   * the prefix and any parameters. It may be an empty string but will not be null.
   * <p>for example /ischedule/domainkey/... will become /domainkey/...

   * @return unprefixed special uri
   */
  public String getNoPrefixResourceUri() {
    return noPrefixResourceUri;
  }

  /** from accept header
   * @return value from accept header
   */
  public String getAcceptType() {
    return acceptType;
  }

  /**
   * @return the content type
   */
  public String getContentType() {
    return contentType;
  }

  /**
   * @return Broken out content type
   */
  public String[] getContentTypePars() {
    return contentTypePars;
  }

  /**
   * @return parsed XML
   */
  public Document getXmlDoc() {
    return xmlDoc;
  }

  /**
   * @return true if this is DAV:add-member POST
   */
  public boolean isAddMember() {
    return addMember;
  }

  /**
   * @param rdr for xml content
   * @return parsed Document
   * @throws WebdavException
   */
  private Document parseXml(final Reader rdr) throws WebdavException{
    if (rdr == null) {
      return null;
    }

    try {
      final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setNamespaceAware(true);

      final DocumentBuilder builder = factory.newDocumentBuilder();

      return builder.parse(new InputSource(rdr));
    } catch (final SAXException e) {
      throw new WebdavBadRequest();
    } catch (final Throwable t) {
      throw new WebdavException(t);
    }
  }

  /**
   * @return true if we have an xml content
   */
  public boolean isAppXml() {
    return contentTypePars != null &&
            (contentTypePars[0].equals("application/xml") ||
                     contentTypePars[0].equals("text/xml"));
  }

  /**
   * @param val - the content type
   */
  public void setContentType(final String val) {
    contentType = val;
  }

  /**
   * @param specialUri the uri to cehck for
   * @return true if the request uri is a special uri
   */
  protected boolean checkUri(final String specialUri) {
    if (specialUri == null) {
      return false;
    }

    final String toMatch = Util.buildPath(true, specialUri);
    final String prefix;

    final int pos = resourceUri.indexOf("/", 1);

    if (pos < 0) {
      prefix = noParameters(resourceUri);
    } else {
      prefix = resourceUri.substring(0, pos);
    }

    if (!toMatch.equals(Util.buildPath(true, prefix))) {
      noPrefixResourceUri = noParameters(resourceUri);
      return false;
    }

    if (pos < 0) {
      noPrefixResourceUri = "";
    } else {
      noPrefixResourceUri = noParameters(resourceUri.substring(pos));
    }

    return true;
  }

  private String noParameters(String uri) {
    final int pos = uri.indexOf("?");
    if (pos > 0) {
      uri = uri.substring(0, pos);
    }

    if (uri.equals("/")) {
      uri = "";
    }

    return uri;
  }
}