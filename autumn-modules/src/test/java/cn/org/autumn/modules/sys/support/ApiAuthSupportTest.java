package cn.org.autumn.modules.sys.support;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;

public class ApiAuthSupportTest {

    @Test
    public void normalizeBearerUnwrapsJsonTokenResponse() {
        String raw = "Bearer {\"access_token\":\"legacy-wrap\",\"refresh_token\":\"rt\",\"token_type\":\"bearer\"}";
        assertEquals("legacy-wrap", ApiAuthSupport.normalizeBearer(raw));
    }

    @Test
    public void normalizeBearerKeepsPlainToken() {
        assertEquals("plain-token", ApiAuthSupport.normalizeBearer("Bearer plain-token"));
    }

    @Test
    public void extractCredentialPrefersRobotHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Robot-Token", "rbt_abc");
        request.addHeader("Authorization", "Bearer other");
        ApiCredential cred = ApiAuthSupport.extractCredential(request);
        assertTrue(cred.isPresent());
        assertEquals("X-Robot-Token", cred.getHeader());
        assertEquals("rbt_abc", cred.getToken());
    }

    @Test
    public void extractCredentialUsesAuthorization() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer plain-token");
        ApiCredential cred = ApiAuthSupport.extractCredential(request);
        assertEquals("Authorization", cred.getHeader());
        assertEquals("plain-token", cred.getToken());
    }
}
