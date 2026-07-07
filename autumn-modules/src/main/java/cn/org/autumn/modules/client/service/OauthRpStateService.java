package cn.org.autumn.modules.client.service;

import cn.org.autumn.utils.Uuid;
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
        String state = Uuid.uuid();
        store(state, callback);
        return state;
    }

    public String consumeState(String state) {
        if (StringUtils.isBlank(state)) {
            return null;
        }
        if (stringRedisTemplate != null) {
            String key = REDIS_PREFIX + state;
            String callback = stringRedisTemplate.opsForValue().get(key);
            if (callback != null) {
                stringRedisTemplate.delete(key);
            }
            return callback;
        }
        StateEntry entry = memory.remove(state);
        if (entry == null || entry.expired()) {
            return null;
        }
        return entry.callback;
    }

    private void store(String state, String callback) {
        if (stringRedisTemplate != null) {
            stringRedisTemplate.opsForValue().set(REDIS_PREFIX + state, StringUtils.defaultString(callback), TTL_SECONDS, java.util.concurrent.TimeUnit.SECONDS);
            return;
        }
        memory.put(state, new StateEntry(StringUtils.defaultString(callback), System.currentTimeMillis() + TTL_SECONDS * 1000L));
    }

    private static final class StateEntry {
        private final String callback;
        private final long expireAt;

        private StateEntry(String callback, long expireAt) {
            this.callback = callback;
            this.expireAt = expireAt;
        }

        private boolean expired() {
            return System.currentTimeMillis() > expireAt;
        }
    }
}
