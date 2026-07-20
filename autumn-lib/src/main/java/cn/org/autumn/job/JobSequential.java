package cn.org.autumn.job;

import cn.org.autumn.config.Config;
import cn.org.autumn.node.ProfileService;
import cn.org.autumn.node.Registry;
import cn.org.autumn.service.DistributedLockService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;

/**
 * {@link JobDuty#SEQUENTIAL}：在线节点按 uuid 排序，令牌轮转依次执行。
 * <p>
 * 成员来自 {@link Registry#online()}（已过滤陈旧心跳）。令牌为成员下标；越界时归零。
 * 无法保证集群互斥时 fail-closed（跳过），与 {@link JobDutySupport} 一致。
 */
@Slf4j
public final class JobSequential {

    private static final long TOKEN_TTL_SECONDS = 600L;

    private JobSequential() {
    }

    public static void run(String jobId, String lockKey, DistributedLockService locks, Runnable action) throws Exception {
        ProfileService profile = bean(ProfileService.class);
        String self = profile != null ? profile.uuid() : null;
        if (StringUtils.isBlank(self)) {
            runAsSingleton(locks, lockKey, action);
            return;
        }
        List<String> raw = members();
        List<String> ordered = new ArrayList<>();
        if (raw.isEmpty()) {
            ordered.add(self);
        } else {
            ordered.addAll(raw);
            if (!ordered.contains(self)) {
                ordered.add(self);
            }
            Collections.sort(ordered);
        }
        final List<String> members = ordered;
        final int index = members.indexOf(self);
        if (index < 0) {
            return;
        }
        RedissonClient redis = redisson();
        if (redis == null) {
            runAsSingleton(locks, lockKey, action);
            return;
        }
        String tokenKey = "autumn:job:seq:" + jobId + ":token";
        final RBucket<String> bucket = redis.getBucket(tokenKey);
        String token = bucket.get();
        int current;
        if (StringUtils.isBlank(token)) {
            current = 0;
            bucket.set("0", TOKEN_TTL_SECONDS, TimeUnit.SECONDS);
        } else {
            try {
                current = Integer.parseInt(token.trim());
            } catch (NumberFormatException e) {
                current = 0;
            }
            if (current < 0 || current >= members.size()) {
                current = 0;
                bucket.set("0", TOKEN_TTL_SECONDS, TimeUnit.SECONDS);
            }
        }
        // 令牌指向的成员已不在 online 列表时不会出现（下标已夹紧）；仅本机轮次执行
        if (current != index) {
            if (log.isDebugEnabled()) {
                log.debug("SEQUENTIAL skip jobId={} selfIndex={} token={}", jobId, index, current);
            }
            return;
        }
        Callable<Void> body = () -> {
            action.run();
            int next = (index + 1) % members.size();
            bucket.set(String.valueOf(next), TOKEN_TTL_SECONDS, TimeUnit.SECONDS);
            return null;
        };
        if (locks == null || !locks.isClusterMutexAvailable()) {
            log.warn("JobDuty SEQUENTIAL skip: cluster mutex unavailable jobId={}", jobId);
            return;
        }
        locks.withClusterMutexOrSkip(lockKey, body);
    }

    private static List<String> members() {
        Registry registry = bean(Registry.class);
        if (registry == null || !registry.enabled()) {
            return List.of();
        }
        return registry.online();
    }

    private static void runAsSingleton(DistributedLockService locks, String lockKey, Runnable action) throws Exception {
        if (locks == null || !locks.isClusterMutexAvailable()) {
            log.warn("JobDuty SEQUENTIAL degrade-skip: cluster mutex unavailable key={}", lockKey);
            return;
        }
        locks.withClusterMutexOrSkip(lockKey, (Callable<Void>) () -> {
            action.run();
            return null;
        });
    }

    private static RedissonClient redisson() {
        return bean(RedissonClient.class);
    }

    @SuppressWarnings("unchecked")
    private static <T> T bean(Class<T> type) {
        Object o = Config.getBean(type);
        return type.isInstance(o) ? (T) o : null;
    }
}
