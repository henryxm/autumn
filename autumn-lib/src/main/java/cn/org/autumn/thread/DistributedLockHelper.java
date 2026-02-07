package cn.org.autumn.thread;

import cn.org.autumn.config.Config;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 分布式锁共享工具 — 封装 Redisson 分布式锁的通用操作。
 *
 * <p>供 {@link TagRunnable}（{@code @TagValue(lock=true)} 时）和 {@link LockOnce} 共同使用，
 * 避免分布式锁逻辑的代码重复。</p>
 *
 * <p>此类为包级可见，不对外暴露。</p>
 */
class DistributedLockHelper {

    private static final Logger log = LoggerFactory.getLogger(DistributedLockHelper.class);

    /**
     * 使用 volatile 保证多线程可见性，配合 double-checked locking 实现线程安全的懒加载
     */
    private static volatile RedissonClient redissonClient;

    private DistributedLockHelper() {
        // 工具类，禁止实例化
    }

    // ======================== RedissonClient 管理 ========================

    /**
     * 线程安全的 RedissonClient 懒加载（double-checked locking）。
     *
     * @return RedissonClient 实例，未就绪时返回 null
     */
    static RedissonClient getRedissonClient() {
        if (redissonClient == null) {
            synchronized (DistributedLockHelper.class) {
                if (redissonClient == null) {
                    try {
                        redissonClient = (RedissonClient) Config.getBean(RedissonClient.class);
                    } catch (Exception e) {
                        if (log.isDebugEnabled())
                            log.debug("RedissonClient Bean 未就绪: {}", e.getMessage());
                    }
                }
            }
        }
        return redissonClient;
    }

    // ======================== 锁键构建 ========================

    /**
     * 构建分布式锁的 Redis Key。
     *
     * <p>格式: {@code loopjob:lock:{typeName}:{methodName}}</p>
     * <p>当 type 为默认值(String.class)时使用 taskClass 类名，method 为空时使用 "default"。</p>
     *
     * @param type      @TagValue.type() — 任务来源类
     * @param method    @TagValue.method() — 方法名
     * @param taskClass 任务实例的实际类（降级用）
     * @return Redis 锁键
     */
    static String buildLockKey(Class<?> type, String method, Class<?> taskClass) {
        String typeName = (type != null && type != String.class)
                ? type.getSimpleName()
                : (taskClass != null ? taskClass.getSimpleName() : "Unknown");
        String methodName = (method != null && !method.isEmpty())
                ? method
                : "default";
        return "loopjob:lock:" + typeName + ":" + methodName;
    }

    /**
     * 从 TagValue 注解构建锁键。
     */
    static String buildLockKey(TagValue value, Class<?> taskClass) {
        if (value == null) {
            return "loopjob:lock:" + (taskClass != null ? taskClass.getSimpleName() : "Unknown") + ":default";
        }
        return buildLockKey(value.type(), value.method(), taskClass);
    }

    // ======================== 锁操作 ========================

    /**
     * 安全释放锁（仅在当前线程持有时释放，忽略释放过程中的异常）。
     *
     * @param lock    Redisson 锁对象
     * @param lockKey 锁键（用于日志）
     */
    static void unlockSafely(RLock lock, String lockKey) {
        try {
            if (lock != null && lock.isHeldByCurrentThread()) {
                lock.unlock();
                if (log.isDebugEnabled())
                    log.debug("锁已释放（任务失败，允许故障转移）: key={}", lockKey);
            }
        } catch (Exception e) {
            log.warn("释放锁异常: key={}, error={}", lockKey, e.getMessage());
        }
    }
}
