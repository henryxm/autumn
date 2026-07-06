package cn.org.autumn.modules.oauth.oauth2.support;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;

public class OAuthConsentCsrfSupportTest {

    @Test
    public void issueValidateAndConsume() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        String token = OAuthConsentCsrfSupport.issue(request);
        assertTrue(OAuthConsentCsrfSupport.validateAndConsume(request, token));
        assertFalse(OAuthConsentCsrfSupport.validateAndConsume(request, token));
    }

    @Test
    public void rejectBlankToken() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        OAuthConsentCsrfSupport.issue(request);
        assertFalse(OAuthConsentCsrfSupport.validateAndConsume(request, ""));
    }
}
