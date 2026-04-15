package cn.org.autumn.service;

import cn.org.autumn.config.Config;
import cn.org.autumn.model.DistributedLockConfig;
import cn.org.autumn.redis.resilience.RedisResilience;
import cn.org.autumn.utils.Uuid;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.lang.reflect.Method;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
/**
 * 分布式锁服务（框架统一入口）。
 * <p>
 * 配置来源：后台 SysConfig 的 {@code DISTRIBUTED_LOCK_CONFIG}，以对象方式读取并归一化默认值。
 * 不依赖环境变量，默认值见 {@link DistributedLockConfig}。
 * <p>
 * 典型模式：
 * 1) 严格模式：{@link #withLock(String, Callable)}，锁失败抛错，适合强一致写操作；
 * 2) 降级模式：{@link #withLockOrFallback(String, Callable, LockFailureHandler)}，锁失败执行 fallback；
 * 3) 抗雪崩模式：{@link #withLockRetry(String, Callable)}，锁失败后短重试 + 随机退避。
 * <p>
 * 服务降级：
 * - 分布式能力关闭、Redisson关闭或客户端不可用时，自动降级本地执行；
 * - 锁竞争失败时默认严格失败，可通过配置/接口改为降级处理。
 */
public class DistributedLockService {

    @FunctionalInterface
    public interface ThrowableRunnable {
        void run() throws Exception;
    }

    @FunctionalInterface
    public interface LockFailureHandler<R> {
        R onLockFailure(String lockKey, Exception ex) throws Exception;
    }

    @Autowired
    protected ObjectProvider<RedissonClient> redissonProvider;

    @Autowired(required = false)
    protected RedisResilience redisResilience;

    public <R> R executeWithDistributedLock(String lockKey, Callable<R> callable) throws Exception {
        DistributedLockConfig config = getConfig();
        return executeWithDistributedLock(lockKey, config.getWaitMs(), config.getLeaseMs(), callable);
    }

    public <R> R withLock(String lockKey, Callable<R> callable) throws Exception {
        return executeWithDistributedLock(lockKey, callable);
    }

    public void withLock(String lockKey, ThrowableRunnable runnable) throws Exception {
        executeWithDistributedLock(lockKey, () -> {
            runnable.run();
            return null;
        });
    }

    public <R> R withLockUnchecked(String lockKey, Callable<R> callable) {
        try {
            return withLock(lockKey, callable);
        } catch (Exception e) {
            throw new IllegalStateException("execute distributed lock failed: " + lockKey, e);
        }
    }

    public void withLockUnchecked(String lockKey, Runnable runnable) {
        withLockUnchecked(lockKey, () -> {
            runnable.run();
            return null;
        });
    }

    public <R> R executeWithDistributedLock(String lockKey, long waitMs, long leaseMs, Callable<R> callable) throws Exception {
        return executeWithDistributedLock(lockKey, waitMs, leaseMs, callable, null);
    }

