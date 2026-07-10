package cn.org.autumn.modules.client.service;

import cn.org.autumn.model.ScanLoginConfig;
import cn.org.autumn.modules.client.model.RpQrcPendingSession;
import cn.org.autumn.modules.qrc.dto.TicketStatusResult;
import cn.org.autumn.modules.sys.service.SysConfigService;
import com.alibaba.fastjson.JSON;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/** RP 待处理扫码会话：进程内内存 + Redis JSON 字符串（避免 JDK 序列化嵌套 DTO 失败）。 */
@Slf4j
@Component
public class RpQrcPendingStore {

    private static final String KEY_PREFIX = "rp:qrc:pending:";

    private final Map<String, RpQrcPendingSession> memory = new ConcurrentHashMap<>();

    @Autowired(required = false)
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private SysConfigService sysConfigService;

    public void save(RpQrcPendingSession session) {
        if (session == null || StringUtils.isBlank(session.getUuid())) {
            return;
        }
        memory.put(session.getUuid(), session);
        if (stringRedisTemplate == null) {
            return;
        }
        long ttl = resolveTtlSeconds(session.getExpiredAt());
        try {
            stringRedisTemplate.opsForValue().set(KEY_PREFIX + session.getUuid(), JSON.toJSONString(session), ttl, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("RP QRC pending redis save failed uuid={}: {}", session.getUuid(), e.getMessage());
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
        if (stringRedisTemplate == null) {
            return null;
        }
        try {
            String raw = stringRedisTemplate.opsForValue().get(KEY_PREFIX + uuid);
            RpQrcPendingSession fromRedis = parseSession(raw);
            if (fromRedis != null) {
                memory.put(uuid, fromRedis);
                return fromRedis;
            }
        } catch (Exception e) {
            log.warn("RP QRC pending redis read failed uuid={}: {}", uuid, e.getMessage());
        }
        return null;
    }

    public void remove(String uuid) {
        if (StringUtils.isBlank(uuid)) {
            return;
        }
        memory.remove(uuid);
        if (stringRedisTemplate != null) {
            try {
                stringRedisTemplate.delete(KEY_PREFIX + uuid);
            } catch (Exception e) {
                log.warn("RP QRC pending redis delete failed uuid={}: {}", uuid, e.getMessage());
            }
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

    private RpQrcPendingSession parseSession(String raw) {
        if (StringUtils.isBlank(raw)) {
            return null;
        }
        return JSON.parseObject(raw, RpQrcPendingSession.class);
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
