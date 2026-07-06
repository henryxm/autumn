package cn.org.autumn.modules.client.service;

import cn.org.autumn.modules.client.dao.WebOauthBindDao;
import cn.org.autumn.modules.client.dto.WebOauthBindResolveResult;
import cn.org.autumn.modules.client.entity.WebAuthenticationEntity;
import cn.org.autumn.modules.client.entity.WebOauthBindEntity;
import cn.org.autumn.modules.client.oauth2.WebOauthBindException;
import cn.org.autumn.modules.client.oauth2.WebOauthBindSupport;
import cn.org.autumn.modules.client.site.ClientConstants;
import cn.org.autumn.modules.sys.entity.SysUserEntity;
import cn.org.autumn.modules.sys.service.SysUserService;
import cn.org.autumn.modules.usr.dto.UserProfile;
import cn.org.autumn.modules.usr.entity.UserProfileEntity;
import cn.org.autumn.modules.usr.service.UserProfileService;
import javax.servlet.http.HttpServletRequest;
import cn.org.autumn.utils.Uuid;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

public class WebOauthBindServiceTest {

    private static final String WEB_AUTH_UUID = "wa_test_001";
    private static final String UPSTREAM_A = "upstream-user-a";
    private static final String LOCAL_A = "local-user-a";
    private static final String LOCAL_B = "local-user-b";
    private static final String LOCAL_NEW = "local-user-new";

    private WebOauthBindService webOauthBindService;

    @Mock
    private WebOauthBindDao webOauthBindDao;

    @Mock
    private SysUserService sysUserService;

    @Mock
    private UserProfileService userProfileService;

    @Mock
    private WebOauthBindSupport webOauthBindSupport;

