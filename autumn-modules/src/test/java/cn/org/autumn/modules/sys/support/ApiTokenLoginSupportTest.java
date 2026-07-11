package cn.org.autumn.modules.sys.support;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class ApiTokenLoginSupportTest {

    @Test
    public void normalizeLoginTokenUnwrapsJsonBearer() {
        String raw = "Bearer {\"access_token\":\"oauth-at\",\"token_type\":\"bearer\"}";
        assertEquals("oauth-at", ApiTokenLoginSupport.normalizeLoginToken(raw));
    }

    @Test
    public void normalizeLoginTokenKeepsPlain() {
        assertEquals("plain-at", ApiTokenLoginSupport.normalizeLoginToken("plain-at"));
    }

    @Test
    public void resolveUserUuidRejectsOAuthErrorJson() {
        String errorJson = "{\"error\":\"invalid_grant\",\"error_description\":\"未获得授权\"}";
        assertFalse(ApiTokenLoginSupport.isLoginableApiToken(errorJson));
    }
}
