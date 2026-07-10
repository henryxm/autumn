package cn.org.autumn.modules.qrc.service;

import cn.org.autumn.modules.oauth.entity.ClientDetailsEntity;
import cn.org.autumn.modules.oauth.service.ClientDetailsService;
import cn.org.autumn.modules.opl.entity.OpenCodeEntity;
import cn.org.autumn.modules.opl.service.OpenCodeService;
import cn.org.autumn.modules.qrc.dto.ConfirmResult;
import cn.org.autumn.modules.qrc.entity.ClientGrantEntity;
import cn.org.autumn.modules.qrc.model.DeliveryMode;
import cn.org.autumn.modules.qrc.model.TicketSnapshot;
import cn.org.autumn.modules.sys.entity.SysUserEntity;
import cn.org.autumn.opl.OplConstants;
import cn.org.autumn.opl.model.OpenAppSnapshot;
import cn.org.autumn.opl.spi.OpenPlatformService;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientGrantServiceDeliverOAuthTest {

    @Mock
    private ClientDetailsService clientDetailsService;

    @Mock
    private OpenPlatformService openPlatformService;

    @Mock
    private OpenCodeService openCodeService;

    @InjectMocks
    private ClientGrantService clientGrantService;

    @Test
    void deliverOAuth_oplAppIssuesOpenCode() throws Exception {
        SysUserEntity user = new SysUserEntity();
        user.setUuid("scanner-1");
        ClientDetailsEntity client = trustedClient("app_open", "https://b.com/open/oauth2/callback?appId=app_open");
        when(clientDetailsService.findByClientId("app_open")).thenReturn(client);

        OpenAppSnapshot app = new OpenAppSnapshot();
        app.setAppId("app_open");
        app.setStatus(OplConstants.STATUS_ACTIVE);
        when(openPlatformService.getApp("app_open")).thenReturn(app);

        OpenCodeEntity codeEntity = new OpenCodeEntity();
        codeEntity.setCode("opl-code-1");
        when(openCodeService.issue(eq("app_open"), eq("scanner-1"), eq(client.getRedirectUri()), isNull(), isNull())).thenReturn(codeEntity);

        TicketSnapshot ticket = ticket("app_open", client.getRedirectUri());
        ConfirmResult result = clientGrantService.deliverOAuth(ticket, grant(), user);

        assertEquals("opl-code-1", result.getResult().get("code"));
        verify(openCodeService).issue(eq("app_open"), eq("scanner-1"), eq(client.getRedirectUri()), isNull(), isNull());
        verify(clientDetailsService, never()).putAuthCode(any(), any());
    }

    @Test
    void deliverOAuth_classicClientIssuesOAuthCode() throws Exception {
        SysUserEntity user = new SysUserEntity();
        user.setUuid("scanner-2");
        ClientDetailsEntity client = trustedClient("rp-demo", "https://b.com/client/oauth2/callback");
        when(clientDetailsService.findByClientId("rp-demo")).thenReturn(client);
        when(openPlatformService.getApp("rp-demo")).thenReturn(null);

        TicketSnapshot ticket = ticket("rp-demo", client.getRedirectUri());
        ConfirmResult result = clientGrantService.deliverOAuth(ticket, grant(), user);

        assertEquals("rp-demo", result.getResult().get("clientId"));
        verify(clientDetailsService).putAuthCode(any(), eq(user));
        verify(openCodeService, never()).issue(any(), any(), any(), any(), any());
    }

    private static ClientGrantEntity grant() {
        ClientGrantEntity grant = new ClientGrantEntity();
        grant.setDelivery(DeliveryMode.POLL_CODE);
        grant.setEnabled(true);
        return grant;
    }

    private static TicketSnapshot ticket(String clientId, String redirectUri) {
        TicketSnapshot ticket = new TicketSnapshot();
        ticket.setUuid("ticket-1");
        Map<String, String> payload = new HashMap<>();
        payload.put("clientId", clientId);
        payload.put("redirectUri", redirectUri);
        payload.put("state", "st");
        ticket.setPayload(payload);
        return ticket;
    }

    private static ClientDetailsEntity trustedClient(String clientId, String redirectUri) {
        ClientDetailsEntity client = new ClientDetailsEntity();
        client.setClientId(clientId);
        client.setClientSecret("secret");
        client.setRedirectUri(redirectUri);
        client.setTrusted(1);
        client.setArchived(0);
        return client;
    }
}
