package cn.org.autumn.modules.oauth.oauth2.support;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class OAuthTokenResponseParserTest {

    @Test
    public void parseTokenBody() {
        String body = "{\"access_token\":\"at1\",\"refresh_token\":\"rt1\",\"token_type\":\"bearer\",\"expires_in\":3600}";
        OAuthTokenResponse parsed = OAuthTokenResponseParser.parse(body);
        assertEquals("at1", parsed.getAccessToken());
        assertEquals("rt1", parsed.getRefreshToken());
        assertEquals("bearer", parsed.getTokenType());
        assertEquals(3600L, parsed.getExpiresIn());
    }

    @Test
    public void extractJsonWrappedToken() {
        String raw = "{\"access_token\":\"abc123\",\"expires_in\":86400}";
        assertTrue(OAuthTokenResponseParser.isJsonWrappedToken(raw));
        assertEquals("abc123", OAuthTokenResponseParser.extractAccessTokenKey(raw));
    }

    @Test
    public void extractPlainToken() {
        String raw = "plainTokenValue";
        assertFalse(OAuthTokenResponseParser.isJsonWrappedToken(raw));
        assertEquals("plainTokenValue", OAuthTokenResponseParser.extractAccessTokenKey(raw));
    }

    @Test
    public void parsePlainAccessToken() {
        String raw = "at_7d2ec174e0c71bf07b4aadf61572d16f";
        OAuthTokenResponse parsed = OAuthTokenResponseParser.parse(raw);
        assertEquals(raw, parsed.getAccessToken());
    }
}
