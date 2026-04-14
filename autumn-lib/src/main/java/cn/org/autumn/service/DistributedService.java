package cn.org.autumn.service;

import com.baomidou.mybatisplus.mapper.BaseMapper;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.concurrent.Callable;

/**
 * 通用分布式服务能力封装：挂在 Service 继承链中，提供更易用的锁 API。
 * <p>
 * 继承链：BaseService -> DistributedService -> ShareCacheService。
 * 使用建议：
 * 1) 业务 Service 已继承 ModuleService/BaseService 时，优先直接调用本类 withLock*；
 * 2) 非继承链组件（独立组件、Listener、Filter 等）请直接注入 DistributedLockService。
 */
public abstract class DistributedService<M extends BaseMapper<T>, T> extends ShareCacheService<M, T> {

    @Autowired
    protected DistributedLockService distributedLockService;

    public <R> R withLock(String lockKey, Callable<R> callable) throws Exception {
        return distributedLockService.withLock(lockKey, callable);
    }

    public void withLock(String lockKey, DistributedLockService.ThrowableRunnable runnable) throws Exception {
        distributedLockService.withLock(lockKey, runnable);
    }

    public <R> R withLockUnchecked(String lockKey, Callable<R> callable) {
        return distributedLockService.withLockUnchecked(lockKey, callable);
    }

    public void withLockUnchecked(String lockKey, Runnable runnable) {
        distributedLockService.withLockUnchecked(lockKey, runnable);
    }

    /**
     * 锁竞争失败时执行 fallback。
     */
    public <R> R withLockOrFallback(String lockKey, Callable<R> callable, DistributedLockService.LockFailureHandler<R> fallback) throws Exception {
        return distributedLockService.withLockOrFallback(lockKey, callable, fallback);
    }

    public <R> R withLockOrFallbackUnchecked(String lockKey, Callable<R> callable, DistributedLockService.LockFailureHandler<R> fallback) {
        return distributedLockService.withLockOrFallbackUnchecked(lockKey, callable, fallback);
    }

    /**
     * 锁竞争重试（带随机退避，缓解并发雪崩）。
     */
    public <R> R withLockRetry(String lockKey, Callable<R> callable) throws Exception {
        return distributedLockService.withLockRetry(lockKey, callable);
    }

    public <R> R withLockRetryUnchecked(String lockKey, Callable<R> callable) {
        return distributedLockService.withLockRetryUnchecked(lockKey, callable);
    }
}
