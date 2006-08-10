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

package edu.rpi.cct.webdav.servlet.shared;

import edu.rpi.sss.util.xml.QName;

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;

/** One of these for each property in a request.
 *
 *   @author Mike Douglass   douglm@rpi.edu
 */
public class WebdavProperty implements Serializable {
  private QName tag;
  private String pval;

  /** Constructor
   *
   * @param tag  QName name
   * @param pval String value
   */
  public WebdavProperty(QName tag,
                        String pval) {
    this.tag = tag;
    this.pval = pval;
  }

  /**
   * @param val
   */
  public void setTag(QName val) {
    tag = val;
  }

  /**
   * @return QName tage name
   */
  public QName getTag() {
    return tag;
  }

  /**
   * @param val
   */
  public void setPval(String val) {
    pval = val;
  }

  /**
   * @return STring value
   */
  public String getPval() {
    return pval;
  }

  /** Convenience method to provide an Iterator over these objects
   *
   * @param c
   * @return Iterator
   */
  public static Iterator iterator(Collection c) {
    WebdavProperty[] ps = (WebdavProperty[])c.toArray(
            new WebdavProperty[c.size()]);
    return new PropertyIterator(ps);
  }

  private static class PropertyIterator implements Iterator {
    WebdavProperty[] ps = null;
    int index;

    /** COnstructor
     *
     * @param ps
     */
    public PropertyIterator(WebdavProperty[] ps) {
      this.ps = ps;
    }

    public boolean hasNext() {
      if ((ps == null) ||
          (index >= ps.length)) {
        return false;
      }

      return true;
    }

    public Object next() {
      if ((ps == null) ||
          (index >= ps.length)) {
        return null;
      }

      WebdavProperty p = ps[index];
      index++;

      return p;
    }

    public void remove() {
      throw new RuntimeException("Unimplemented");
    }
  }
}
