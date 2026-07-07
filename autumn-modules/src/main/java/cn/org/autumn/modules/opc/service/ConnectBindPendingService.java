package cn.org.autumn.modules.opc.service;

import cn.org.autumn.modules.opc.dto.ConnectBindPendingContext;
import cn.org.autumn.modules.opc.entity.ConnectAppEntity;
import cn.org.autumn.opl.model.OpenUserInfoSnapshot;
import cn.org.autumn.utils.Uuid;
import com.alibaba.fastjson.JSON;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/** 跨平台 OPC 绑定选择：暂存 userInfo 与 token，供 bind/choice 页消费。 */
@Service
public class ConnectBindPendingService {

    private static final String REDIS_PREFIX = "opc:bind:pending:";
    private static final long TTL_SECONDS = 600L;

    private final Map<String, PendingEntry> memory = new ConcurrentHashMap<>();

    @Autowired(required = false)
    StringRedisTemplate stringRedisTemplate;

    public String save(ConnectAppEntity app, OpenUserInfoSnapshot userInfo, String accessToken, String callback) {
        if (app == null || userInfo == null || StringUtils.isBlank(userInfo.getOpenId())) {
            throw new IllegalArgumentException("待绑定授权信息无效");
        }
        ConnectBindPendingContext context = new ConnectBindPendingContext();
        context.setConnectAppUuid(app.getUuid());
        context.setAppId(app.getAppId());
        context.setUserInfoJson(JSON.toJSONString(userInfo));
        context.setAccessToken(StringUtils.defaultString(accessToken));
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

    public ConnectBindPendingContext peek(String token) {
        String payload = readPayload(token, false);
        if (StringUtils.isBlank(payload)) {
            return null;
        }
        return JSON.parseObject(payload, ConnectBindPendingContext.class);
    }

    public ConnectBindPendingContext consume(String token) {
        String payload = readPayload(token, true);
        if (StringUtils.isBlank(payload)) {
            return null;
        }
        return JSON.parseObject(payload, ConnectBindPendingContext.class);
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
