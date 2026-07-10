package cn.org.autumn.modules.client.service;

import cn.org.autumn.modules.client.model.RpQrcPendingSession;
import cn.org.autumn.modules.qrc.service.QrcWebhookDeliveryService;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RpQrcInboundService {

    private static final Logger log = LoggerFactory.getLogger(RpQrcInboundService.class);

    public static final String EVENT_SCANNED = QrcWebhookDeliveryService.EVENT_SCANNED;
    public static final String EVENT_AUTHORIZED = QrcWebhookDeliveryService.EVENT_AUTHORIZED;
    public static final String EVENT_DENIED = QrcWebhookDeliveryService.EVENT_DENIED;

    @Autowired
    private RpQrcPendingStore rpQrcPendingStore;

    @Autowired
    private ScanLoginCredentialService scanLoginCredentialService;

    @Autowired
    private RpQrcCallbackService rpQrcCallbackService;

    public void handleInbound(String rawBody, Map<String, String> headers) {
        if (StringUtils.isBlank(rawBody)) {
            throw new IllegalArgumentException("回调体为空");
        }
        JSONObject root = JSON.parseObject(rawBody);
        if (root == null) {
            throw new IllegalArgumentException("回调体格式无效");
        }
        String uuid = root.getString("uuid");
        if (StringUtils.isBlank(uuid)) {
            throw new IllegalArgumentException("uuid 不能为空");
        }
        RpQrcPendingSession pending = rpQrcPendingStore.get(uuid);
        if (pending == null) {
            throw new IllegalArgumentException("未知或已过期的扫码会话");
        }
        verifySignature(rawBody, headers, pending);
        JSONObject data = root.getJSONObject("data");
        if (data == null) {
            throw new IllegalArgumentException("回调 data 为空");
        }
        String event = StringUtils.defaultIfBlank(root.getString("event"), header(headers, "X-Qrc-Event"));
        if (log.isDebugEnabled()) {
            log.debug("RP QRC inbound uuid={} event={} status={}", uuid, event, pending.getStatus());
        }
        if (EVENT_SCANNED.equalsIgnoreCase(event)) {
            rpQrcCallbackService.applyScanned(pending, data);
            return;
        }
        if (EVENT_AUTHORIZED.equalsIgnoreCase(event)) {
            handleAuthorized(pending, data);
            return;
        }
        if (EVENT_DENIED.equalsIgnoreCase(event)) {
            rpQrcCallbackService.applyDenied(pending, data);
            return;
        }
        throw new IllegalArgumentException("不支持的 Webhook 事件: " + event);
    }

    private void handleAuthorized(RpQrcPendingSession pending, JSONObject data) {
        String code = data.getString("code");
        if (StringUtils.isBlank(code)) {
            throw new IllegalArgumentException("回调未包含授权码");
        }
        pending.setState(StringUtils.defaultString(data.getString("state")));
        Map<String, String> result = new HashMap<>();
        result.put("code", code);
        result.put("state", pending.getState());
        if (StringUtils.isNotBlank(data.getString("clientId"))) {
            result.put("clientId", data.getString("clientId"));
        } else if (StringUtils.isNotBlank(pending.getClientId())) {
            result.put("clientId", pending.getClientId());
        }
        pending.setResult(result);
        rpQrcCallbackService.ensureScannedBeforeAuthorize(pending, data);
        rpQrcCallbackService.completeOnInbound(pending, code);
    }

    private void verifySignature(String rawBody, Map<String, String> headers, RpQrcPendingSession pending) {
        String signature = header(headers, "X-Qrc-Signature");
        if (StringUtils.isBlank(signature)) {
            throw new IllegalArgumentException("缺少签名头 X-Qrc-Signature");
        }
        String secret = resolveWebhookSecret(pending);
        if (StringUtils.isBlank(secret)) {
            throw new IllegalStateException("未配置 Webhook 签名密钥");
        }
        String expected = QrcWebhookDeliveryService.sign(rawBody, secret);
        if (!signature.equalsIgnoreCase(expected)) {
            throw new IllegalArgumentException("Webhook 签名校验失败");
        }
    }

    private String resolveWebhookSecret(RpQrcPendingSession pending) {
        if (pending == null || StringUtils.isBlank(pending.getCredentialType()) || StringUtils.isBlank(pending.getCredentialId())) {
            return null;
        }
        try {
            return scanLoginCredentialService.require(pending.getCredentialType(), pending.getCredentialId()).getClientSecret();
        } catch (Exception e) {
            return null;
        }
    }

    private static String header(Map<String, String> headers, String name) {
        if (headers == null || name == null) {
            return null;
        }
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(name)) {
                return entry.getValue();
            }
        }
        return null;
    }
}
