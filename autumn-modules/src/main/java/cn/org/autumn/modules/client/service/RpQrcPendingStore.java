package cn.org.autumn.modules.client.service;

import cn.org.autumn.model.ScanLoginConfig;
import cn.org.autumn.modules.client.model.RpQrcPendingSession;
import cn.org.autumn.modules.job.task.LoopJob;
import cn.org.autumn.modules.qrc.dto.TicketStatusResult;
import cn.org.autumn.modules.sys.service.SysConfigService;
import cn.org.autumn.thread.FinishStatus;
import cn.org.autumn.thread.JobPhase;
import cn.org.autumn.thread.JobPhaseGate;
import cn.org.autumn.thread.TagRunnable;
import cn.org.autumn.thread.TagTaskExecutor;
import cn.org.autumn.thread.TagValue;
import com.alibaba.fastjson2.JSON;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * RP 待处理扫码会话：进程内内存 + Redis JSON 字符串（避免 JDK 序列化嵌套 DTO 失败）。
 * <p>
 * 终态 grace：{@link #saveTerminal} 缩短 {@code expiredAt}；Redis 依赖 TTL。
 * 秒级 {@link LoopJob.ThirtySecond} 仅扫描内存并入队，Redis/内存删除由 {@link TagTaskExecutor} drain（见 docs/AI_ASYNC_TASK.md §4.1）。
 */
@Slf4j
@Component
public class RpQrcPendingStore implements LoopJob.ThirtySecond {

    private static final String KEY_PREFIX = "rp:qrc:pending:";

    /** 完成后保留短暂窗口，供 SSE 投递与 poll 降级读到 COMPLETED/redirect。 */
    public static final long TERMINAL_GRACE_MS = 30_000L;

    private final Map<String, RpQrcPendingSession> memory = new ConcurrentHashMap<>();

    /** 待异步清理的 uuid（秒级回调只写本队列，不碰 Redis）。 */
    private final Map<String, Boolean> removeQueue = new ConcurrentHashMap<>();

    private final AtomicReference<JobPhase> drainPhase = JobPhaseGate.create();

    @Autowired(required = false)
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private SysConfigService sysConfigService;

    @Autowired
    private TagTaskExecutor asyncTaskExecutor;

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
        removeQueue.remove(uuid);
        if (stringRedisTemplate != null) {
            try {
                stringRedisTemplate.delete(KEY_PREFIX + uuid);
            } catch (Exception e) {
                log.warn("RP QRC pending redis delete failed uuid={}: {}", uuid, e.getMessage());
            }
        }
    }

    /**
     * 终态会话缩短 TTL 并落库，保证 grace 窗口内 poll/SSE 可读。
     * 到期清理：秒级入队 + 异步 drain；勿私建 ScheduledExecutor。
     */
    public void saveTerminal(RpQrcPendingSession session, long graceMs) {
        if (session == null || StringUtils.isBlank(session.getUuid())) {
            return;
        }
        long grace = Math.max(1_000L, graceMs);
        session.setExpiredAt(System.currentTimeMillis() + grace);
        save(session);
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

    /** 秒级：仅内存扫描入队 + 触发 drain，不访问 Redis/DB。 */
    @Override
    public void onThirtySecond() {
        enqueueExpiredKeys();
        scheduleDrain();
    }

    void enqueueExpiredKeys() {
        long now = System.currentTimeMillis();
        for (Map.Entry<String, RpQrcPendingSession> entry : memory.entrySet()) {
            RpQrcPendingSession session = entry.getValue();
            if (session == null || session.getExpiredAt() <= 0 || session.getExpiredAt() >= now) {
                continue;
            }
            removeQueue.put(entry.getKey(), Boolean.TRUE);
        }
    }

    void scheduleDrain() {
        if (removeQueue.isEmpty()) {
            JobPhaseGate.resetIdle(drainPhase);
            return;
        }
        if (!JobPhaseGate.tryBegin(drainPhase)) {
            return;
        }
        asyncTaskExecutor.execute(new TagRunnable() {
            @Override
            protected void onFinished(FinishStatus status) {
                JobPhaseGate.endAndMaybeReschedule(drainPhase, () -> !removeQueue.isEmpty(), () -> scheduleDrain());
            }

            @Override
            @TagValue(type = RpQrcPendingStore.class, method = "drainExpired", tag = "RP扫码过期清理", lock = false)
            public void exe() {
                drainExpired();
            }
        });
    }

    /** 异步 drain：清理本机内存并删 Redis（允许 IO）。 */
    void drainExpired() {
        List<String> uuids = new ArrayList<>(removeQueue.keySet());
        for (String uuid : uuids) {
            removeQueue.remove(uuid);
            try {
                remove(uuid);
            } catch (Exception e) {
                log.warn("RP QRC pending drain failed uuid={}: {}", uuid, e.getMessage());
            }
        }
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
