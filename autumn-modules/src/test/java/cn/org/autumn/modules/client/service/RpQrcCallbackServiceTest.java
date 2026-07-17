package cn.org.autumn.modules.client.service;

import cn.org.autumn.model.AuthLoginProviderType;
import cn.org.autumn.modules.client.entity.WebAuthenticationEntity;
import cn.org.autumn.modules.client.model.RpQrcPendingSession;
import cn.org.autumn.modules.client.model.ScanLoginCredentialContext;
import cn.org.autumn.modules.opc.dto.ConnectOAuthFinishResult;
import cn.org.autumn.modules.opc.entity.ConnectAppEntity;
import cn.org.autumn.modules.opc.service.ConnectLoginService;
import cn.org.autumn.modules.sys.service.SysConfigService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RpQrcCallbackServiceTest {

    @Mock
    private ScanLoginCredentialService scanLoginCredentialService;

    @Mock
    private WebOauthLoginService webOauthLoginService;

    @Mock
    private ConnectLoginService connectLoginService;

    @Mock
    private RpQrcPendingStore rpQrcPendingStore;

    @Mock
    private RpQrcSessionContextService rpQrcSessionContextService;

    @Mock
    private RpQrcEventStreamService rpQrcEventStreamService;

    @Mock
    private SysConfigService sysConfigService;

    @InjectMocks
    private RpQrcCallbackService rpQrcCallbackService;

    @Test
    void completeOnInbound_classicUsesWebOauthLoginService() {
        RpQrcPendingSession pending = pending(AuthLoginProviderType.OAUTH2_CLASSIC, "rp-demo");
        ScanLoginCredentialContext credential = classicCredential();
        when(scanLoginCredentialService.require(AuthLoginProviderType.OAUTH2_CLASSIC, "rp-demo")).thenReturn(credential);
        when(sysConfigService.getBaseUrl()).thenReturn("https://b.com");
        stubSessionRunner();

        rpQrcCallbackService.completeOnInbound(pending, "classic-code");

        verify(webOauthLoginService).completeRemoteOAuthCallback(any(), eq(credential.getWebAuth()), eq("classic-code"));
        assertEquals("COMPLETED", pending.getStatus());
        verify(rpQrcPendingStore).saveTerminal(eq(pending), eq(RpQrcPendingStore.TERMINAL_GRACE_MS));
        verify(rpQrcEventStreamService).publish(pending);
    }

    @Test
    void completeOnInbound_openUsesConnectLoginService() {
        RpQrcPendingSession pending = pending(AuthLoginProviderType.OAUTH2_OPEN, "app_demo");
        ScanLoginCredentialContext credential = openCredential();
        when(scanLoginCredentialService.require(AuthLoginProviderType.OAUTH2_OPEN, "app_demo")).thenReturn(credential);
        when(sysConfigService.getBaseUrl()).thenReturn("https://b.com");
        when(connectLoginService.finishOAuthLogin(any(), eq(credential.getConnectApp()), eq("open-code"), eq("/welcome"))).thenReturn(ConnectOAuthFinishResult.success("/welcome"));
        stubSessionRunner();

        rpQrcCallbackService.completeOnInbound(pending, "open-code");

        verify(connectLoginService).finishOAuthLogin(any(), eq(credential.getConnectApp()), eq("open-code"), eq("/welcome"));
        assertEquals("COMPLETED", pending.getStatus());
        assertEquals("/welcome", pending.getRedirectUrl());
        assertEquals("open-code", pending.getCode());
    }

    @Test
    void completeOnInbound_openBindChoiceSetsOpenBindUrl() {
        RpQrcPendingSession pending = pending(AuthLoginProviderType.OAUTH2_OPEN, "app_demo");
        ScanLoginCredentialContext credential = openCredential();
        when(scanLoginCredentialService.require(AuthLoginProviderType.OAUTH2_OPEN, "app_demo")).thenReturn(credential);
        when(sysConfigService.getBaseUrl()).thenReturn("https://b.com");
        when(connectLoginService.finishOAuthLogin(any(), any(), eq("open-code"), any())).thenReturn(ConnectOAuthFinishResult.bindChoice("/open/oauth2/bind/choice?token=pending-1&appId=app_demo"));
        stubSessionRunner();

        rpQrcCallbackService.completeOnInbound(pending, "open-code");

        assertEquals("COMPLETED", pending.getStatus());
        assertEquals("/open/oauth2/bind/choice?token=pending-1&appId=app_demo", pending.getRedirectUrl());
    }

    @Test
    void completeOnInbound_openWithoutAppThrows() {
        RpQrcPendingSession pending = pending(AuthLoginProviderType.OAUTH2_OPEN, "app_demo");
        ScanLoginCredentialContext credential = new ScanLoginCredentialContext();
        credential.setType(AuthLoginProviderType.OAUTH2_OPEN);
        credential.setId("app_demo");
        when(scanLoginCredentialService.require(AuthLoginProviderType.OAUTH2_OPEN, "app_demo")).thenReturn(credential);
        stubSessionRunner();

        assertThrows(IllegalStateException.class, () -> rpQrcCallbackService.completeOnInbound(pending, "open-code"));
    }

    private void stubSessionRunner() {
        doAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(1);
            runnable.run();
            return true;
        }).when(rpQrcSessionContextService).runWithBrowserSession(any(), any());
    }

    private static RpQrcPendingSession pending(String type, String id) {
        RpQrcPendingSession pending = new RpQrcPendingSession();
        pending.setUuid("ticket-1");
        pending.setCredentialType(type);
        pending.setCredentialId(id);
        pending.setBrowserSessionId("browser-session");
        pending.setCallback("/welcome");
        return pending;
    }

    private static ScanLoginCredentialContext classicCredential() {
        WebAuthenticationEntity web = new WebAuthenticationEntity();
        web.setClientId("rp-demo");
        ScanLoginCredentialContext ctx = new ScanLoginCredentialContext();
        ctx.setType(AuthLoginProviderType.OAUTH2_CLASSIC);
        ctx.setId("rp-demo");
        ctx.setWebAuth(web);
        return ctx;
    }

    private static ScanLoginCredentialContext openCredential() {
        ConnectAppEntity app = new ConnectAppEntity();
        app.setAppId("app_demo");
        ScanLoginCredentialContext ctx = new ScanLoginCredentialContext();
        ctx.setType(AuthLoginProviderType.OAUTH2_OPEN);
        ctx.setId("app_demo");
        ctx.setConnectApp(app);
        return ctx;
    }
}
