package cn.org.autumn.thread;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * 分布式锁任务基类 — 保证在多节点并行部署环境下，同一任务在指定时间窗口内只被执行一次。
 *
 * <h3>核心策略</h3>
 * <ul>
 *   <li>使用 Redisson 分布式锁 {@code tryLock(0, leaseTime, MINUTES)}，非阻塞 + 自动过期</li>
 *   <li>成功执行后<b>不主动释放锁</b>，让锁自然过期，防止同一时间窗口内其他节点再次执行</li>
 *   <li>执行失败后<b>主动释放锁</b>，允许其他节点接管重试（故障转移）</li>
 *   <li>Redis 不可用时默认跳过执行，避免多节点重复执行；子类可覆写 {@link #onRedisUnavailable()} 自定义行为</li>
 *   <li><b>错峰延迟</b>：获取锁之前应用随机延迟，减少多节点同时争抢 Redis 锁的压力</li>
 * </ul>
 *
 * <h3>与 {@code @TagValue(lock=true)} 的区别</h3>
 * <p>{@code LockOnce} 是继承式用法，提供 {@link #onRedisUnavailable()} 可覆写钩子。
 * 如果不需要自定义 Redis 不可用时的行为，可直接使用 {@code @TagValue(lock=true)} 注解在
 * 普通 {@link TagRunnable} 上实现相同的分布式锁效果。</p>
 *
 * <h3>时间窗口</h3>
 * <p>通过 {@link TagValue#time()} 配置锁的租约时间（单位：分钟，默认10分钟）。
 * 在此时间窗口内，无论有多少个节点/多少次调度触发，任务最多执行一次。</p>
 *
 * <h3>用法示例</h3>
 * <pre>
 * asyncTaskExecutor.execute(new LockOnce("unique-task-id") {
 *     &#64;Override
 *     &#64;TagValue(method = "dailyCleanup", type = MyService.class, tag = "每日清理", time = 60, delay = 10)
 *     public void exe() {
 *         // 业务逻辑，保证60分钟内只执行一次，各节点错开10秒内随机启动
 *     }
 * });
 * </pre>
 */
public abstract class LockOnce extends TagRunnable {

    private static final Logger log = LoggerFactory.getLogger(LockOnce.class);

    public LockOnce() {
    }

    public LockOnce(String id) {
        super(id);
    }

    // ======================== 核心执行流程 ========================

    @Override
    public void run() {
        bindThread();
        try {
            // 1. 检查系统就绪状态
            if (!can())
                return;
            // 2. 错峰延迟 — 在获取锁之前延迟，减少多节点同时争抢 Redis 锁的压力
            if (!applyStaggerDelay())
                return;
            // 3. 获取 Redisson 客户端并执行
            TagValue value = getTagValue();
            RedissonClient client = DistributedLockHelper.getRedissonClient();
            if (client != null && value != null) {
                executeWithDistributedLock(client, value);
            } else if (client == null) {
                onRedisUnavailable();
            } else {
                // TagValue 为空，无法构建锁键，直接执行
                executeDirectly();
            }
        } catch (Throwable t) {
            if (log.isDebugEnabled())
                log.debug("分布式锁任务发生不可预期错误: tag={}, method={}, error={}", getTag(), getMethod(), t.getMessage(), t);
            try {
                TagTaskExecutor.recordCompletion(this, 0, false, "Unexpected: " + t.getMessage());
            } catch (Exception re) {
                // 忽略
            }
        } finally {
            clearThread();
            TagTaskExecutor.remove(this);
        }
    }

    /**
     * 使用 Redisson 分布式锁执行任务
     */
    private void executeWithDistributedLock(RedissonClient client, TagValue value) {
        String lockKey = DistributedLockHelper.buildLockKey(value, getClass());
        long leaseMinutes = Math.max(1, value.time());
        RLock lock = client.getLock(lockKey);
        boolean acquired = false;
        try {
            acquired = lock.tryLock(0, leaseMinutes, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            if (log.isDebugEnabled())
                log.debug("获取分布式锁被中断: key={}, tag={}", lockKey, safeTag(value));
            clearThread();
            TagTaskExecutor.remove(this);
            return;
        } catch (Exception e) {
            if (log.isDebugEnabled())
                log.debug("获取分布式锁异常: key={}, error={}", lockKey, e.getMessage(), e);
            clearThread();
            TagTaskExecutor.remove(this);
            return;
        }
        if (!acquired) {
            if (log.isDebugEnabled()) {
                log.debug("分布式锁未获取（其他节点执行中）: key={}, tag={}", lockKey, safeTag(value));
            }
            clearThread();
            TagTaskExecutor.remove(this);
            return;
        }
        // ---- 锁已获取，执行任务 ----
        long start = System.currentTimeMillis();
        boolean success = true;
        String errorMsg = null;
        try {
            if (log.isDebugEnabled())
                log.debug("分布式锁已获取，开始执行: key={}, tag={}, lease={}min", lockKey, safeTag(value), leaseMinutes);
            exe();
            if (log.isDebugEnabled())
                log.debug("任务执行成功: key={}, tag={}, 耗时={}ms", lockKey, safeTag(value), System.currentTimeMillis() - start);
        } catch (Throwable t) {
            success = false;
            if (isCancelled() || Thread.currentThread().isInterrupted()) {
                errorMsg = "任务被中断取消";
                if (log.isDebugEnabled())
                    log.debug("分布式锁任务被中断: key={}, tag={}", lockKey, safeTag(value));
            } else {
                errorMsg = t.getMessage();
                if (log.isDebugEnabled())
                    log.debug("任务执行失败: key={}, tag={}, error={}", lockKey, safeTag(value), t.getMessage(), t);
            }
            // 失败后主动释放锁，允许其他节点故障转移重试
            DistributedLockHelper.unlockSafely(lock, lockKey);
        } finally {
            long duration = System.currentTimeMillis() - start;
            TagTaskExecutor.recordCompletion(this, duration, success, errorMsg);
            clearThread();
            TagTaskExecutor.remove(this);
        }
    }

    /**
     * 无分布式锁的直接执行（TagValue 为空时的降级路径）
     */
    private void executeDirectly() {
        long start = System.currentTimeMillis();
        boolean success = true;
        String errorMsg = null;

        try {
            exe();
        } catch (Throwable t) {
            success = false;
            if (isCancelled() || Thread.currentThread().isInterrupted()) {
                errorMsg = "任务被中断取消";
            } else {
                errorMsg = t.getMessage();
                if (log.isDebugEnabled())
                    log.debug("任务直接执行失败: tag={}, error={}", getTag(), t.getMessage(), t);
            }
        } finally {
            long duration = System.currentTimeMillis() - start;
            TagTaskExecutor.recordCompletion(this, duration, success, errorMsg);
            clearThread();
            TagTaskExecutor.remove(this);
        }
    }

    // ======================== 工具方法 ========================

    /**
     * 安全获取 TagValue.tag()，避免 null
     */
    private String safeTag(TagValue value) {
        if (value == null) return getTag();
        String t = value.tag();
        return !t.isEmpty() ? t : getTag();
    }

    /**
     * Redis 不可用时的处理策略。
     * <p>默认行为：跳过执行并记录警告日志（保证多节点不重复执行）。</p>
     * <p>子类可覆写此方法以自定义行为，例如在单节点部署时选择直接执行。</p>
     */
    protected void onRedisUnavailable() {
        if (log.isDebugEnabled())
            log.debug("RedissonClient 不可用，跳过分布式任务以避免多节点重复执行: tag={}, method={}", getTag(), getMethod());
    }
}
