package cn.org.autumn.modules.client.service;

import cn.org.autumn.auth.scope.AuthScopeCatalog;
import cn.org.autumn.modules.auth.service.AuthRealNameService;
import cn.org.autumn.modules.auth.service.OAuthExtensionService;
import cn.org.autumn.modules.auth.support.AuthScopeSupport;
import cn.org.autumn.modules.client.dto.WebOauthBindResolveResult;
import cn.org.autumn.modules.client.entity.WebAuthenticationEntity;
import cn.org.autumn.modules.client.oauth2.WebOauthBindException;
import cn.org.autumn.modules.client.oauth2.WebOauthBindSupport;
import cn.org.autumn.modules.client.oauth2.WebOauthEndpointResolver;
import cn.org.autumn.modules.oauth.oauth2.support.OAuth2HttpClient;
import cn.org.autumn.modules.oauth.service.ClientDetailsService;
import cn.org.autumn.modules.oauth.store.TokenStore;
import cn.org.autumn.modules.oauth.store.ValueType;
import cn.org.autumn.modules.sys.entity.SysUserEntity;
import cn.org.autumn.modules.usr.dto.UserProfile;
import cn.org.autumn.modules.usr.entity.UserProfileEntity;
import cn.org.autumn.modules.usr.service.UserProfileService;
import cn.org.autumn.modules.usr.service.UserTokenService;
import cn.org.autumn.oauth.model.OAuthClientSnapshot;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.context.ApplicationEventPublisher;

public class WebOauthLoginServiceTest {

    @InjectMocks
    private WebOauthLoginService webOauthLoginService;

    @Mock
    private OAuth2HttpClient oauth2HttpClient;

    @Mock
    private WebOauthBindService webOauthBindService;

    @Mock
    private WebOauthBindSupport webOauthBindSupport;

    @Mock
    private UserProfileService userProfileService;

    @Mock
    private UserTokenService userTokenService;

    @Mock
    private ClientDetailsService clientDetailsService;

    @Mock
    private WebOauthEndpointResolver webOauthEndpointResolver;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @Spy
    private AuthScopeCatalog authScopeCatalog = new AuthScopeCatalog();

    @Mock
    private AuthScopeSupport authScopeSupport;

    @Mock
    private OAuthExtensionService oauthExtensionService;

    @Mock
    private AuthRealNameService authRealNameService;

    @Mock
    private HttpServletRequest request;