    @Mock
    private HttpServletRequest request;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        webOauthBindService = new WebOauthBindService();
        ReflectionTestUtils.setField(webOauthBindService, "baseMapper", webOauthBindDao);
        ReflectionTestUtils.setField(webOauthBindService, "sysUserService", sysUserService);
        ReflectionTestUtils.setField(webOauthBindService, "userProfileService", userProfileService);
        ReflectionTestUtils.setField(webOauthBindService, "webOauthBindSupport", webOauthBindSupport);
        webOauthBindService = Mockito.spy(webOauthBindService);
    }

    @Test
    public void noBindNotLoggedInCreatesLocalUserAndBind() {
        WebAuthenticationEntity webAuth = webAuth();
        UserProfile upstream = upstream(UPSTREAM_A);
        Mockito.doReturn(null).when(webOauthBindService).sessionUserUuid();
        Mockito.when(webOauthBindSupport.isSameInstance(webAuth, request)).thenReturn(false);
        Mockito.when(webOauthBindDao.getByAuthenticationAndUpper(WEB_AUTH_UUID, UPSTREAM_A)).thenReturn(null);
        Mockito.when(sysUserService.getByUuid(UPSTREAM_A)).thenReturn(null);
        SysUserEntity created = user(LOCAL_NEW);
        Mockito.when(sysUserService.provisionConnectUser(Mockito.anyString(), Mockito.anyString())).thenReturn(created);
        mockProfile(LOCAL_NEW);

        WebOauthBindResolveResult result = webOauthBindService.resolveAndBind(webAuth, upstream, request);

        Assert.assertFalse(result.isIdempotent());
        Assert.assertEquals(LOCAL_NEW, result.getProfile().getUuid());
        ArgumentCaptor<WebOauthBindEntity> captor = ArgumentCaptor.forClass(WebOauthBindEntity.class);
        Mockito.verify(webOauthBindDao).insert(captor.capture());
        Assert.assertEquals(UPSTREAM_A, captor.getValue().getUpper());
        Assert.assertEquals(LOCAL_NEW, captor.getValue().getUser());
    }

    @Test
    public void noBindLoggedInBindsSessionUser() {
        WebAuthenticationEntity webAuth = webAuth();
        UserProfile upstream = upstream(UPSTREAM_A);
        Mockito.doReturn(LOCAL_B).when(webOauthBindService).sessionUserUuid();
        Mockito.when(webOauthBindSupport.isSameInstance(webAuth, request)).thenReturn(false);
        Mockito.when(webOauthBindDao.getByAuthenticationAndUpper(WEB_AUTH_UUID, UPSTREAM_A)).thenReturn(null);
        Mockito.when(webOauthBindDao.getByAuthenticationAndUser(WEB_AUTH_UUID, LOCAL_B)).thenReturn(null);
        mockProfile(LOCAL_B);

        WebOauthBindResolveResult result = webOauthBindService.resolveAndBind(webAuth, upstream, request);

        Assert.assertFalse(result.isIdempotent());
        Assert.assertEquals(LOCAL_B, result.getProfile().getUuid());
        Mockito.verify(webOauthBindDao).insert(Mockito.argThat(bind -> UPSTREAM_A.equals(bind.getUpper()) && LOCAL_B.equals(bind.getUser())));
    }

    @Test
    public void sameInstanceWithoutSessionUsesUpstreamUuidAsLocalUser() {
        WebAuthenticationEntity webAuth = webAuth();
        UserProfile upstream = upstream(LOCAL_A);
        Mockito.doReturn(null).when(webOauthBindService).sessionUserUuid();
        Mockito.when(webOauthBindSupport.isSameInstance(webAuth, request)).thenReturn(true);
        Mockito.when(sysUserService.getByUuid(LOCAL_A)).thenReturn(user(LOCAL_A));
        Mockito.when(webOauthBindDao.getByAuthenticationAndUpper(WEB_AUTH_UUID, LOCAL_A)).thenReturn(null);
        mockProfile(LOCAL_A);

        WebOauthBindResolveResult result = webOauthBindService.resolveAndBind(webAuth, upstream, request);

        Assert.assertTrue(result.isIdempotent());
        Assert.assertEquals(LOCAL_A, result.getProfile().getUuid());
        Mockito.verify(webOauthBindDao).insert(Mockito.argThat(bind -> LOCAL_A.equals(bind.getUpper()) && LOCAL_A.equals(bind.getUser())));
        Mockito.verify(sysUserService, Mockito.never()).provisionConnectUser(Mockito.anyString(), Mockito.anyString());
    }

    @Test
    public void sameInstanceIdempotentInsertsBindWhenMissing() {
        WebAuthenticationEntity webAuth = webAuth();
        UserProfile upstream = upstream(LOCAL_A);
        Mockito.doReturn(null).when(webOauthBindService).sessionUserUuid();
        Mockito.when(webOauthBindSupport.isSameInstance(webAuth, request)).thenReturn(true);
        Mockito.when(sysUserService.getByUuid(LOCAL_A)).thenReturn(user(LOCAL_A));
        Mockito.when(webOauthBindDao.getByAuthenticationAndUpper(WEB_AUTH_UUID, LOCAL_A)).thenReturn(null);
        mockProfile(LOCAL_A);

        WebOauthBindResolveResult result = webOauthBindService.resolveAndBind(webAuth, upstream, request);

        Assert.assertTrue(result.isIdempotent());
        Assert.assertEquals(LOCAL_A, result.getProfile().getUuid());
        Mockito.verify(webOauthBindDao).insert(Mockito.any(WebOauthBindEntity.class));
    }

    @Test
    public void sameInstanceIdempotentNoOpWhenBindExists() {
        WebAuthenticationEntity webAuth = webAuth();
        UserProfile upstream = upstream(LOCAL_A);
        WebOauthBindEntity existing = bind(UPSTREAM_A, LOCAL_A);
        Mockito.doReturn(null).when(webOauthBindService).sessionUserUuid();
        Mockito.when(webOauthBindSupport.isSameInstance(webAuth, request)).thenReturn(true);
        Mockito.when(sysUserService.getByUuid(LOCAL_A)).thenReturn(user(LOCAL_A));
        Mockito.when(webOauthBindSupport.isSameUser(LOCAL_A, LOCAL_A)).thenReturn(true);
        Mockito.when(webOauthBindDao.getByAuthenticationAndUpper(WEB_AUTH_UUID, LOCAL_A)).thenReturn(existing);
        mockProfile(LOCAL_A);

        WebOauthBindResolveResult result = webOauthBindService.resolveAndBind(webAuth, upstream, request);

        Assert.assertTrue(result.isIdempotent());
        Mockito.verify(webOauthBindDao, Mockito.never()).insert(Mockito.any(WebOauthBindEntity.class));
    }

    @Test
    public void existingUpstreamBindLogsInWhenNotLoggedIn() {
        WebAuthenticationEntity webAuth = webAuth();
        UserProfile upstream = upstream(UPSTREAM_A);
        WebOauthBindEntity existing = bind(UPSTREAM_A, LOCAL_A);
        Mockito.doReturn(null).when(webOauthBindService).sessionUserUuid();
        Mockito.when(webOauthBindSupport.isSameInstance(webAuth, request)).thenReturn(false);
        Mockito.when(webOauthBindDao.getByAuthenticationAndUpper(WEB_AUTH_UUID, UPSTREAM_A)).thenReturn(existing);
        mockProfile(LOCAL_A);

        WebOauthBindResolveResult result = webOauthBindService.resolveAndBind(webAuth, upstream, request);

        Assert.assertFalse(result.isIdempotent());
        Assert.assertEquals(LOCAL_A, result.getProfile().getUuid());
    }

    @Test
    public void existingUpstreamBindConflictWhenSessionIsOtherUser() {
        WebAuthenticationEntity webAuth = webAuth();
        UserProfile upstream = upstream(UPSTREAM_A);
        WebOauthBindEntity existing = bind(UPSTREAM_A, LOCAL_A);
        Mockito.doReturn(LOCAL_B).when(webOauthBindService).sessionUserUuid();
        Mockito.when(webOauthBindSupport.isSameInstance(webAuth, request)).thenReturn(false);
        Mockito.when(webOauthBindSupport.isSameUser(LOCAL_A, LOCAL_B)).thenReturn(false);
        Mockito.when(webOauthBindDao.getByAuthenticationAndUpper(WEB_AUTH_UUID, UPSTREAM_A)).thenReturn(existing);

        try {
            webOauthBindService.resolveAndBind(webAuth, upstream, request);
            Assert.fail("expected WebOauthBindException");
        } catch (WebOauthBindException e) {
            Assert.assertEquals(WebOauthBindException.ConflictType.UPSTREAM_BOUND_TO_OTHER, e.getConflictType());
        }
    }

    @Test
    public void loggedInUserAlreadyBoundOtherUpstreamThrowsConflict() {
        WebAuthenticationEntity webAuth = webAuth();
        UserProfile upstream = upstream(UPSTREAM_A);
        WebOauthBindEntity localBind = bind("other-upstream", LOCAL_B);
        Mockito.doReturn(LOCAL_B).when(webOauthBindService).sessionUserUuid();
        Mockito.when(webOauthBindSupport.isSameInstance(webAuth, request)).thenReturn(false);
        Mockito.when(webOauthBindDao.getByAuthenticationAndUpper(WEB_AUTH_UUID, UPSTREAM_A)).thenReturn(null);
        Mockito.when(webOauthBindDao.getByAuthenticationAndUser(WEB_AUTH_UUID, LOCAL_B)).thenReturn(localBind);
        Mockito.when(webOauthBindSupport.isSameUser("other-upstream", UPSTREAM_A)).thenReturn(false);

        try {
            webOauthBindService.resolveAndBind(webAuth, upstream, request);
            Assert.fail("expected WebOauthBindException");
        } catch (WebOauthBindException e) {
            Assert.assertEquals(WebOauthBindException.ConflictType.LOCAL_ALREADY_BOUND, e.getConflictType());
        }
    }

    @Test
    public void legacyUserLazyMigration() {
        WebAuthenticationEntity webAuth = webAuth();
        UserProfile upstream = upstream(UPSTREAM_A);
        SysUserEntity legacy = user(UPSTREAM_A);
        Mockito.doReturn(null).when(webOauthBindService).sessionUserUuid();
        Mockito.when(webOauthBindSupport.isSameInstance(webAuth, request)).thenReturn(false);
        Mockito.when(webOauthBindDao.getByAuthenticationAndUpper(WEB_AUTH_UUID, UPSTREAM_A)).thenReturn(null);
        Mockito.when(sysUserService.getByUuid(UPSTREAM_A)).thenReturn(legacy);
        mockProfile(UPSTREAM_A);

        WebOauthBindResolveResult result = webOauthBindService.resolveAndBind(webAuth, upstream, request);

        Assert.assertFalse(result.isIdempotent());
        Assert.assertEquals(UPSTREAM_A, result.getProfile().getUuid());
        Mockito.verify(webOauthBindDao).insert(Mockito.argThat(bind -> UPSTREAM_A.equals(bind.getUpper()) && UPSTREAM_A.equals(bind.getUser())));
        Mockito.verify(sysUserService, Mockito.never()).provisionConnectUser(Mockito.anyString(), Mockito.anyString());
    }

    @Test
    public void usernameConflictRetriesWithSuffix() {
        WebAuthenticationEntity webAuth = webAuth();
        UserProfile upstream = upstream(UPSTREAM_A);
        Mockito.doReturn(null).when(webOauthBindService).sessionUserUuid();
        Mockito.when(webOauthBindSupport.isSameInstance(webAuth, request)).thenReturn(false);
        Mockito.when(webOauthBindDao.getByAuthenticationAndUpper(WEB_AUTH_UUID, UPSTREAM_A)).thenReturn(null);
        Mockito.when(sysUserService.getByUuid(UPSTREAM_A)).thenReturn(null);
        SysUserEntity created = user(LOCAL_NEW);
        String usernameBase = ClientConstants.OAUTH_AUTO_REGISTER_USERNAME_PREFIX + Uuid.prefix(UPSTREAM_A, 12);
        Mockito.when(sysUserService.provisionConnectUser(Mockito.eq(usernameBase), Mockito.anyString())).thenThrow(new IllegalArgumentException("该账号已被注册"));
        Mockito.when(sysUserService.provisionConnectUser(Mockito.eq(usernameBase + "_1"), Mockito.anyString())).thenReturn(created);
        mockProfile(LOCAL_NEW);

        WebOauthBindResolveResult result = webOauthBindService.resolveAndBind(webAuth, upstream, request);

        Assert.assertEquals(LOCAL_NEW, result.getProfile().getUuid());
        Mockito.verify(sysUserService).provisionConnectUser(Mockito.eq(usernameBase + "_1"), Mockito.anyString());
    }

    private WebAuthenticationEntity webAuth() {
        WebAuthenticationEntity webAuth = new WebAuthenticationEntity();
        webAuth.setUuid(WEB_AUTH_UUID);
        webAuth.setClientId("test-client");
        webAuth.setAccessTokenUri("https://example.com/oauth2/token");
        webAuth.setUserInfoUri("https://example.com/oauth2/userInfo");
        return webAuth;
    }

    private UserProfile upstream(String uuid) {
        UserProfile profile = new UserProfile();
        profile.setUuid(uuid);
        profile.setNickname("nick");
        return profile;
    }

    private WebOauthBindEntity bind(String upstreamUuid, String localUser) {
        WebOauthBindEntity bind = new WebOauthBindEntity();
        bind.setAuthentication(WEB_AUTH_UUID);
        bind.setUpper(upstreamUuid);
        bind.setUser(localUser);
        return bind;
    }

    private SysUserEntity user(String uuid) {
        SysUserEntity user = new SysUserEntity();
        user.setUuid(uuid);
        return user;
    }

    private void mockProfile(String uuid) {
        UserProfileEntity profile = new UserProfileEntity();
        profile.setUuid(uuid);
        profile.setNickname("nick");
        Mockito.when(userProfileService.getByUuid(uuid)).thenReturn(profile);
    }
}
