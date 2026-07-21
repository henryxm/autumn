package cn.org.autumn.job;

import cn.org.autumn.config.Config;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RBucket;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;

/**
 * SINGLETON 周期栅栏：同一逻辑周期（按 Redis TIME 分桶）全集群只允许一台声明执行权。
 * <p>
 * 解决「先到的节点跑完释放互斥锁后，同周期晚到的节点再次获锁再跑」的问题。
 * Redis 不可用或 TIME 失败时 fail-closed（返回 false）。
 */
@Slf4j
public final class JobPeriodFence {

    /** TTL = 周期 + 容差，避免桶边界时钟抖动导致键过早过期。 */
    static final long TTL_GRACE_MS = 10_000L;

    private JobPeriodFence() {
    }

    /**
     * @param fenceBase 与互斥锁同源的业务键（通常为 lockKey）
     * @param intervalMs LoopJob 分类间隔；≤0 时不做分桶（退化为单键，仍 SETNX）
     * @return true 表示本机首次占桶，可继续抢互斥锁并执行
     */
    public static boolean tryClaim(String fenceBase, long intervalMs) {
        if (StringUtils.isBlank(fenceBase)) {
            return false;
        }
        RedissonClient redis = bean(RedissonClient.class);
        if (redis == null) {
            log.warn("JobPeriodFence skip: no Redisson base={}", fenceBase);
            return false;
        }
        Long redisMs = redisTimeMs(redis);
        if (redisMs == null) {
            log.warn("JobPeriodFence skip: TIME failed base={}", fenceBase);
            return false;
        }
        long bucket = periodBucket(redisMs, intervalMs);
        String key = fenceKey(fenceBase, bucket);
        long ttlMs = ttlMs(intervalMs);
        try {
            RBucket<String> b = redis.getBucket(key);
            boolean ok = b.setIfAbsent("1", Duration.ofMillis(ttlMs));
            if (!ok && log.isDebugEnabled()) {
                log.debug("JobPeriodFence occupied key={} bucket={}", key, bucket);
            }
            return ok;
        } catch (Exception e) {
            log.warn("JobPeriodFence SETNX failed key={}: {}", key, e.toString());
            return false;
        }
    }

    static long periodBucket(long redisTimeMs, long intervalMs) {
        if (intervalMs <= 0) {
            return 0L;
        }
        return Math.floorDiv(redisTimeMs, intervalMs);
    }

    static long ttlMs(long intervalMs) {
        long base = intervalMs > 0 ? intervalMs : TimeUnit.MINUTES.toMillis(1);
        return base + TTL_GRACE_MS;
    }

    static String fenceKey(String fenceBase, long bucket) {
        return "autumn:job:once:" + fenceBase.trim() + ":" + bucket;
    }

    @SuppressWarnings("unchecked")
    static Long redisTimeMs(RedissonClient redis) {
        try {
            Object raw = redis.getScript().eval(RScript.Mode.READ_ONLY, "return redis.call('TIME')", RScript.ReturnType.MULTI, Collections.emptyList());
            if (!(raw instanceof List<?> list) || list.size() < 2) {
                return null;
            }
            long seconds = Long.parseLong(String.valueOf(list.get(0)));
            long micros = Long.parseLong(String.valueOf(list.get(1)));
            return seconds * 1000L + micros / 1000L;
        } catch (Exception e) {
            log.warn("JobPeriodFence redis TIME error: {}", e.toString());
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T bean(Class<T> type) {
        Object o = Config.getBean(type);
        return type.isInstance(o) ? (T) o : null;
    }
}
