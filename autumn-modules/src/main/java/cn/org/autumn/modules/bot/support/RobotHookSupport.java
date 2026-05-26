package cn.org.autumn.modules.bot.support;

import cn.org.autumn.exception.CodeException;
import cn.org.autumn.utils.SubscriptionMatch;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import org.apache.commons.lang3.StringUtils;

import javax.crypto.Mac;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.crypto.spec.SecretKeySpec;
import java.net.InetAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;

public final class RobotHookSupport {

    private static final String HEADER_TIMESTAMP = "X-Robot-Timestamp";
    private static final String HEADER_SIGNATURE = "X-Robot-Signature";
    private static final String HEADER_EVENT = "X-Robot-Event";

    private RobotHookSupport() {
    }

    public static void validateCallbackUrl(String callbackUrl) throws CodeException {
        if (StringUtils.isBlank(callbackUrl))
            throw new CodeException("回调地址不能为空");
        URI uri;
        try {
            uri = new URI(callbackUrl.trim());
        } catch (Exception e) {
            throw new CodeException("回调地址格式无效");
        }
        String scheme = uri.getScheme();
        if (!"https".equalsIgnoreCase(scheme) && !"http".equalsIgnoreCase(scheme))
            throw new CodeException("回调地址须为http或https");
        String host = uri.getHost();
        if (StringUtils.isBlank(host))
            throw new CodeException("回调地址主机无效");
        if (isBlockedHost(host))
            throw new CodeException("回调地址不允许访问内网或本机");
    }

    public static String formatCallbackBody(Gson gson, String robot, String event, String timestamp, Object data) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("event", event);
        body.put("robot", robot);
        body.put("timestamp", Long.parseLong(timestamp));
        if (data != null)
            body.put("data", data);
        return gson.toJson(body);
    }

    public static String toDataJson(Gson gson, Object payload) {
        if (payload == null)
            return null;
        if (payload instanceof String)
            return StringUtils.isBlank((String) payload) ? null : ((String) payload).trim();
        return gson.toJson(payload);
    }

    public static Object parseDataJson(Gson gson, String dataJson) {
        if (StringUtils.isBlank(dataJson))
            return null;
        try {
            JsonElement element = new JsonParser().parse(dataJson.trim());
            if (element == null || element.isJsonNull())
                return null;
            return gson.fromJson(element, Object.class);
        } catch (JsonSyntaxException e) {
            return dataJson;
        }
    }

    /**
     * 将 JSON 文本字段合并进 Map（用于入站消息 Hook 信封，避免 payload 双重转义）。
     */
    public static void putJsonField(Map<String, Object> target, String key, String json, Gson gson) {
        if (target == null || StringUtils.isBlank(key) || StringUtils.isBlank(json))
            return;
        Object parsed = parseDataJson(gson, json);
        if (parsed != null)
            target.put(key, parsed);
    }

    public static boolean matchesEvent(String events, String event) {
        return SubscriptionMatch.matches(events, event);
    }

    public static String sign(String secret, String timestamp, String body) {
        String key = StringUtils.isBlank(secret) ? "" : secret;
        String payload = timestamp + "." + (body == null ? "" : body);
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] bytes = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    public static String headerTimestamp() {
        return HEADER_TIMESTAMP;
    }

    public static String headerSignature() {
        return HEADER_SIGNATURE;
    }

    public static String headerEvent() {
        return HEADER_EVENT;
    }

    private static boolean isBlockedHost(String host) {
        String lower = host.toLowerCase();
        if ("localhost".equals(lower) || lower.endsWith(".local"))
            return true;
        try {
            InetAddress[] addresses = InetAddress.getAllByName(host);
            for (InetAddress address : addresses) {
                if (address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress() || address.isSiteLocalAddress())
                    return true;
            }
        } catch (Exception ignored) {
            return true;
        }
        return false;
    }
}
