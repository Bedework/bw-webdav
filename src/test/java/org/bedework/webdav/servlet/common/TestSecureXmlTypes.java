package org.bedework.webdav.servlet.common;

import org.bedework.webdav.servlet.shared.WebdavException;
import org.bedework.webdav.servlet.shared.WebdavNsIntf;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.UUID;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

public class TestSecureXmlTypes {

  @Rule
  public MockitoRule mockitoRule = MockitoJUnit.rule();

  @Mock
  private HttpServletRequest request;
  @Mock
  private HttpServletResponse response;

  @Mock(answer = Answers.CALLS_REAL_METHODS)
  private WebdavNsIntf webdavNsIntf;

  @Mock(answer = Answers.CALLS_REAL_METHODS)
  private MethodBase methodBase;

  private PostRequestPars requestPars;

  @Before
  public void setup() throws WebdavException, IOException {
    when(request.getContentType()).thenReturn("application/xml");
    requestPars = new PostRequestPars(request, webdavNsIntf, UUID.randomUUID().toString());

    when(methodBase.getNsIntf()).thenReturn(webdavNsIntf);

    when(request.getContentLength()).thenReturn(1);
    when(request.getReader()).thenReturn(getResource("/malicious-request.xml"));
  }

  @Test
  public void testPostRequestParsWithMaliciousRequest() throws WebdavException {
    assertTrue(requestPars.processXml());
  }

  @Test
  public void testMethodBaseWithMaliciousRequest() throws WebdavException {
    assertNotNull(methodBase.parseContent(request, response));
  }

  private BufferedReader getResource(final String name) {
    return new BufferedReader(
        new InputStreamReader(this.getClass().getResourceAsStream(name))
    );
  }
}
