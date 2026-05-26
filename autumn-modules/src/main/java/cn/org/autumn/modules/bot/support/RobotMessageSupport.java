package cn.org.autumn.modules.bot.support;

import cn.org.autumn.exception.CodeException;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import org.apache.commons.lang.StringUtils;

import java.util.regex.Pattern;

public final class RobotMessageSupport {

    private static final Pattern TYPE_PATTERN = Pattern.compile("^[a-z][a-z0-9._-]{0,63}$");
    private static final Pattern IDEMPOTENCY_KEY_PATTERN = Pattern.compile("^[a-zA-Z0-9._-]{8,64}$");
    /** 业务载荷 JSON 最大字节数（UTF-8） */
    private static final int MAX_PAYLOAD_BYTES = 256 * 1024;

    private RobotMessageSupport() {
    }

    public static void validateType(String type) throws CodeException {
        if (StringUtils.isBlank(type))
            throw new CodeException("消息类型不能为空");
        String trimmed = type.trim();
        if (!TYPE_PATTERN.matcher(trimmed).matches())
            throw new CodeException("消息类型格式无效，须为小写字母开头，仅含字母数字._-，最长64字符");
    }

    /**
     * 校验并规范化为紧凑 JSON 文本（入队与持久化队列用）。
     */
    public static String normalizePayload(String data) throws CodeException {
        if (StringUtils.isBlank(data))
            return null;
        String trimmed = data.trim();
        if (trimmed.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > MAX_PAYLOAD_BYTES)
            throw new CodeException("业务载荷超过大小限制");
        try {
            JsonElement element = new JsonParser().parse(trimmed);
            if (element == null || element.isJsonNull())
                return null;
            return element.toString();
        } catch (JsonSyntaxException e) {
            throw new CodeException("业务载荷不是合法 JSON");
        }
    }

    public static void validateIdempotencyKey(String key) throws CodeException {
        if (StringUtils.isBlank(key))
            return;
        String trimmed = key.trim();
        if (!IDEMPOTENCY_KEY_PATTERN.matcher(trimmed).matches())
            throw new CodeException("幂等键格式无效，须为8-64位字母数字._-");
    }

    public static void assertScope(String scopes, String required) throws CodeException {
        if (StringUtils.isBlank(required))
            return;
        if (StringUtils.isBlank(scopes))
            return;
        if (!cn.org.autumn.utils.SubscriptionMatch.matches(scopes, required))
            throw new CodeException("机器人无此 API 权限:" + required);
    }
}
