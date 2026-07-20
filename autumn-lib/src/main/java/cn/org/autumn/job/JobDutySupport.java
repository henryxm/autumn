package cn.org.autumn.job;

import cn.org.autumn.config.Config;
import cn.org.autumn.node.ProfileService;
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
 */
@Slf4j
public final class JobDutySupport {

    private JobDutySupport() {
    }

    public static boolean allowRoles(String[] requiredRoles) {
        if (requiredRoles == null || requiredRoles.length == 0) {
            return true;
        }
        boolean any = false;
        for (String r : requiredRoles) {
            if (StringUtils.isNotBlank(r)) {
                any = true;
                break;
            }
        }
        if (!any) {
            return true;
        }
        ProfileService profile = bean(ProfileService.class);
        if (profile == null || !profile.adjusted()) {
            return true;
        }
        return profile.hasAll(requiredRoles);
    }

    public static void run(JobDuty duty, String jobId, String lockOverride, Runnable action) throws Exception {
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
            runSingleton(locks, lockKey, action);
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

    private static void runSingleton(DistributedLockService locks, String lockKey, Runnable action) throws Exception {
        if (locks == null || !locks.isClusterMutexAvailable()) {
            log.warn("JobDuty SINGLETON skip: cluster mutex unavailable key={}", lockKey);
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
