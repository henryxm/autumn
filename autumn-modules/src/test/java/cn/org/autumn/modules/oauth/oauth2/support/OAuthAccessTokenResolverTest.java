package cn.org.autumn.modules.oauth.oauth2.support;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;

public class OAuthAccessTokenResolverTest {

    @Test
    public void standardBearerHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer bearer-token-1");
        assertEquals("bearer-token-1", OAuthAccessTokenResolver.resolve(request, OAuthAccessTokenResolver.Policy.STANDARD));
    }

    @Test
    public void standardPlainQuery() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("access_token", "plain-token-2");
        assertEquals("plain-token-2", OAuthAccessTokenResolver.resolve(request, OAuthAccessTokenResolver.Policy.STANDARD));
    }

    @Test
    public void legacyJsonWrappedQuery() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("access_token", "{\"access_token\":\"legacy-inner\"}");
        assertEquals("legacy-inner", OAuthAccessTokenResolver.resolve(request, OAuthAccessTokenResolver.Policy.LEGACY_JSON_WRAP));
    }

    @Test
    public void legacyBearerFallback() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer legacy-bearer");
        assertEquals("legacy-bearer", OAuthAccessTokenResolver.resolve(request, OAuthAccessTokenResolver.Policy.LEGACY_JSON_WRAP));
    }

    @Test
    public void permissivePlainToken() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("access_token", "spm-plain");
        assertEquals("spm-plain", OAuthAccessTokenResolver.resolve(request, OAuthAccessTokenResolver.Policy.PERMISSIVE));
    }

    @Test
    public void buildAuthorizeUrlUsesClientIdForOauth() {
        OAuth2HttpClient client = new OAuth2HttpClient();
        String url = client.buildAuthorizeUrl("https://auth.example.com/oauth2/authorize", OAuth2HttpClient.CredentialParam.OAUTH, "my-client", "https://app/cb", "basic", "st");
        assertTrue(url.contains("client_id=my-client"));
        assertTrue(url.contains("redirect_uri="));
    }

    @Test
    public void buildAuthorizeUrlUsesAppIdForOpl() {
        OAuth2HttpClient client = new OAuth2HttpClient();
        String url = client.buildAuthorizeUrl("https://auth.example.com/open/oauth2/authorize", OAuth2HttpClient.CredentialParam.OPL, "ax-app", "https://app/cb", "basic", "st");
        assertTrue(url.contains("app_id=ax-app"));
        assertTrue(url.contains("redirect_uri="));
    }
}