    private WebAuthenticationEntity webAuth;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        webAuth = new WebAuthenticationEntity();
        webAuth.setUuid("wa1");
        webAuth.setClientId("test");
        webAuth.setClientSecret("secret");
        webAuth.setAccessTokenUri("https://remote.example.com/oauth2/token");
        webAuth.setUserInfoUri("https://remote.example.com/oauth2/userInfo");
        webAuth.setRedirectUri("https://local.example.com/client/oauth2/callback");
        Mockito.when(webOauthEndpointResolver.resolveAccessTokenUri(webAuth)).thenReturn(webAuth.getAccessTokenUri());
        Mockito.when(webOauthEndpointResolver.resolveUserInfoUri(Mockito.eq(webAuth), Mockito.anyBoolean())).thenReturn(webAuth.getUserInfoUri());
        Mockito.when(clientDetailsService.isValidCode(Mockito.anyString())).thenReturn(false);
        Mockito.when(authScopeSupport.toSnapshot(Mockito.any())).thenReturn(new OAuthClientSnapshot());
    }

    @Test
    public void crossInstanceUsesBearerTokenFromJsonBody() {
        String tokenBody = "{\"access_token\":\"at_123\",\"token_type\":\"bearer\"}";
        Mockito.when(oauth2HttpClient.exchangeAuthorizationCodeRaw(Mockito.any(), Mockito.eq(webAuth.getAccessTokenUri()), Mockito.anyString(), Mockito.anyString(), Mockito.eq("code1"), Mockito.anyString())).thenReturn(tokenBody);
        Mockito.when(webOauthBindSupport.resolveUserInfoDelivery(webAuth, request)).thenReturn(OAuth2HttpClient.UserInfoDelivery.BEARER);
        Mockito.when(oauth2HttpClient.fetchUserInfoBody(webAuth.getUserInfoUri(), "at_123", OAuth2HttpClient.UserInfoDelivery.BEARER)).thenReturn("{\"uuid\":\"u1\",\"nickname\":\"n1\"}");
        UserProfile profile = new UserProfile();
        profile.setUuid("local-u1");
        WebOauthBindResolveResult bindResult = WebOauthBindResolveResult.of(profile, false);
        Mockito.when(webOauthBindService.resolveAndBind(Mockito.eq(webAuth), Mockito.any(UserProfile.class), Mockito.eq(request))).thenReturn(bindResult);

        webOauthLoginService.completeOAuthCallback(request, webAuth, "code1");

        ArgumentCaptor<String> tokenCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(oauth2HttpClient).fetchUserInfoBody(Mockito.eq(webAuth.getUserInfoUri()), tokenCaptor.capture(), Mockito.eq(OAuth2HttpClient.UserInfoDelivery.BEARER));
        Assert.assertEquals("at_123", tokenCaptor.getValue());
        Mockito.verify(userProfileService).establishSession(profile);
        Mockito.verify(userTokenService).saveToken(tokenBody);
    }

    @Test
    public void invalidUserInfoThrowsBindException() {
        Mockito.when(oauth2HttpClient.exchangeAuthorizationCodeRaw(Mockito.any(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn("{\"access_token\":\"at\"}");
        Mockito.when(webOauthBindSupport.resolveUserInfoDelivery(webAuth, request)).thenReturn(OAuth2HttpClient.UserInfoDelivery.BEARER);
        Mockito.when(oauth2HttpClient.fetchUserInfoBody(Mockito.anyString(), Mockito.anyString(), Mockito.any())).thenReturn("{\"nickname\":\"no-uuid\"}");

        try {
            webOauthLoginService.completeOAuthCallback(request, webAuth, "code1");
            Assert.fail("expected WebOauthBindException");
        } catch (WebOauthBindException e) {
            Assert.assertEquals(WebOauthBindException.ConflictType.UPSTREAM_UUID_INVALID, e.getConflictType());
        }
    }

    @Test
    public void idempotentStillCallsEstablishSession() {
        String tokenBody = "{\"access_token\":\"at_legacy\"}";
        Mockito.when(oauth2HttpClient.exchangeAuthorizationCodeRaw(Mockito.any(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(tokenBody);
        Mockito.when(webOauthBindSupport.resolveUserInfoDelivery(webAuth, request)).thenReturn(OAuth2HttpClient.UserInfoDelivery.LEGACY);
        Mockito.when(oauth2HttpClient.fetchUserInfoBody(Mockito.anyString(), Mockito.eq(tokenBody), Mockito.eq(OAuth2HttpClient.UserInfoDelivery.LEGACY))).thenReturn("{\"uuid\":\"u1\"}");
        UserProfile profile = new UserProfile();
        profile.setUuid("u1");
        Mockito.when(webOauthBindService.resolveAndBind(Mockito.eq(webAuth), Mockito.any(UserProfile.class), Mockito.eq(request))).thenReturn(WebOauthBindResolveResult.of(profile, true));

        webOauthLoginService.completeOAuthCallback(request, webAuth, "code1");

        Mockito.verify(userProfileService).establishSession(profile);
        Mockito.verify(userTokenService).saveToken(tokenBody);
    }

    @Test
    public void localUserInfoRespectsBasicScope_omitsPhoneAndVerified() {
        SysUserEntity user = new SysUserEntity();
        user.setUuid("u1");
        user.setVerify(1);
        user.setMobile("13800138000");
        UserProfileEntity profileEntity = new UserProfileEntity();
        profileEntity.setUuid("u1");
        profileEntity.setNickname("nick");
        profileEntity.setUsername("user1");
        TokenStore store = new TokenStore(user, null, "at", null, "basic", 3600L);
        Mockito.when(clientDetailsService.get(ValueType.accessToken, "at")).thenReturn(store);
        Mockito.when(userProfileService.from(user)).thenReturn(profileEntity);

        UserProfile profile = webOauthLoginService.fetchUserProfileLocally("at");

        Assert.assertEquals("u1", profile.getUuid());
        Assert.assertEquals("nick", profile.getNickname());
        Assert.assertNull(profile.getMobile());
        Assert.assertNull(profile.getVerified());
    }

    @Test
    public void localUserInfoIncludesPhoneAndVerifiedWhenGranted() {
        SysUserEntity user = new SysUserEntity();
        user.setUuid("u1");
        user.setVerify(1);
        user.setMobile("13800138000");
        UserProfileEntity profileEntity = new UserProfileEntity();
        profileEntity.setUuid("u1");
        TokenStore store = new TokenStore(user, null, "at", null, "identity,phone,verified", 3600L);
        Mockito.when(clientDetailsService.get(ValueType.accessToken, "at")).thenReturn(store);
        Mockito.when(userProfileService.from(user)).thenReturn(profileEntity);

        UserProfile profile = webOauthLoginService.fetchUserProfileLocally("at");

        Assert.assertEquals("13800138000", profile.getMobile());
        Assert.assertEquals(Integer.valueOf(1), profile.getVerified());
    }
}
