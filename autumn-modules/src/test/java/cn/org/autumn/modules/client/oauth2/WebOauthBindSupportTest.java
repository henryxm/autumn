package cn.org.autumn.modules.client.oauth2;

import cn.org.autumn.modules.client.entity.WebAuthenticationEntity;
import cn.org.autumn.modules.client.service.WebOauthCombineService;
import cn.org.autumn.modules.oauth.oauth2.support.OAuth2HttpClient;
import javax.servlet.http.HttpServletRequest;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

public class WebOauthBindSupportTest {

    @InjectMocks
    private WebOauthBindSupport webOauthBindSupport;

    @Mock
    private WebOauthCombineService webOauthCombineService;

    @Mock
    private HttpServletRequest request;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void explicitLegacyConfigWins() {
        WebAuthenticationEntity webAuth = webAuth("legacy");
        Assert.assertEquals(OAuth2HttpClient.UserInfoDelivery.LEGACY, webOauthBindSupport.resolveUserInfoDelivery(webAuth, request));
    }

    @Test
    public void explicitBearerConfigWins() {
        WebAuthenticationEntity webAuth = webAuth("bearer");
        Assert.assertEquals(OAuth2HttpClient.UserInfoDelivery.BEARER, webOauthBindSupport.resolveUserInfoDelivery(webAuth, request));
    }

    @Test
    public void sameInstanceDefaultsLegacy() {
        WebAuthenticationEntity webAuth = webAuth(null);
        Mockito.when(webOauthCombineService.getByClientId("same-host")).thenReturn(new cn.org.autumn.modules.client.entity.WebOauthCombineEntity());
        Assert.assertEquals(OAuth2HttpClient.UserInfoDelivery.LEGACY, webOauthBindSupport.resolveUserInfoDelivery(webAuth, request));
    }

    @Test
    public void crossInstanceDefaultsBearer() {
        WebAuthenticationEntity webAuth = webAuth(null);
        webAuth.setClientId("remote");
        webAuth.setAccessTokenUri("https://remote.example.com/oauth2/token");
        webAuth.setUserInfoUri("https://remote.example.com/oauth2/userInfo");
        Mockito.when(request.getHeader("host")).thenReturn("local.example.com");
        Mockito.when(webOauthCombineService.getByClientId("remote")).thenReturn(null);
        Assert.assertEquals(OAuth2HttpClient.UserInfoDelivery.BEARER, webOauthBindSupport.resolveUserInfoDelivery(webAuth, request));
    }

    private WebAuthenticationEntity webAuth(String delivery) {
        WebAuthenticationEntity webAuth = new WebAuthenticationEntity();
        webAuth.setClientId("same-host");
        webAuth.setAccessTokenUri("https://same-host/oauth2/token");
        webAuth.setUserInfoUri("https://same-host/oauth2/userInfo");
        webAuth.setUserInfoDelivery(delivery);
        return webAuth;
    }
}
