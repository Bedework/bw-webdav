/* **********************************************************************
    Copyright 2007 Rensselaer Polytechnic Institute. All worldwide rights reserved.

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

import edu.rpi.cct.webdav.servlet.shared.WebdavException;
import edu.rpi.cmt.access.AccessXmlUtil;
import edu.rpi.sss.util.xml.QName;
import edu.rpi.sss.util.xml.XmlEmit;

/**
 * @author douglm
 *
 */
public class AccessUtil extends AccessXmlUtil {
  private String namespacePrefix;

  /** Acls use tags in the webdav and caldav namespace. For use over caldav
   * we should supply the uris. Otherwise a null namespace will be used.
   *
   * @param namespacePrefix String prefix
   * @param xml   XmlEmit
   * @param cb
   * @param debug
   * @throws WebdavException
   */
  public AccessUtil(String namespacePrefix, XmlEmit xml,
                    AccessXmlCb cb,
                    boolean debug) throws WebdavException {
    super(caldavPrivTags, xml, cb, debug);

    this.namespacePrefix = namespacePrefix;
  }

  /** Override this to construct urls from the parameter
   *
   * @param who String
   * @return String href
   */
  public String makeUserHref(String who) {
    return namespacePrefix + "/principals/users/" + who;
  }

  /** Override this to construct urls from the parameter
   *
   * @param who String
   * @return String href
   */
  public String makeGroupHref(String who) {
    return namespacePrefix + "/principals/groups/" + who;
  }

  /**
   * @return QName[]
   */
  public QName[] getPrivTags() {
    return caldavPrivTags;
  }
}