    /**
     * 场景1：严格分布式锁。
     * 默认语义：获取失败即抛错，不执行业务，适合扣库存、幂等写入、单任务互斥等强一致场景。
     * <p>
     * 降级语义：
     * 1) 全局开关关闭 / Redisson未启用 / 无RedissonClient 时，自动降级为本地直执行业务；
     * 2) 锁竞争失败是否降级由 DistributedLockConfig.degradeOnAcquireFailure 控制（默认 false）。<br>
     * 3) {@link RedisResilience} 熔断 OPEN 时默认跳过 tryLock 并本地执行；可用 {@code DISTRIBUTED_LOCK_CONFIG.ignoreCircuitBreaker} 强制仍访问 Redis。
     */
    public <R> R executeWithDistributedLock(String lockKey, long waitMs, long leaseMs, Callable<R> callable, LockFailureHandler<R> lockFailureHandler) throws Exception {
        if (callable == null) {
            return null;
        }
        DistributedLockConfig config = getConfig();
        if (!config.isEnabled() || !config.isEnableRedisson()) {
            return callable.call();
        }
        RedissonClient redisson = redissonProvider.getIfAvailable();
        if (redisson == null) {
            return callable.call();
        }
        if (!config.isIgnoreCircuitBreaker() && redisResilience != null && !redisResilience.allowDistributedLock()) {
            log.warn("distributed lock skipped (redis circuit), local execution key={}", lockKey);
            return callable.call();
        }
        String key = buildLockKey(lockKey, config.getKeyPrefix());
        RLock lock = redisson.getLock(key);
        boolean acquired = false;
        try {
            try {
                acquired = lock.tryLock(Math.max(waitMs, 0), Math.max(leaseMs, 1000), TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw e;
            } catch (Exception e) {
                log.warn("distributed lock redis unreachable, local execution key={}", key, e);
                if (redisResilience != null && RedisResilience.isInfrastructureFailure(e)) {
                    redisResilience.recordFailure();
                }
                return callable.call();
            }
            if (!acquired) {
                IllegalStateException ex = new IllegalStateException("acquire distributed lock failed: " + key);
                if (lockFailureHandler != null) {
                    return lockFailureHandler.onLockFailure(key, ex);
                }
                if (config.isDegradeOnAcquireFailure()) {
                    log.warn("distributed lock contention degraded key={}", key);
                    return callable.call();
                }
                throw ex;
            }
            return callable.call();
        } finally {
            if (acquired) {
                try {
                    if (lock.isHeldByCurrentThread()) {
                        lock.unlock();
                    }
                    if (redisResilience != null) {
                        redisResilience.recordSuccess();
                    }
                } catch (Exception ex) {
                    log.warn("unlock distributed lock failed key={} err={}", key, ex.toString());
                    if (redisResilience != null && RedisResilience.isInfrastructureFailure(ex)) {
                        redisResilience.recordFailure();
                    }
                }
            }
        }
    }

    /**
     * 场景2：锁竞争失败后自定义降级逻辑。
     * 例如：返回缓存快照、跳过重复执行、标记稍后重试。
     */
    public <R> R withLockOrFallback(String lockKey, Callable<R> callable, LockFailureHandler<R> fallback) throws Exception {
        DistributedLockConfig config = getConfig();
        return executeWithDistributedLock(lockKey, config.getWaitMs(), config.getLeaseMs(), callable, fallback);
    }

    /**
     * 场景3：雪崩防护（短重试 + 抖动退避）。
     * 锁竞争失败后不会立即热重试，而是随机退避，减少同一时刻对同一锁的集中冲击。
     */
    public <R> R withLockRetry(String lockKey, Callable<R> callable) throws Exception {
        DistributedLockConfig config = getConfig();
        int retryTimes = config.getRetryTimes();
        Exception lastEx = null;
        for (int i = 0; i <= retryTimes; i++) {
            try {
                return executeWithDistributedLock(lockKey, config.getWaitMs(), config.getLeaseMs(), callable);
            } catch (Exception ex) {
                lastEx = ex;
                if (i >= retryTimes) {
                    break;
                }
                sleepBackoff(config.getRetryBackoffMinMs(), config.getRetryBackoffMaxMs());
            }
        }
        if (lastEx instanceof RuntimeException) {
            throw (RuntimeException) lastEx;
        }
        throw lastEx;
    }

    public <R> R withLockOrFallbackUnchecked(String lockKey, Callable<R> callable, LockFailureHandler<R> fallback) {
        try {
            return withLockOrFallback(lockKey, callable, fallback);
        } catch (Exception e) {
            throw new IllegalStateException("execute distributed lock fallback failed: " + lockKey, e);
        }
    }

    public <R> R withLockRetryUnchecked(String lockKey, Callable<R> callable) {
        try {
            return withLockRetry(lockKey, callable);
        } catch (Exception e) {
            throw new IllegalStateException("execute distributed lock retry failed: " + lockKey, e);
        }
    }

    protected String buildLockKey(String lockKey) {
        return buildLockKey(lockKey, null);
    }

    protected String buildLockKey(String lockKey, String keyPrefix) {
        String k = lockKey == null ? "" : lockKey.trim();
        if (k.isEmpty()) {
            k = Uuid.uuid();
        }
        String prefix = keyPrefix == null ? "autumn:lock:" : keyPrefix.trim();
        if (prefix.isEmpty()) {
            prefix = "autumn:lock:";
        }
        return prefix + k;
    }

    protected DistributedLockConfig getConfig() {
        try {
            Object bean = Config.getBean("sysConfigService");
            if (bean != null) {
                Method method = bean.getClass().getMethod("getObject", String.class, Class.class);
                Object config = method.invoke(bean, DistributedLockConfig.CONFIG_KEY, DistributedLockConfig.class);
                if (config instanceof DistributedLockConfig) {
                    DistributedLockConfig lockConfig = (DistributedLockConfig) config;
                    lockConfig.normalize();
                    return lockConfig;
                }
            }
        } catch (Exception ex) {
            log.debug("read distributed lock config failed: {}", ex.toString());
        }
        DistributedLockConfig config = new DistributedLockConfig();
        config.normalize();
        return config;
    }

    protected void sleepBackoff(long minMs, long maxMs) throws InterruptedException {
        long sleepMs = minMs;
        if (maxMs > minMs) {
            sleepMs = ThreadLocalRandom.current().nextLong(minMs, maxMs + 1);
        }
        if (sleepMs > 0) {
            TimeUnit.MILLISECONDS.sleep(sleepMs);
        }
    }
}
