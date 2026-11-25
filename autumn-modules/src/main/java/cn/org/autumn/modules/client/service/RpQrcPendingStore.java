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
import cn.org.autumn.utils.RedisUtils;
import com.alibaba.fastjson2.JSON;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * RP 待处理扫码会话（模式 D）：本机内存缓存 + {@link RedisUtils} 存 <strong>JSON 字符串</strong>。
 * <p>
 * <strong>Invariant（破坏即回归「扫码会话不存在或已过期」）</strong>
 * <ul>
 *   <li>持久化通道必须与浏览器 bind（{@code ScanTicketService.putBrowserBind}）同为 {@link RedisUtils}
 *       / {@code autumn.redis.open}，禁止再引入可选 {@code StringRedisTemplate} 分轨。</li>
 *   <li>值必须是 JSON 文本再经 RedisTemplate 写入，禁止把 {@link RpQrcPendingSession} 整对象交给 JDK 序列化
 *       （历史：ScannerBrief 不可序列化导致 inbound 500，见 commit a94c8acd；该次改走 StringRedisTemplate
 *       又造成 bighub 多副本 pending miss，见 docs/AI_MULTI_DOMAIN.md §5.1）。</li>
 *   <li>建票 {@link #saveOrFail}：Redis 不可用、{@link RedisUtils#trySet} 失败均硬失败并回滚内存，禁止幽灵 uuid。</li>
 *   <li>{@link #get} 在 Redis 可用时<strong>优先读 Redis</strong>再回填内存，避免他节点更新后本机内存钉死旧状态。</li>
 * </ul>
 * PC {@code status}/{@code stream} 只读本 store，<strong>不查 AS 票</strong>；升级超然信无法消除 pending miss。
 * 终态 grace：{@link #saveTerminal}；秒级入队 + {@link TagTaskExecutor} drain（docs/AI_ASYNC_TASK.md §4.1）。
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

    private final AtomicReference<Boolean> memoryOnlyWarned = new AtomicReference<>(Boolean.FALSE);

    @Autowired
    private RedisUtils redisUtils;

    @Autowired
    private SysConfigService sysConfigService;

    @Autowired
    private TagTaskExecutor asyncTaskExecutor;

    /**
     * 持久化 pending（状态更新等）。Redis 写失败仍保留本机内存并打 ERROR，不中断业务回调。
     * 多副本下软失败可能导致他节点 status 看不到 SCANNED/COMPLETED，应优先保证 Redis 健康。
     */
    public void save(RpQrcPendingSession session) {
        saveInternal(session, false);
    }

    /**
     * 建票专用：无可用 Redis 或 {@link RedisUtils#trySet} 未写入时回滚内存并抛错，避免浏览器拿无效 uuid 去 stream。
     */
    public void saveOrFail(RpQrcPendingSession session) {
        saveInternal(session, true);
    }

    /** 是否已接入可用 Redis（运维/单测/可观测）。 */
    public boolean isRedisBacked() {
        return redisUtils != null && redisUtils.isAvailable();
    }

    private void saveInternal(RpQrcPendingSession session, boolean failHardOnRedisError) {
        if (session == null || StringUtils.isBlank(session.getUuid())) {
            return;
        }
        memory.put(session.getUuid(), session);
        // 复活/续期时清掉可能已入队的过期清理，避免 saveTerminal 后被 drain 误删
        if (removeQueue.remove(session.getUuid()) != null) {
            log.debug("RP QRC pending removeQueue cleared uuid={}", session.getUuid());
        }
        if (!isRedisBacked()) {
            if (failHardOnRedisError) {
                memory.remove(session.getUuid());
                log.error("RP QRC pending saveOrFail rejected uuid={}: Redis 不可用（autumn.redis.open + RedisTemplate）", session.getUuid());
                throw new IllegalStateException("RP 扫码会话需要 Redis（autumn.redis.open），当前不可用");
            }
            if (memoryOnlyWarned.compareAndSet(Boolean.FALSE, Boolean.TRUE)) {
                log.warn("RP QRC pending 仅内存（RedisUtils 不可用）：多节点集群下 stream/status 会读不到会话，请开启 autumn.redis.open");
            }
            return;
        }
        long ttl = resolveTtlSeconds(session.getExpiredAt());
        String key = KEY_PREFIX + session.getUuid();
        try {
            // 必须用 trySet：set() 对 no-op/超大值静默跳过，硬失败路径不可依赖「无异常即成功」
            boolean written = redisUtils.trySet(key, JSON.toJSONString(session), ttl);
            if (!written) {
                if (failHardOnRedisError) {
                    memory.remove(session.getUuid());
                    log.error("RP QRC pending redis trySet skipped uuid={}（不可用或超大值）", session.getUuid());
                    throw new IllegalStateException("RP 扫码会话写入 Redis 失败，请检查集群 Redis");
                }
                log.error("RP QRC pending redis trySet soft-skipped uuid={}（本机内存仍可读；他节点可能 miss）", session.getUuid());
            }
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            if (failHardOnRedisError) {
                memory.remove(session.getUuid());
                log.error("RP QRC pending redis save failed uuid={}: {}", session.getUuid(), e.getMessage());
                throw new IllegalStateException("RP 扫码会话写入 Redis 失败，请检查集群 Redis", e);
            }
            log.error("RP QRC pending redis save soft-failed uuid={}: {}（本机内存仍可读）", session.getUuid(), e.getMessage());
        }
    }

    /**
     * 读取 pending。Redis 可用时优先 Redis（多副本权威源），再回填本机；Redis miss 时回落本机内存
     * （同节点 soft-save 场景）。勿改回「内存优先」——否则他节点 inbound 更新后本机 poll 钉死旧状态。
     */
    public RpQrcPendingSession get(String uuid) {
        if (StringUtils.isBlank(uuid)) {
            return null;
        }
        if (isRedisBacked()) {
            try {
                Object raw = redisUtils.get(KEY_PREFIX + uuid);
                RpQrcPendingSession fromRedis = parseSession(raw);
                if (fromRedis != null) {
                    memory.put(uuid, fromRedis);
                    return fromRedis;
                }
            } catch (Exception e) {
                log.warn("RP QRC pending redis read failed uuid={}: {}", uuid, e.getMessage());
            }
        }
        return memory.get(uuid);
    }

    public void remove(String uuid) {
        if (StringUtils.isBlank(uuid)) {
            return;
        }
        memory.remove(uuid);
        removeQueue.remove(uuid);
        if (isRedisBacked()) {
            try {
                redisUtils.delete(KEY_PREFIX + uuid);
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
        // 终态：Redis 失败仍保留本机 grace，供本节点 SSE/poll；不阻断 inbound 完成链
        saveInternal(session, false);
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

    /** 异步 drain：清理本机内存并删 Redis（允许 IO）。删除前复查 expiredAt，避免与 save/saveTerminal 竞态误删。 */
    void drainExpired() {
        long now = System.currentTimeMillis();
        List<String> uuids = new ArrayList<>(removeQueue.keySet());
        for (String uuid : uuids) {
            removeQueue.remove(uuid);
            RpQrcPendingSession live = memory.get(uuid);
            if (live != null && live.getExpiredAt() > now) {
                log.debug("RP QRC pending drain skip revived uuid={}", uuid);
                continue;
            }
            try {
                remove(uuid);
            } catch (Exception e) {
                log.warn("RP QRC pending drain failed uuid={}: {}", uuid, e.getMessage());
            }
        }
    }

    private RpQrcPendingSession parseSession(Object raw) {
        if (raw == null) {
            return null;
        }
        if (raw instanceof RpQrcPendingSession) {
            return (RpQrcPendingSession) raw;
        }
        String text = raw instanceof String ? (String) raw : String.valueOf(raw);
        if (StringUtils.isBlank(text)) {
            return null;
        }
        return JSON.parseObject(text, RpQrcPendingSession.class);
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
