/* **********************************************************************
    Copyright 2010 Rensselaer Polytechnic Institute. All worldwide rights reserved.

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
import edu.rpi.cct.webdav.servlet.shared.WebdavNsIntf;
import edu.rpi.cct.webdav.servlet.shared.WebdavNsNode;

import java.io.CharArrayReader;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Class called to handle GET
 *
 * Get the content of a node. Note this is subclassed by HeadMethod which
 * overrides init and sets doContent false.
 *
 *   @author Mike Douglass   douglm@rpi.edu
 */
public class GetMethod extends MethodBase {
  protected boolean doContent;

  /** size of buffer used for copying content to response.
   */
  private static final int bufferSize = 4096;

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.common.MethodBase#init()
   */
  @Override
  public void init() {
    doContent = true;
  }

  @Override
  public void doMethod(final HttpServletRequest req,
                       final HttpServletResponse resp) throws WebdavException {
    if (debug) {
      trace("GetMethod: doMethod");
    }

    try {
      WebdavNsIntf intf = getNsIntf();

      if (intf.specialUri(req, resp, getResourceUri(req))) {
        return;
      }

      //String reqContentType = req.getContentType();
      //boolean reqHtml = "text/html".equals(reqContentType);

      WebdavNsNode node = intf.getNode(getResourceUri(req),
                                       WebdavNsIntf.existanceMust,
                                       WebdavNsIntf.nodeTypeUnknown);

      if ((node == null) || !node.getExists()) {
        resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
        return;
      }

      if (!intf.prefetch(req, resp, node)) {
        return;
      }

      Reader in = null;

      /* For binary */
      InputStream streamIn = null;

      /** Get the content now to set up length, type etc.
       */
      String contentType;
      long contentLength;

      if (node.getContentBinary()) {
        streamIn = intf.getBinaryContent(node);
        contentType = node.getContentType();
        contentLength = node.getContentLen();
      } else if (node.isCollection()) {
        if (intf.getDirectoryBrowsingDisallowed()) {
          throw new WebdavException(HttpServletResponse.SC_FORBIDDEN);
        }

        String content = generateHtml(req, node);
        in = new CharArrayReader(content.toCharArray());
        contentType = "text/html";
        contentLength = content.getBytes().length;
      } else {
        in = intf.getContent(req, resp, node);
        contentType = node.getContentType();
        contentLength = node.getContentLen();
      }

      resp.setHeader("ETag", node.getEtagValue(true));

      if (node.getLastmodDate() != null) {
        resp.addHeader("Last-Modified", node.getLastmodDate().toString());
      }

      resp.setContentType(contentType);

      if (contentLength > Integer.MAX_VALUE) {
        resp.setContentLength(-1);
      } else {
        resp.setContentLength((int)contentLength);
      }

      if (doContent) {
        if ((in == null) && (streamIn == null)) {
          if (debug) {
            debugMsg("status: " + HttpServletResponse.SC_NO_CONTENT);
          }

          resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
        } else {
          if (debug) {
            debugMsg("send content - length=" + node.getContentLen());
          }
          if (node.getContentBinary()) {
          } else {
          }

          if (node.getContentBinary()) {
            streamContent(streamIn, resp.getOutputStream());
          } else {
            writeContent(in, resp.getWriter());
          }
        }
      }
    } catch (WebdavException we) {
      throw we;
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  private void writeContent(final Reader in, final Writer out)
      throws WebdavException {
    try {
      char[] buff = new char[bufferSize];
      int len;

      while (true) {
        len = in.read(buff);

        if (len < 0) {
          break;
        }

        out.write(buff, 0, len);
      }
    } catch (Throwable t) {
      throw new WebdavException(t);
    } finally {
      try {
        in.close();
      } catch (Throwable t) {}
      try {
        out.close();
      } catch (Throwable t) {}
    }
  }

  private void streamContent(final InputStream in, final OutputStream out)
      throws WebdavException {
    try {
      byte[] buff = new byte[bufferSize];
      int len;

      while (true) {
        len = in.read(buff);

        if (len < 0) {
          break;
        }

        out.write(buff, 0, len);
      }
    } catch (Throwable t) {
      throw new WebdavException(t);
    } finally {
      try {
        in.close();
      } catch (Throwable t) {}
      try {
        out.close();
      } catch (Throwable t) {}
    }
  }

  /** Return a String giving an HTML representation of the directory.
   *
   * TODO
   *
   * <p>Use some form of template to generate an internationalized form of the
   * listing. We don't need a great deal to start with. It will also allow us to
   * provide stylesheets, images etc. Probably place it in the resources directory.
   *
   * @param req
   * @param node  WebdavNsNode
   * @return Reader
   * @throws WebdavException
   */
  protected String generateHtml(final HttpServletRequest req,
                                final WebdavNsNode node) throws WebdavException {
    try {
      Sbuff sb = new Sbuff();

      sb.lines(new String[] {"<html>",
                             "  <head>"});
      /* Need some styles I guess */
      sb.append("    <title>");
      sb.append(node.getDisplayname());
      sb.line("</title>");

      sb.lines(new String[] {"</head>",
                             "<body>"});

      sb.append("    <h1>");
      sb.append(node.getDisplayname());
      sb.line("</h1>");

      sb.line("  <hr>");

      sb.line("  <table width=\"100%\" " +
              "cellspacing=\"0\"" +
              " cellpadding=\"4\">");

      for (WebdavNsNode child: getNsIntf().getChildren(node)) {
        /* icon would be nice */

        sb.line("<tr>");

        if (node.isCollection()) {
          /* folder */
        } else {
          /* calendar? */
        }

        sb.line("  <td align=\"left\">");
        sb.append("<a href=\"");
        sb.append(req.getContextPath());
        sb.append(child.getUri());
        sb.append("\">");
        sb.append(child.getDisplayname());
        sb.line("</a>");
        sb.line("</td>");

        sb.line("  <td align=\"left\">");

        String lastMod = child.getLastmodDate();

        if (lastMod != null) {
          sb.line(lastMod);
        } else {
          sb.line("&nbsp;");
        }
        sb.line("</td>");
        sb.append("</tr>\r\n");
      }

      sb.line("</table>");

      /* Could use a footer */
      sb.line("</body>");
      sb.line("</html>");

      return sb.toString();
    } catch (WebdavException we) {
      throw we;
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  private static class Sbuff {
    StringBuilder sb = new StringBuilder();

    /**
     * @param ss
     */
    public void lines(final String[] ss) {
      for (int i = 0; i < ss.length; i++) {
        line(ss[i]);
      }
    }

    /**
     * @param s
     */
    public void line(final String s) {
      sb.append(s);
      sb.append("\r\n");
    }

    /**
     * @param s
     */
    public void append(final String s) {
      sb.append(s);
    }

    @Override
    public String toString() {
      return sb.toString();
    }
  }

}

