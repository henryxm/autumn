package cn.org.autumn.job;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import lombok.extern.slf4j.Slf4j;

/**
 * JobDuty.LOCAL 进程内锁：同 JVM、同作用域键互斥；不依赖 Redis，不参与集群调度。
 */
@Slf4j
final class JobLocalLocks {

    private static final ConcurrentHashMap<String, ReentrantLock> LOCKS = new ConcurrentHashMap<>();

    private JobLocalLocks() {
    }

    /**
     * 非阻塞抢锁并执行；未获锁则跳过（与 SINGLETON 静默跳过一致）。
     */
    static void runWithTryLock(String scopedKey, Runnable action) {
        if (action == null) {
            return;
        }
        String key = scopedKey == null || scopedKey.isBlank() ? "local:unknown" : scopedKey;
        ReentrantLock lock = LOCKS.computeIfAbsent(key, k -> new ReentrantLock());
        if (!lock.tryLock()) {
            if (log.isDebugEnabled()) {
                log.debug("JobDuty LOCAL skip: local lock busy key={}", key);
            }
            return;
        }
        try {
            action.run();
        } finally {
            lock.unlock();
        }
    }
}
