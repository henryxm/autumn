package cn.org.autumn.modules.qrc.service;

import cn.org.autumn.model.ScanLoginConfig;
import cn.org.autumn.modules.oauth.entity.ClientDetailsEntity;
import cn.org.autumn.modules.oauth.service.ClientDetailsService;
import cn.org.autumn.modules.qrc.entity.ClientGrantEntity;
import cn.org.autumn.modules.qrc.model.TicketPayloads;
import cn.org.autumn.modules.qrc.model.TicketSnapshot;
import cn.org.autumn.modules.sys.service.SysConfigService;
import cn.org.autumn.utils.HttpClientUtils;
import com.google.gson.Gson;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

/** QRC Webhook 投递：扫码 {@code qrc.scanned} 与确认 {@code qrc.authorized} 共用签名与 HTTP 投递。 */
@Slf4j
@Service
public class QrcWebhookDeliveryService {

    public static final String EVENT_SCANNED = "qrc.scanned";
    public static final String EVENT_AUTHORIZED = "qrc.authorized";

    private final Gson gson = new Gson();

    @Autowired
    @Lazy
    private ClientDetailsService clientDetailsService;

    @Autowired
    @Lazy
    private SysConfigService sysConfigService;

    public void deliverScanned(TicketSnapshot ticket, ClientGrantEntity grant, Map<String, Object> data) {
        deliver(ticket, grant, EVENT_SCANNED, data);
    }

    public void deliverAuthorized(TicketSnapshot ticket, ClientGrantEntity grant, Map<String, String> result) {
        Map<String, Object> data = new HashMap<>();
        if (result != null) {
            data.putAll(result);
        }
        deliver(ticket, grant, EVENT_AUTHORIZED, data);
    }

    public void deliver(TicketSnapshot ticket, ClientGrantEntity grant, String event, Map<String, Object> data) {
        if (ticket == null || StringUtils.isBlank(event)) {
            return;
        }
        String webhook = resolveWebhookUrl(ticket, grant);
        if (StringUtils.isBlank(webhook)) {
            return;
        }
        Map<String, Object> body = new HashMap<>();
        body.put("event", event);
        body.put("uuid", ticket.getUuid());
        body.put("timestamp", System.currentTimeMillis());
        body.put("data", data == null ? new HashMap<>() : data);
        String json = gson.toJson(body);
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json;charset=UTF-8");
        headers.put("X-Qrc-Event", event);
        headers.put("X-Qrc-Timestamp", String.valueOf(System.currentTimeMillis()));
        String secret = resolveWebhookSecret(ticket, grant);
        if (StringUtils.isNotBlank(secret)) {
            headers.put("X-Qrc-Signature", sign(json, secret));
        }
        try {
            ScanLoginConfig config = sysConfigService.getConfigObjectValidate(ScanLoginConfig.CONFIG_KEY, ScanLoginConfig.class);
            int timeout = config == null ? new ScanLoginConfig().getWebhookTimeoutMs() : config.getWebhookTimeoutMs();
            HttpClientUtils.doPostJson(webhook, json, headers, timeout);
        } catch (Exception e) {
            log.warn("QRC webhook delivery failed ticket={} event={}: {}", ticket.getUuid(), event, e.getMessage());
        }
    }

    public boolean shouldDeliverWebhook(TicketSnapshot ticket) {
        if (ticket == null || ticket.getPayload() == null) {
            return false;
        }
        String delivery = TicketPayloads.get(ticket, "delivery");
        if (!"WEBHOOK".equalsIgnoreCase(delivery)) {
            return false;
        }
        return StringUtils.isNotBlank(TicketPayloads.get(ticket, "webhook"));
    }

    private String resolveWebhookUrl(TicketSnapshot ticket, ClientGrantEntity grant) {
        String webhook = TicketPayloads.get(ticket, "webhook");
        if (StringUtils.isBlank(webhook) && grant != null) {
            webhook = grant.getWebhook();
        }
        return webhook;
    }

    private String resolveWebhookSecret(TicketSnapshot ticket, ClientGrantEntity grant) {
        String payloadSecret = TicketPayloads.get(ticket, "webhookSecret");
        if (StringUtils.isNotBlank(payloadSecret)) {
            return payloadSecret;
        }
        if (grant != null && StringUtils.isNotBlank(grant.getSecret())) {
            return grant.getSecret();
        }
        String clientId = TicketPayloads.get(ticket, "clientId");
        if (StringUtils.isBlank(clientId)) {
            return null;
        }
        try {
            ClientDetailsEntity client = clientDetailsService.findByClientId(clientId);
            if (client != null && StringUtils.isNotBlank(client.getClientSecret())) {
                return client.getClientSecret();
            }
        } catch (Exception ignored) {
            return null;
        }
        return null;
    }

    public static String sign(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] raw = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : raw) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }
}
