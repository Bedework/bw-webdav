package org.bedework.webdav.servlet.common;

import org.bedework.webdav.servlet.shared.WebdavBadRequest;
import org.bedework.webdav.servlet.shared.WebdavException;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.Reader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

/**
 * Contributed by https://github.com/viqueen
 */
public interface SecureXml {
  default Document parseXmlSafely(final int contentLength,
                                  final Reader reader) {
    if (contentLength == 0) {
      return null;
    }

    if (reader == null) {
      // No content?
      return null;
    }

    try {
      final DocumentBuilderFactory factory =
              DocumentBuilderFactory.newInstance();
      factory.setNamespaceAware(true);
      factory.setFeature(
              "http://javax.xml.XMLConstants/feature/secure-processing",
              true);
      factory.setFeature(
              "http://xml.org/sax/features/external-general-entities",
              false);
      factory.setFeature(
              "http://xml.org/sax/features/external-parameter-entities",
              false);
      factory.setAttribute(
              "http://apache.org/xml/features/nonvalidating/load-external-dtd",
              false);

      final DocumentBuilder builder = factory.newDocumentBuilder();

      return builder.parse(new InputSource(reader));
    } catch (final SAXException exception) {
      throw new WebdavBadRequest(exception.getMessage());
    } catch (final Throwable t) {
      throw new WebdavException(t);
    }
  }
}
