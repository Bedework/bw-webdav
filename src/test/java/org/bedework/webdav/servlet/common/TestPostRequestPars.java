package org.bedework.webdav.servlet.common;

import org.bedework.webdav.servlet.shared.WebdavException;
import org.bedework.webdav.servlet.shared.WebdavNsIntf;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.UUID;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

public class TestPostRequestPars {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private HttpServletRequest request;
    @Mock
    private WebdavNsIntf webdavNsIntf;

    private PostRequestPars requestPars;

    @Before
    public void setup() throws WebdavException {
        when(request.getContentType()).thenReturn("application/xml");
        requestPars = new PostRequestPars(request, webdavNsIntf, UUID.randomUUID().toString());
    }

    @Test
    public void testWithMaliciousRequest() throws IOException, WebdavException {
        when(request.getReader()).thenReturn(getResource("/malicious-request.xml"));
        assertTrue(requestPars.processXml());
    }

    private BufferedReader getResource(final String name) {
        return new BufferedReader(
                new InputStreamReader(this.getClass().getResourceAsStream(name))
        );
    }
}
