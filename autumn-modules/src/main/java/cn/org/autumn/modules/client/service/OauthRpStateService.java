package cn.org.autumn.modules.client.service;

import cn.org.autumn.modules.client.dto.OauthRpStatePayload;
import cn.org.autumn.utils.Uuid;
import com.alibaba.fastjson.JSON;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class OauthRpStateService {

    private static final String REDIS_PREFIX = "oauth:rp:state:";
    private static final long TTL_SECONDS = 300L;

    private final Map<String, StateEntry> memory = new ConcurrentHashMap<>();

    @Autowired(required = false)
    StringRedisTemplate stringRedisTemplate;

    public String issueState(String callback) {
        return issueState(callback, null);
    }

    public String issueState(String callback, String clientId) {
        String state = Uuid.uuid();
        store(state, callback, clientId);
        return state;
    }

    public OauthRpStatePayload peekStatePayload(String state) {
        if (StringUtils.isBlank(state)) {
            return null;
        }
        String raw;
        if (stringRedisTemplate != null) {
            raw = stringRedisTemplate.opsForValue().get(REDIS_PREFIX + state);
        } else {
            StateEntry entry = memory.get(state);
            if (entry == null || entry.expired()) {
                return null;
            }
            raw = entry.payload;
        }
        return decodePayload(raw);
    }

    public String consumeState(String state) {
        OauthRpStatePayload payload = consumeStatePayload(state);
        return payload == null ? null : payload.getCallback();
    }

    public OauthRpStatePayload consumeStatePayload(String state) {
        if (StringUtils.isBlank(state)) {
            return null;
        }
        String raw;
        if (stringRedisTemplate != null) {
            String key = REDIS_PREFIX + state;
            raw = stringRedisTemplate.opsForValue().get(key);
            if (raw != null) {
                stringRedisTemplate.delete(key);
            }
        } else {
            StateEntry entry = memory.remove(state);
            if (entry == null || entry.expired()) {
                return null;
            }
            raw = entry.payload;
        }
        return decodePayload(raw);
    }

    private void store(String state, String callback, String clientId) {
        String payload = encodePayload(callback, clientId);
        if (stringRedisTemplate != null) {
            stringRedisTemplate.opsForValue().set(REDIS_PREFIX + state, payload, TTL_SECONDS, java.util.concurrent.TimeUnit.SECONDS);
            return;
        }
        memory.put(state, new StateEntry(payload, System.currentTimeMillis() + TTL_SECONDS * 1000L));
    }

    private static String encodePayload(String callback, String clientId) {
        if (StringUtils.isBlank(clientId)) {
            return StringUtils.defaultString(callback);
        }
        OauthRpStatePayload payload = new OauthRpStatePayload();
        payload.setCallback(StringUtils.defaultString(callback));
        payload.setClientId(clientId.trim());
        return JSON.toJSONString(payload);
    }

    private static OauthRpStatePayload decodePayload(String raw) {
        if (StringUtils.isBlank(raw)) {
            return null;
        }
        String trimmed = raw.trim();
        if (trimmed.startsWith("{")) {
            try {
                OauthRpStatePayload payload = JSON.parseObject(trimmed, OauthRpStatePayload.class);
                if (payload != null) {
                    return payload;
                }
            } catch (Exception ignored) {
            }
        }
        OauthRpStatePayload legacy = new OauthRpStatePayload();
        legacy.setCallback(trimmed);
        return legacy;
    }

    private static final class StateEntry {
        private final String payload;
        private final long expireAt;

        private StateEntry(String payload, long expireAt) {
            this.payload = payload;
            this.expireAt = expireAt;
        }

        private boolean expired() {
            return System.currentTimeMillis() > expireAt;
        }
    }
}
