package cn.org.autumn.modules.client.service;

import cn.org.autumn.modules.client.model.RpQrcPendingSession;
import cn.org.autumn.modules.qrc.dto.ScannerBrief;
import cn.org.autumn.modules.qrc.service.QrcWebhookDeliveryService;
import com.alibaba.fastjson2.JSON;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RpQrcInboundDualEventTest {

    @Mock
    private RpQrcPendingStore rpQrcPendingStore;

    @Mock
    private ScanLoginCredentialService scanLoginCredentialService;

    @Mock
    private RpQrcCallbackService rpQrcCallbackService;

    @InjectMocks
    private RpQrcInboundService rpQrcInboundService;

    @Test
    void handleInbound_scanned_updatesPendingAndPushes() {
        RpQrcPendingSession pending = basePending();
        when(rpQrcPendingStore.get("ticket-1")).thenReturn(pending);
        stubSecret();

        Map<String, Object> brief = new HashMap<>();
        brief.put("displayName", "Scanner");
        Map<String, Object> data = new HashMap<>();
        data.put("status", "SCANNED");
        data.put("scannerBrief", brief);
        String json = signedBody("ticket-1", RpQrcInboundService.EVENT_SCANNED, data);

        rpQrcInboundService.handleInbound(json, signatureHeaders(json));

        verify(rpQrcCallbackService).applyScanned(eq(pending), any(com.alibaba.fastjson2.JSONObject.class));
        verify(rpQrcCallbackService, never()).completeOnInbound(any(), any());
    }

    @Test
    void handleInbound_authorized_triggersComplete() {
        RpQrcPendingSession pending = basePending();
        when(rpQrcPendingStore.get("ticket-1")).thenReturn(pending);
        stubSecret();

        Map<String, Object> data = new HashMap<>();
        data.put("code", "auth-code");
        data.put("state", "st");
        String json = signedBody("ticket-1", RpQrcInboundService.EVENT_AUTHORIZED, data);

        rpQrcInboundService.handleInbound(json, signatureHeaders(json));

        verify(rpQrcCallbackService).ensureScannedBeforeAuthorize(eq(pending), any(com.alibaba.fastjson.JSONObject.class));
        verify(rpQrcCallbackService).completeOnInbound(eq(pending), eq("auth-code"));
        verify(rpQrcCallbackService, never()).applyScanned(any(), any());
    }

    @Test
    void handleInbound_authorizedWithoutPriorScan_promotesScannedFirst() {
        RpQrcPendingSession pending = basePending();
        pending.setStatus("PENDING");
        when(rpQrcPendingStore.get("ticket-1")).thenReturn(pending);
        stubSecret();

        Map<String, Object> brief = new HashMap<>();
        brief.put("displayName", "Late Scanner");
        Map<String, Object> data = new HashMap<>();
        data.put("code", "auth-code");
        data.put("scannerBrief", brief);
        String json = signedBody("ticket-1", RpQrcInboundService.EVENT_AUTHORIZED, data);

        rpQrcInboundService.handleInbound(json, signatureHeaders(json));

        verify(rpQrcCallbackService).ensureScannedBeforeAuthorize(eq(pending), any(com.alibaba.fastjson.JSONObject.class));
        verify(rpQrcCallbackService).completeOnInbound(eq(pending), eq("auth-code"));
    }

    @Test
    void handleInbound_denied_marksDenied() {
        RpQrcPendingSession pending = basePending();
        when(rpQrcPendingStore.get("ticket-1")).thenReturn(pending);
        stubSecret();

        Map<String, Object> data = new HashMap<>();
        data.put("status", "DENIED");
        String json = signedBody("ticket-1", RpQrcInboundService.EVENT_DENIED, data);

        rpQrcInboundService.handleInbound(json, signatureHeaders(json));

        verify(rpQrcCallbackService).applyDenied(eq(pending), any(com.alibaba.fastjson.JSONObject.class));
        verify(rpQrcCallbackService, never()).completeOnInbound(any(), any());
    }

    @Test
    void handleInbound_rejectsUnknownEvent() {
        RpQrcPendingSession pending = basePending();
        when(rpQrcPendingStore.get("ticket-1")).thenReturn(pending);
        stubSecret();

        Map<String, Object> data = new HashMap<>();
        data.put("code", "x");
        String json = signedBody("ticket-1", "qrc.unknown", data);

        assertThrows(IllegalArgumentException.class, () -> rpQrcInboundService.handleInbound(json, signatureHeaders(json)));
    }

    private RpQrcPendingSession basePending() {
        RpQrcPendingSession pending = new RpQrcPendingSession();
        pending.setUuid("ticket-1");
        pending.setCredentialType("oauth2_classic");
        pending.setCredentialId("b-web");
        return pending;
    }

    private void stubSecret() {
        cn.org.autumn.modules.client.model.ScanLoginCredentialContext credential = new cn.org.autumn.modules.client.model.ScanLoginCredentialContext();
        credential.setClientSecret("secret");
        when(scanLoginCredentialService.require("oauth2_classic", "b-web")).thenReturn(credential);
    }

    private static String signedBody(String uuid, String event, Map<String, Object> data) {
        Map<String, Object> body = new HashMap<>();
        body.put("uuid", uuid);
        body.put("event", event);
        body.put("data", data);
        return JSON.toJSONString(body);
    }

    private static Map<String, String> signatureHeaders(String json) {
        Map<String, String> headers = new HashMap<>();
        headers.put("X-Qrc-Signature", QrcWebhookDeliveryService.sign(json, "secret"));
        return headers;
    }
}
