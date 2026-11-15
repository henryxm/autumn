package cn.org.autumn.modules.sys.support;

import static org.junit.Assert.assertEquals;

import cn.org.autumn.web.AuthSource;
import org.junit.Test;

public class AuthContextBinderTest {

    @Test
    public void enrichSessionWhenOauthMatchesUuid() {
        OAuthTokenInfo oauth = new OAuthTokenInfo("u1", "Ak1", null);
        AuthContextBinder.EnrichResult r = AuthContextBinder.enrichSessionWithOAuth(AuthSource.SESSION, "u1", "name", oauth, "");
        assertEquals(AuthSource.OAUTH_TOKEN, r.getSource());
        assertEquals("Ak1", r.getAccessKey());
    }

    @Test
    public void enrichSessionWhenShadowUsernameMatchesClientId() {
        OAuthTokenInfo oauth = new OAuthTokenInfo("shadow-uuid", "AkShadow", null);
        AuthContextBinder.EnrichResult r = AuthContextBinder.enrichSessionWithOAuth(AuthSource.SESSION, "other", "AkShadow", oauth, "");
        assertEquals(AuthSource.OAUTH_TOKEN, r.getSource());
        assertEquals("AkShadow", r.getAccessKey());
    }

    @Test
    public void ignoreOauthWhenUserMismatch() {
        OAuthTokenInfo oauth = new OAuthTokenInfo("u2", "Ak2", null);
        AuthContextBinder.EnrichResult r = AuthContextBinder.enrichSessionWithOAuth(AuthSource.SESSION, "u1", "name", oauth, "");
        assertEquals(AuthSource.SESSION, r.getSource());
        assertEquals("", r.getAccessKey());
    }

    @Test
    public void keepExistingAccessKey() {
        OAuthTokenInfo oauth = new OAuthTokenInfo("u1", "AkNew", null);
        AuthContextBinder.EnrichResult r = AuthContextBinder.enrichSessionWithOAuth(AuthSource.SESSION, "u1", "name", oauth, "AkOld");
        assertEquals(AuthSource.OAUTH_TOKEN, r.getSource());
        assertEquals("AkOld", r.getAccessKey());
    }
}
