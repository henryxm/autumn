package cn.org.autumn.modules.client.service;

import cn.org.autumn.model.AuthLoginProviderType;
import cn.org.autumn.modules.client.entity.WebAuthenticationEntity;
import cn.org.autumn.modules.client.model.ScanLoginCredentialContext;
import cn.org.autumn.modules.client.oauth2.WebOauthEndpointResolver;
import cn.org.autumn.modules.opc.entity.ConnectAppEntity;
import cn.org.autumn.modules.opc.service.ConnectAppService;
import cn.org.autumn.modules.sys.service.SysConfigService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScanLoginCredentialServiceTest {

    @Mock
    private WebAuthenticationService webAuthenticationService;

    @Mock
    private ConnectAppService connectAppService;

    @Mock
    private WebOauthEndpointResolver webOauthEndpointResolver;

    @Mock
    private SysConfigService sysConfigService;

    @InjectMocks
    private ScanLoginCredentialService scanLoginCredentialService;

    @Test
    void require_classicRemoteRp() {
        WebAuthenticationEntity web = new WebAuthenticationEntity();
        web.setClientId("b-web");
        web.setName("B Web");
        web.setClientSecret("secret");
        web.setRedirectUri("https://b.com/client/oauth2/callback");
        web.setOriginUri("https://a.com");
        when(webAuthenticationService.getByClientId("b-web")).thenReturn(web);
        when(webOauthEndpointResolver.resolveOriginUri(web)).thenReturn("https://a.com");
        when(webOauthEndpointResolver.hasRemoteOrigin(web)).thenReturn(true);

        ScanLoginCredentialContext ctx = scanLoginCredentialService.require(AuthLoginProviderType.OAUTH2_CLASSIC, "b-web");

        assertEquals("rp", ctx.getQrcMode());
        assertEquals("b-web", ctx.getClientId());
    }

    @Test
    void require_openCrossPlatform() {
        ConnectAppEntity app = new ConnectAppEntity();
        app.setAppId("app_b");
        app.setName("B Open");
        app.setRedirectUri("https://b.com/open/oauth2/callback?appId=app_b");
        app.setPlatformBaseUrl("https://a.com");
        app.setStatus(ConnectAppEntity.STATUS_ACTIVE);
        when(connectAppService.getByAppId("app_b")).thenReturn(app);
        when(connectAppService.hasConfiguredSecret(app)).thenReturn(true);
        when(connectAppService.requirePlainSecret(app)).thenReturn("opl-secret");
        when(connectAppService.tryFillDefaultUris(app, null)).thenReturn(true);
        when(sysConfigService.getBaseUrl()).thenReturn("https://b.com");

        ScanLoginCredentialContext ctx = scanLoginCredentialService.require(AuthLoginProviderType.OAUTH2_OPEN, "app_b");

        assertEquals("rp", ctx.getQrcMode());
        assertEquals("app_b", ctx.getClientId());
    }

    @Test
    void require_invalidType() {
        assertThrows(IllegalArgumentException.class, () -> scanLoginCredentialService.require("invalid", "x"));
    }
}
