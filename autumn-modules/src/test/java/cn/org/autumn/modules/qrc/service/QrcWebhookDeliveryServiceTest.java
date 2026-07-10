package cn.org.autumn.modules.qrc.service;

import cn.org.autumn.modules.qrc.entity.ClientGrantEntity;
import cn.org.autumn.modules.qrc.model.TicketSnapshot;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QrcWebhookDeliveryServiceTest {

    @Test
    void sign_isDeterministicHex() {
        String first = QrcWebhookDeliveryService.sign("{\"uuid\":\"u1\"}", "secret");
        String second = QrcWebhookDeliveryService.sign("{\"uuid\":\"u1\"}", "secret");
        assertEquals(first, second);
        assertEquals(64, first.length());
        assertNotEquals(first, QrcWebhookDeliveryService.sign("{\"uuid\":\"u1\"}", "other"));
    }

    @Test
    void shouldDeliverWebhook_requiresWebhookDeliveryAndUrl() {
        QrcWebhookDeliveryService service = new QrcWebhookDeliveryService();
        TicketSnapshot ticket = new TicketSnapshot();
        ticket.setUuid("t1");
        Map<String, String> payload = new HashMap<>();
        payload.put("delivery", "WEBHOOK");
        payload.put("webhook", "https://b.com/inbound");
        ticket.setPayload(payload);
        assertTrue(service.shouldDeliverWebhook(ticket));

        payload.remove("webhook");
        assertFalse(service.shouldDeliverWebhook(ticket));

        payload.put("webhook", "https://b.com/inbound");
        payload.put("delivery", "POLL_CODE");
        assertFalse(service.shouldDeliverWebhook(ticket));
    }

    @Test
    void eventConstants_matchContract() {
        assertEquals("qrc.scanned", QrcWebhookDeliveryService.EVENT_SCANNED);
        assertEquals("qrc.authorized", QrcWebhookDeliveryService.EVENT_AUTHORIZED);
        ClientGrantEntity grant = new ClientGrantEntity();
        grant.setClientId("demo");
        assertNotEquals("", QrcWebhookDeliveryService.sign("body", "secret"));
    }
}
