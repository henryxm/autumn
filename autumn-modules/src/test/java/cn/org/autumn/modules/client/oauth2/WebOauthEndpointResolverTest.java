package cn.org.autumn.modules.client.oauth2;

import cn.org.autumn.modules.client.entity.WebAuthenticationEntity;
import org.junit.Assert;
import org.junit.Test;

public class WebOauthEndpointResolverTest {

    private final WebOauthEndpointResolver resolver = new WebOauthEndpointResolver();

    @Test
    public void explicitUriTakesPrecedenceOverOriginUri() {
        WebAuthenticationEntity web = new WebAuthenticationEntity();
        web.setOriginUri("https://as.example.com");
        web.setUserInfoUri("https://custom.example.com/oauth2/userInfo");
        Assert.assertEquals("https://custom.example.com/oauth2/userInfo", resolver.resolveUserInfoUri(web, true));
        Assert.assertEquals("https://as.example.com/oauth2/token", resolver.resolveAccessTokenUri(web));
    }

    @Test
    public void inferFromOriginUriWhenEndpointBlank() {
        WebAuthenticationEntity web = new WebAuthenticationEntity();
        web.setOriginUri("https://chaoran.xin/");
        Assert.assertEquals("https://chaoran.xin/oauth2/authorize", resolver.resolveAuthorizeUri(web));
        Assert.assertEquals("https://chaoran.xin/qrc/api/v1/ticket/open/create", resolver.resolveQrcOpenCreateUri(web));
    }

    @Test
    public void hasRemoteOriginWhenOriginUriPresent() {
        WebAuthenticationEntity web = new WebAuthenticationEntity();
        web.setOriginUri("https://chaoran.xin/");
        Assert.assertTrue(resolver.hasRemoteOrigin(web));
        web.setOriginUri("");
        Assert.assertFalse(resolver.hasRemoteOrigin(web));
    }

    @Test
    public void blankOriginUriReturnsNullForAccessToken() {
        WebAuthenticationEntity web = new WebAuthenticationEntity();
        Assert.assertNull(resolver.resolveAccessTokenUri(web));
    }
}
