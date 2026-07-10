package cn.org.autumn.modules.client.support;

import cn.org.autumn.modules.client.entity.WebAuthenticationEntity;
import cn.org.autumn.modules.oauth.entity.ClientDetailsEntity;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OAuthClientDomainSupportTest {

    @Test
    void ensureUuid_generatesWhenBlank() {
        assertNotNull(OAuthClientDomainSupport.ensureUuid(null));
        assertEquals("abc", OAuthClientDomainSupport.ensureUuid(" abc "));
    }

    @Test
    void buildBaseUrl_normalizesScheme() {
        assertEquals("https://new.example.com", OAuthClientDomainSupport.buildBaseUrl("https", "new.example.com"));
        assertEquals("http://localhost", OAuthClientDomainSupport.buildBaseUrl("http", "localhost"));
    }

    @Test
    void applyRpDomainRebind_rebuildsStandardEndpointsAndRedirectUri() {
        WebAuthenticationEntity entity = new WebAuthenticationEntity();
        entity.setRedirectUri("https://old.example.com/client/oauth2/callback");
        entity.setAuthorizeUri("https://old.example.com/oauth2/authorize");
        entity.setAccessTokenUri("https://old.example.com/oauth2/token");
        entity.setUserInfoUri("https://old.example.com/oauth2/userInfo");

        OAuthClientDomainSupport.applyRpDomainRebind(entity, "https", "new.example.com");

        assertEquals("https://new.example.com/client/oauth2/callback", entity.getRedirectUri());
        assertEquals("https://new.example.com/oauth2/authorize", entity.getAuthorizeUri());
        assertEquals("https://new.example.com/oauth2/token", entity.getAccessTokenUri());
        assertEquals("https://new.example.com/oauth2/userInfo", entity.getUserInfoUri());
        assertEquals("new.example.com", entity.getClientId());
    }

    @Test
    void applyAsDomainRebind_rebuildsClientUriAndRedirectUri_notIcon() {
        ClientDetailsEntity entity = new ClientDetailsEntity();
        entity.setRedirectUri("https://old.example.com/client/oauth2/callback");
        entity.setClientUri("https://old.example.com");
        entity.setClientIconUri("https://cdn.example.com/icon.png");

        OAuthClientDomainSupport.applyAsDomainRebind(entity, "https", "new.example.com");

        assertEquals("https://new.example.com/client/oauth2/callback", entity.getRedirectUri());
        assertEquals("https://new.example.com", entity.getClientUri());
        assertEquals("https://cdn.example.com/icon.png", entity.getClientIconUri());
        assertEquals("new.example.com", entity.getClientName());
    }

    @Test
    void isSiteDefault() {
        assertTrue(OAuthClientDomainSupport.isSiteDefault(cn.org.autumn.config.ClientType.SiteDefault));
    }
}
