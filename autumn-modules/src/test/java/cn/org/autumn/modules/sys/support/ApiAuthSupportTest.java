package cn.org.autumn.modules.sys.support;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

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
}
