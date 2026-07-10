package cn.org.autumn.modules.client.service;

import cn.org.autumn.model.ScanLoginConfig;
import cn.org.autumn.modules.client.model.RpQrcPendingSession;
import cn.org.autumn.modules.qrc.dto.TicketStatusResult;
import cn.org.autumn.modules.sys.service.SysConfigService;
import cn.org.autumn.utils.RedisUtils;
import com.alibaba.fastjson.JSON;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class RpQrcPendingStore {

    private static final String KEY_PREFIX = "rp:qrc:pending:";

    private final Map<String, RpQrcPendingSession> memory = new ConcurrentHashMap<>();

    @Autowired
    private RedisUtils redisUtils;

    @Autowired
    private SysConfigService sysConfigService;

    public void save(RpQrcPendingSession session) {
        if (session == null || StringUtils.isBlank(session.getUuid())) {
            return;
        }
        memory.put(session.getUuid(), session);
        if (redisUtils.isOpen()) {
            long ttl = resolveTtlSeconds(session.getExpiredAt());
            redisUtils.set(KEY_PREFIX + session.getUuid(), session, ttl);
        }
    }

    public RpQrcPendingSession get(String uuid) {
        if (StringUtils.isBlank(uuid)) {
            return null;
        }
        RpQrcPendingSession cached = memory.get(uuid);
        if (cached != null) {
            return cached;
        }
        if (redisUtils.isOpen()) {
            Object raw = redisUtils.get(KEY_PREFIX + uuid);
            RpQrcPendingSession fromRedis = parseSession(raw);
            if (fromRedis != null) {
                memory.put(uuid, fromRedis);
                return fromRedis;
            }
        }
        return null;
    }

    public void remove(String uuid) {
        if (StringUtils.isBlank(uuid)) {
            return;
        }
        memory.remove(uuid);
        if (redisUtils.isOpen()) {
            redisUtils.delete(KEY_PREFIX + uuid);
        }
    }

    public TicketStatusResult toStatusResult(RpQrcPendingSession session) {
        if (session == null) {
            return null;
        }
        TicketStatusResult result = new TicketStatusResult();
        result.setUuid(session.getUuid());
        result.setStatus(session.getStatus());
        result.setResult(session.getResult());
        result.setScannerBrief(session.getScannerBrief());
        result.setRedirect(session.getRedirectUrl());
        long remain = (session.getExpiredAt() - System.currentTimeMillis()) / 1000;
        result.setExpireIn(Math.max(0, remain));
        return result;
    }

    private RpQrcPendingSession parseSession(Object raw) {
        if (raw == null) {
            return null;
        }
        if (raw instanceof RpQrcPendingSession) {
            return (RpQrcPendingSession) raw;
        }
        return JSON.parseObject(JSON.toJSONString(raw), RpQrcPendingSession.class);
    }

    private long resolveTtlSeconds(long expiredAt) {
        long remain = (expiredAt - System.currentTimeMillis()) / 1000;
        if (remain > 0) {
            return remain;
        }
        ScanLoginConfig config = sysConfigService.getConfigObject(ScanLoginConfig.CONFIG_KEY, ScanLoginConfig.class);
        int fallback = config == null ? 300 : config.getTicketTtlSeconds();
        return Math.max(60, fallback);
    }
}
