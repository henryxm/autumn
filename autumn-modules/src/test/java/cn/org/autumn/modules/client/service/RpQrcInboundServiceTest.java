package cn.org.autumn.modules.client.service;

import cn.org.autumn.modules.client.model.RpQrcPendingSession;
import cn.org.autumn.modules.qrc.service.QrcWebhookDeliveryService;
import com.alibaba.fastjson2.JSON;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RpQrcInboundServiceTest {

    @Mock
    private RpQrcPendingStore rpQrcPendingStore;

    @Mock
    private ScanLoginCredentialService scanLoginCredentialService;

    @Mock
    private RpQrcCallbackService rpQrcCallbackService;

    @InjectMocks
    private RpQrcInboundService rpQrcInboundService;

    @Test
    void handleInbound_authorized_triggersComplete() {
        RpQrcPendingSession pending = new RpQrcPendingSession();
        pending.setUuid("ticket-1");
        pending.setCredentialType("oauth2_classic");
        pending.setCredentialId("b-web");
        when(rpQrcPendingStore.get("ticket-1")).thenReturn(pending);
        cn.org.autumn.modules.client.model.ScanLoginCredentialContext credential = new cn.org.autumn.modules.client.model.ScanLoginCredentialContext();
        credential.setClientSecret("secret");
        when(scanLoginCredentialService.require("oauth2_classic", "b-web")).thenReturn(credential);

        Map<String, Object> data = new HashMap<>();
        data.put("code", "auth-code");
        data.put("state", "st");
        Map<String, Object> body = new HashMap<>();
        body.put("uuid", "ticket-1");
        body.put("event", RpQrcInboundService.EVENT_AUTHORIZED);
        body.put("data", data);
        String json = JSON.toJSONString(body);
        Map<String, String> headers = new HashMap<>();
        headers.put("X-Qrc-Signature", QrcWebhookDeliveryService.sign(json, "secret"));

        rpQrcInboundService.handleInbound(json, headers);

        verify(rpQrcCallbackService).completeOnInbound(eq(pending), eq("auth-code"));
    }

    @Test
    void handleInbound_rejectsBadSignature() {
        RpQrcPendingSession pending = new RpQrcPendingSession();
        pending.setUuid("ticket-1");
        pending.setCredentialType("oauth2_classic");
        pending.setCredentialId("b-web");
        when(rpQrcPendingStore.get("ticket-1")).thenReturn(pending);
        cn.org.autumn.modules.client.model.ScanLoginCredentialContext credential = new cn.org.autumn.modules.client.model.ScanLoginCredentialContext();
        credential.setClientSecret("secret");
        when(scanLoginCredentialService.require("oauth2_classic", "b-web")).thenReturn(credential);

        assertThrows(IllegalArgumentException.class, () -> rpQrcInboundService.handleInbound("{\"uuid\":\"ticket-1\",\"event\":\"qrc.authorized\",\"data\":{\"code\":\"x\"}}", new HashMap<String, String>()));
    }
}
