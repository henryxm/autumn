package cn.org.autumn.job;

import cn.org.autumn.config.Config;
import cn.org.autumn.node.ProfileService;
import cn.org.autumn.node.role.ServerRoleGate;
import cn.org.autumn.service.DistributedLockService;
import java.util.Arrays;
import java.util.concurrent.Callable;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

/**
 * LoopJob 职责门禁与锁包裹（完全兼容：{@link JobDuty#ALL} 无框架锁）。
 * <p>
 * {@link JobDuty#SINGLETON}/{@link JobDuty#SEQUENTIAL} 在无法保证跨节点互斥时 fail-closed（跳过），
 * 不走 {@link DistributedLockService} 通用路径的「本地降级执行」。
 * <p>
 * SINGLETON 可选 {@code oncePerPeriod}：先 Redis TIME 分桶 SETNX，再互斥锁；同逻辑周期只跑一台。
 * 业务始终在<strong>持锁线程</strong>内同步执行（{@code async} 仅把整段抢锁+业务丢到线程池，不跨线程持锁）。
 * 角色闸委托 {@link ServerRoleGate}（空 roles / ALL = 全开）。
 */
@Slf4j
public final class JobDutySupport {

    private JobDutySupport() {
    }

    public static boolean allowRoles(String[] requiredRoles) {
        return ServerRoleGate.allowsAll(requiredRoles);
    }

    /** 兼容旧调用：无周期栅栏。 */
    public static void run(JobDuty duty, String jobId, String lockOverride, Runnable action) throws Exception {
        run(duty, jobId, lockOverride, false, 0L, action);
    }

    /**
     * @param oncePerPeriod 仅对 {@link JobDuty#SINGLETON} 生效；为 true 时先占周期桶再抢锁
     * @param periodIntervalMs LoopJob 分类间隔（毫秒），用于分桶；≤0 时栅栏退化为单键
     */
    public static void run(JobDuty duty, String jobId, String lockOverride, boolean oncePerPeriod, long periodIntervalMs, Runnable action) throws Exception {
        JobDuty d = duty == null ? JobDuty.ALL : duty;
        if (d == JobDuty.DISABLED) {
            return;
        }
        if (d == JobDuty.ALL) {
            action.run();
            return;
        }
        String lockKey = StringUtils.isNotBlank(lockOverride) ? lockOverride.trim() : "autumn:job:" + jobId;
        DistributedLockService locks = bean(DistributedLockService.class);
        if (d == JobDuty.SINGLETON) {
            runSingleton(locks, lockKey, oncePerPeriod, periodIntervalMs, action);
            return;
        }
        if (d == JobDuty.SEQUENTIAL) {
            JobSequential.run(jobId, lockKey, locks, action);
            return;
        }
        action.run();
    }

    /**
     * 合并类/方法级 duty：方法级仅当 {@code incoming != ALL} 时覆盖，避免注解缺省 ALL 误伤类级 SINGLETON。
     */
    public static JobDuty mergeDuty(JobDuty current, JobDuty incoming, boolean methodLevel) {
        JobDuty next = incoming != null ? incoming : JobDuty.ALL;
        if (!methodLevel || next != JobDuty.ALL) {
            return next;
        }
        return current != null ? current : JobDuty.ALL;
    }

    /**
     * 合并 oncePerPeriod：方法级注解缺省 false 不把类级 true 打回；方法级 true 可打开。
     */
    public static boolean mergeOncePerPeriod(boolean current, boolean incoming, boolean methodLevel) {
        if (!methodLevel) {
            return incoming;
        }
        return current || incoming;
    }

    private static void runSingleton(DistributedLockService locks, String lockKey, boolean oncePerPeriod, long periodIntervalMs, Runnable action) throws Exception {
        if (locks == null || !locks.isClusterMutexAvailable()) {
            log.warn("JobDuty SINGLETON skip: cluster mutex unavailable key={}", lockKey);
            return;
        }
        if (oncePerPeriod && !JobPeriodFence.tryClaim(lockKey, periodIntervalMs)) {
            if (log.isDebugEnabled()) {
                log.debug("JobDuty SINGLETON skip: period fence key={}", lockKey);
            }
            return;
        }
        locks.withClusterMutexOrSkip(lockKey, (Callable<Void>) () -> {
            action.run();
            return null;
        });
    }

    @SuppressWarnings("unchecked")
    private static <T> T bean(Class<T> type) {
        Object o = Config.getBean(type);
        return type.isInstance(o) ? (T) o : null;
    }

    public static String rolesSummary(String[] roles) {
        if (roles == null || roles.length == 0) {
            return "";
        }
        return String.join(",", Arrays.stream(roles).filter(StringUtils::isNotBlank).toArray(String[]::new));
    }
}
