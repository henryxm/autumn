package cn.org.autumn.modules.sys.service;

import cn.org.autumn.model.AuthLoginProviderList;
import cn.org.autumn.model.AuthLoginProviderType;
import cn.org.autumn.modules.client.entity.WebAuthenticationEntity;
import cn.org.autumn.modules.client.oauth2.WebOauthEndpointResolver;
import cn.org.autumn.modules.client.service.AuthSiteRoleService;
import cn.org.autumn.modules.client.service.WebAuthenticationService;
import cn.org.autumn.modules.opc.entity.ConnectAppEntity;
import cn.org.autumn.modules.opc.service.ConnectAppService;
import cn.org.autumn.modules.opc.support.ConnectBindSupport;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import java.util.Collections;
import java.util.Date;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthLoginProviderServiceTest {

    @Mock
    private WebAuthenticationService webAuthenticationService;

    @Mock
    private ConnectAppService connectAppService;

    @Mock
    private AuthSiteRoleService authSiteRoleService;

    @Mock
    private SysConfigService sysConfigService;

    @Mock
    private WebOauthEndpointResolver webOauthEndpointResolver;

    @Mock
    private ConnectBindSupport connectBindSupport;

    @InjectMocks
    private AuthLoginProviderService authLoginProviderService;

    @Test
    void listPageProviders_emptyWhenNothingEligible() {
        when(authSiteRoleService.isRpEnabled()).thenReturn(true);
        when(webAuthenticationService.selectList(any(QueryWrapper.class))).thenReturn(Collections.emptyList());
        when(connectAppService.listPageLoginActive()).thenReturn(Collections.emptyList());

        AuthLoginProviderList list = authLoginProviderService.listPageProviders();

        assertFalse(list.isVisible());
        assertTrue(list.getProviders().isEmpty());
    }

    @Test
    void listPageProviders_classicAndOpenTypes() {
        when(authSiteRoleService.isRpEnabled()).thenReturn(true);
        when(sysConfigService.getBaseUrl()).thenReturn("https://local.example.com");

        WebAuthenticationEntity classic = new WebAuthenticationEntity();
        classic.setClientId("rp-demo");
        classic.setName("Demo RP");
        classic.setClientSecret("secret");
        classic.setRedirectUri("https://local.example.com/client/oauth2/callback");
        classic.setPageLogin(1);
        when(webAuthenticationService.selectList(any(QueryWrapper.class))).thenReturn(Collections.singletonList(classic));
        when(webOauthEndpointResolver.resolveAuthorizeUri(classic)).thenReturn("https://remote.example.com/oauth2/authorize");

        ConnectAppEntity open = new ConnectAppEntity();
        open.setAppId("app_demo");
        open.setName("Open Demo");
        open.setRedirectUri("https://local.example.com/open/oauth2/opc/callback?appId=app_demo");
        open.setPlatformBaseUrl("https://opl.example.com");
        open.setStatus(ConnectAppEntity.STATUS_ACTIVE);
        open.setPageLogin(1);
        when(connectAppService.listPageLoginActive()).thenReturn(Collections.singletonList(open));
        when(connectAppService.hasConfiguredSecret(open)).thenReturn(true);
        when(connectBindSupport.isSamePlatform(open)).thenReturn(false);
        when(connectAppService.tryFillDefaultUris(eq(open), isNull())).thenReturn(true);

        AuthLoginProviderList list = authLoginProviderService.listPageProviders();

        assertTrue(list.isVisible());
        assertEquals(2, list.getProviders().size());
        assertEquals(AuthLoginProviderType.OAUTH2_CLASSIC, list.getProviders().get(0).getType());
        assertEquals("rp-demo", list.getProviders().get(0).getClientId());
        assertEquals(AuthLoginProviderType.OAUTH2_OPEN, list.getProviders().get(1).getType());
        assertEquals("app_demo", list.getProviders().get(1).getAppId());
        assertTrue(list.getProviders().get(1).getLoginUrl().startsWith("/open/oauth2/login?appId="));
    }

    @Test
    void listPageProviders_openUsesSiteBaseWhenSamePlatform() {
        when(authSiteRoleService.isRpEnabled()).thenReturn(false);
        when(sysConfigService.getBaseUrl()).thenReturn("http://192.168.3.93");

        ConnectAppEntity open = new ConnectAppEntity();
        open.setAppId("local_app");
        open.setName("Local Open");
        open.setRedirectUri("http://192.168.3.93/open/oauth2/callback?appId=local_app");
        open.setStatus(ConnectAppEntity.STATUS_ACTIVE);
        open.setPageLogin(1);
        when(connectAppService.listPageLoginActive()).thenReturn(Collections.singletonList(open));
        when(connectAppService.hasConfiguredSecret(open)).thenReturn(true);
        when(connectBindSupport.isSamePlatform(open)).thenReturn(true);
        when(connectAppService.tryFillDefaultUris(open, "http://192.168.3.93")).thenReturn(true);

        AuthLoginProviderList list = authLoginProviderService.listPageProviders();

        assertTrue(list.isVisible());
        assertEquals(1, list.getProviders().size());
        assertEquals(AuthLoginProviderType.OAUTH2_OPEN, list.getProviders().get(0).getType());
        assertEquals("local_app", list.getProviders().get(0).getAppId());
    }

    @Test
    void listPageProviders_sortedByCreateTimeDescending() {
        when(authSiteRoleService.isRpEnabled()).thenReturn(true);
        when(sysConfigService.getBaseUrl()).thenReturn("https://local.example.com");

        WebAuthenticationEntity olderClassic = new WebAuthenticationEntity();
        olderClassic.setClientId("older-rp");
        olderClassic.setClientSecret("secret");
        olderClassic.setRedirectUri("https://local.example.com/client/oauth2/callback");
        olderClassic.setPageLogin(1);
        olderClassic.setCreateTime(new Date(1_000L));
        when(webAuthenticationService.selectList(any(QueryWrapper.class))).thenReturn(Collections.singletonList(olderClassic));
        when(webOauthEndpointResolver.resolveAuthorizeUri(olderClassic)).thenReturn("https://remote.example.com/oauth2/authorize");

        ConnectAppEntity newerOpen = new ConnectAppEntity();
        newerOpen.setAppId("newer_app");
        newerOpen.setRedirectUri("https://local.example.com/open/oauth2/callback?appId=newer_app");
        newerOpen.setPlatformBaseUrl("https://opl.example.com");
        newerOpen.setStatus(ConnectAppEntity.STATUS_ACTIVE);
        newerOpen.setPageLogin(1);
        newerOpen.setCreate(new Date(2_000L));
        when(connectAppService.listPageLoginActive()).thenReturn(Collections.singletonList(newerOpen));
        when(connectAppService.hasConfiguredSecret(newerOpen)).thenReturn(true);
        when(connectBindSupport.isSamePlatform(newerOpen)).thenReturn(false);
        when(connectAppService.tryFillDefaultUris(eq(newerOpen), isNull())).thenReturn(true);

        AuthLoginProviderList list = authLoginProviderService.listPageProviders();

        assertEquals(2, list.getProviders().size());
        assertEquals("newer_app", list.getProviders().get(0).getAppId());
        assertEquals("older-rp", list.getProviders().get(1).getClientId());
    }
}
