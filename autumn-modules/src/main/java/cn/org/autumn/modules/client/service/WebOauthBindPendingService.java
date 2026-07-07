package cn.org.autumn.modules.client.service;

import cn.org.autumn.modules.client.dto.WebOauthBindPendingContext;
import cn.org.autumn.modules.client.entity.WebAuthenticationEntity;
import cn.org.autumn.modules.usr.dto.UserProfile;
import cn.org.autumn.utils.Uuid;
import com.alibaba.fastjson2.JSON;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/** 跨实例 OAuth 绑定选择：暂存上游 userInfo 与 token，供 bind/choice 页消费。 */
@Service
public class WebOauthBindPendingService {

    private static final String REDIS_PREFIX = "oauth:web:bind:pending:";
    private static final long TTL_SECONDS = 600L;

    private final Map<String, PendingEntry> memory = new ConcurrentHashMap<>();

    @Autowired(required = false)
    StringRedisTemplate stringRedisTemplate;

    public String save(WebAuthenticationEntity webAuth, UserProfile upstream, String tokenBody, String callback) {
        if (webAuth == null || upstream == null || StringUtils.isBlank(upstream.getUuid())) {
            throw new IllegalArgumentException("待绑定授权信息无效");
        }
        WebOauthBindPendingContext context = new WebOauthBindPendingContext();
        context.setWebAuthUuid(webAuth.getUuid());
        context.setUpstreamJson(JSON.toJSONString(upstream));
        context.setTokenBody(tokenBody);
        context.setCallback(StringUtils.defaultString(callback));
        String token = Uuid.uuid();
        String payload = JSON.toJSONString(context);
        if (stringRedisTemplate != null) {
            stringRedisTemplate.opsForValue().set(REDIS_PREFIX + token, payload, TTL_SECONDS, TimeUnit.SECONDS);
        } else {
            memory.put(token, new PendingEntry(payload, System.currentTimeMillis() + TTL_SECONDS * 1000L));
        }
        return token;
    }

    public WebOauthBindPendingContext peek(String token) {
        String payload = readPayload(token, false);
        if (StringUtils.isBlank(payload)) {
            return null;
        }
        return JSON.parseObject(payload, WebOauthBindPendingContext.class);
    }

    public WebOauthBindPendingContext consume(String token) {
        String payload = readPayload(token, true);
        if (StringUtils.isBlank(payload)) {
            return null;
        }
        return JSON.parseObject(payload, WebOauthBindPendingContext.class);
    }

    private String readPayload(String token, boolean remove) {
        if (StringUtils.isBlank(token)) {
            return null;
        }
        if (stringRedisTemplate != null) {
            String key = REDIS_PREFIX + token;
            String payload = stringRedisTemplate.opsForValue().get(key);
            if (remove && payload != null) {
                stringRedisTemplate.delete(key);
            }
            return payload;
        }
        if (remove) {
            PendingEntry entry = memory.remove(token);
            if (entry == null || entry.expired()) {
                return null;
            }
            return entry.payload;
        }
        PendingEntry entry = memory.get(token);
        if (entry == null || entry.expired()) {
            return null;
        }
        return entry.payload;
    }

    private static final class PendingEntry {
        private final String payload;
        private final long expireAt;

        private PendingEntry(String payload, long expireAt) {
            this.payload = payload;
            this.expireAt = expireAt;
        }

        private boolean expired() {
            return System.currentTimeMillis() > expireAt;
        }
    }
}
