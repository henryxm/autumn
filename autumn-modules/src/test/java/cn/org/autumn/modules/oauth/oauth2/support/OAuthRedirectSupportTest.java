package cn.org.autumn.modules.oauth.oauth2.support;

import static org.junit.Assert.assertTrue;

import org.apache.oltu.oauth2.common.OAuth;
import org.junit.Test;

public class OAuthRedirectSupportTest {

    @Test
    public void appendCodeAndStateEncodesCode() {
        String url = OAuthRedirectSupport.appendCodeAndState("https://example.com/cb", "a+b/c", "st ate");
        assertTrue(url.contains(OAuth.OAUTH_CODE + "="));
        assertTrue(url.contains("%2B") || url.contains("a%2Bb%2Fc"));
    }
}
