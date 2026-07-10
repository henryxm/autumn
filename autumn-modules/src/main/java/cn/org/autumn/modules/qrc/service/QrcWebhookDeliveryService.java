package cn.org.autumn.modules.qrc.service;

import cn.org.autumn.model.ScanLoginConfig;
import cn.org.autumn.modules.oauth.entity.ClientDetailsEntity;
import cn.org.autumn.modules.oauth.service.ClientDetailsService;
import cn.org.autumn.modules.qrc.entity.ClientGrantEntity;
import cn.org.autumn.modules.qrc.model.TicketPayloads;
import cn.org.autumn.modules.qrc.model.TicketSnapshot;
import cn.org.autumn.modules.sys.service.SysConfigService;
import cn.org.autumn.utils.HttpClientUtils;
import cn.org.autumn.utils.HttpClientUtils.HttpPostResult;
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
    public static final String EVENT_DENIED = "qrc.denied";

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

    public void deliverDenied(TicketSnapshot ticket, ClientGrantEntity grant, Map<String, Object> data) {
        deliver(ticket, grant, EVENT_DENIED, data);
    }

    public void deliverAuthorized(TicketSnapshot ticket, ClientGrantEntity grant, Map<String, ?> result) {
        Map<String, Object> data = new HashMap<>();
        if (result != null) {
            data.putAll(result);
        }
        deliver(ticket, grant, EVENT_AUTHORIZED, data);
    }

    public void deliver(TicketSnapshot ticket, ClientGrantEntity grant, String event, Map<String, Object> data) {
        if (ticket == null || StringUtils.isBlank(event)) {
            log.warn("QRC webhook skipped: ticket or event missing event={}", event);
            return;
        }
        String webhook = resolveWebhookUrl(ticket, grant);
        if (StringUtils.isBlank(webhook)) {
            log.warn("QRC webhook skipped: blank URL ticket={} event={} delivery={} clientId={}", ticket.getUuid(), event, TicketPayloads.get(ticket, "delivery"), TicketPayloads.get(ticket, "clientId"));
            return;
        }
        if (!webhook.startsWith("http://") && !webhook.startsWith("https://")) {
            log.warn("QRC webhook URL must be absolute, skip ticket={} event={} url={}", ticket.getUuid(), event, webhook);
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
        } else {
            log.warn("QRC webhook unsigned ticket={} event={} clientId={}", ticket.getUuid(), event, TicketPayloads.get(ticket, "clientId"));
        }
        ScanLoginConfig config = sysConfigService.getConfigObjectValidate(ScanLoginConfig.CONFIG_KEY, ScanLoginConfig.class);
        int timeout = config == null ? new ScanLoginConfig().getWebhookTimeoutMs() : config.getWebhookTimeoutMs();
        log.info("QRC webhook delivering ticket={} event={} url={} clientId={}", ticket.getUuid(), event, webhook, TicketPayloads.get(ticket, "clientId"));
        HttpPostResult httpResult = HttpClientUtils.doPostJsonDetailed(webhook, json, headers, timeout);
        if (StringUtils.isNotBlank(httpResult.getError())) {
            log.warn("QRC webhook delivery failed ticket={} event={} url={}: {}", ticket.getUuid(), event, webhook, httpResult.getError());
            return;
        }
        if (!httpResult.isSuccess()) {
            log.warn("QRC webhook HTTP {} ticket={} event={} url={} body={}", httpResult.getStatusCode(), ticket.getUuid(), event, webhook, abbreviate(httpResult.getBody()));
            return;
        }
        if (!isBusinessSuccess(httpResult.getBody())) {
            log.warn("QRC webhook RP rejected ticket={} event={} url={} body={}", ticket.getUuid(), event, webhook, abbreviate(httpResult.getBody()));
            return;
        }
        log.info("QRC webhook delivered ticket={} event={} url={} http={} body={}", ticket.getUuid(), event, webhook, httpResult.getStatusCode(), abbreviate(httpResult.getBody()));
    }

    private boolean isBusinessSuccess(String body) {
        if (StringUtils.isBlank(body)) {
            return true;
        }
        try {
            com.google.gson.JsonObject json = gson.fromJson(body.trim(), com.google.gson.JsonObject.class);
            if (json != null && json.has("code") && !json.get("code").isJsonNull()) {
                return json.get("code").getAsInt() == 0;
            }
        } catch (Exception ignored) {
        }
        return true;
    }

    private static String abbreviate(String raw) {
        if (raw == null) {
            return "";
        }
        String trimmed = raw.trim();
        return trimmed.length() <= 200 ? trimmed : trimmed.substring(0, 200) + "...";
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
