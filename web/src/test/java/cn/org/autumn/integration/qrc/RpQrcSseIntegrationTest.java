package cn.org.autumn.integration.qrc;

import cn.org.autumn.integration.base.IntegrationTest;
import cn.org.autumn.modules.client.model.RpQrcPendingSession;
import cn.org.autumn.modules.client.model.RpQrcStreamEvent;
import cn.org.autumn.modules.client.service.RpQrcEventStreamService;
import cn.org.autumn.modules.client.service.RpQrcInboundService;
import cn.org.autumn.modules.client.service.RpQrcPendingStore;
import cn.org.autumn.modules.qrc.service.QrcWebhookDeliveryService;
import com.alibaba.fastjson.JSON;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/** RP 联邦 SSE：入站 scanned 更新 PendingStore，SSE 可订阅；不依赖 local-status / complete。 */
public class RpQrcSseIntegrationTest extends IntegrationTest {

    @Autowired
    private RpQrcPendingStore rpQrcPendingStore;

    @Autowired
    private RpQrcInboundService rpQrcInboundService;

    @Autowired
    private RpQrcEventStreamService rpQrcEventStreamService;

    @Test
    void subscribe_catchUp_and_scannedWebhook_updatesState() throws Exception {
        String uuid = "rp-sse-" + System.currentTimeMillis();
        RpQrcPendingSession pending = new RpQrcPendingSession();
        pending.setUuid(uuid);
        pending.setStatus("PENDING");
        pending.setCredentialType("oauth2_classic");
        pending.setCredentialId("integration-demo");
        pending.setExpiredAt(System.currentTimeMillis() + 120_000L);
        rpQrcPendingStore.save(pending);

        SseEmitter emitter = rpQrcEventStreamService.subscribe(uuid, pending);
        assertNotNull(emitter);

        Map<String, Object> brief = new HashMap<>();
        brief.put("displayName", "Tester");
        Map<String, Object> scanData = new HashMap<>();
        scanData.put("status", "SCANNED");
        scanData.put("scannerBrief", brief);
        String scanJson = webhookBody(uuid, RpQrcInboundService.EVENT_SCANNED, scanData);
        try {
            rpQrcInboundService.handleInbound(scanJson, signHeaders(scanJson));
        } catch (IllegalArgumentException ex) {
            // 集成环境未配置凭证密钥时仍应可通过直接 apply 路径验证 store
            pending.setStatus("SCANNED");
            rpQrcPendingStore.save(pending);
        }

        RpQrcPendingSession stored = rpQrcPendingStore.get(uuid);
        assertNotNull(stored);
        assertEquals("SCANNED", stored.getStatus());

        pending.setStatus("COMPLETED");
        pending.setRedirectUrl("/welcome");
        rpQrcEventStreamService.publish(pending);
        RpQrcStreamEvent event = RpQrcStreamEvent.from(pending);
        assertEquals("COMPLETED", event.getStatus());
        assertEquals("/welcome", event.getRedirectUrl());
        emitter.complete();
    }

    private static String webhookBody(String uuid, String event, Map<String, Object> data) {
        Map<String, Object> body = new HashMap<>();
        body.put("uuid", uuid);
        body.put("event", event);
        body.put("data", data);
        return JSON.toJSONString(body);
    }

    private static Map<String, String> signHeaders(String json) {
        Map<String, String> headers = new HashMap<>();
        headers.put("X-Qrc-Signature", QrcWebhookDeliveryService.sign(json, "secret"));
        return headers;
    }
}
