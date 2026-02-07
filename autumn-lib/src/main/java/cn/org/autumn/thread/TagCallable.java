package cn.org.autumn.thread;

import cn.org.autumn.config.Config;
import cn.org.autumn.site.UpgradeFactory;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.lang.reflect.Method;
import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * 可追踪、可管理的异步 Callable 任务基类。
 * <p>功能与 {@link TagRunnable} 完全对齐，支持线程绑定、元数据解析、错峰延迟、协作式取消、超时检测和 null 安全。</p>
 *
 * @see TagRunnable
 */
public abstract class TagCallable<V> implements Callable<V>, Tag {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    // ======================== 任务元数据 ========================

    private String name = "";
    private Date time = new Date();
    private String tag = "";
    private String id = "";
    private String method = "";
    private Class<?> type = null;
    private TagValue tagValue = null;
    private volatile boolean tagValueResolved = false;

    // ======================== 运行时状态 ========================

    private volatile Thread executionThread;
    private volatile boolean cancelled = false;
    private long timeout = 0;
    private long delay = 0;
    private volatile boolean delaying = false;
    private boolean lock = false;

    // ======================== 系统依赖 ========================

    static UpgradeFactory upgradeFactory;

    // ======================== 构造器 ========================

    public TagCallable() {
    }

    public TagCallable(String id) {
        this.id = id != null ? id : "";
    }

    // ======================== 核心执行流程 ========================

    @Override
    public V call() throws Exception {
        // 绑定执行线程 + 解析元数据
        this.executionThread = Thread.currentThread();
        this.name = Thread.currentThread().getName();
        resolveTagValueOnce();
        try {
            // 1. 检查系统就绪状态
            if (!can()) {
                TagTaskExecutor.recordSkipped(this, "系统未就绪");
                return null;
            }
            // 2. 错峰延迟
            if (!applyStaggerDelay()) {
                TagTaskExecutor.recordSkipped(this, "错峰延迟被中断");
                return null;
            }
            // 3. 分支：需要分布式锁 vs 直接执行
            if (this.lock) {
                return callWithDistributedLock();
            } else {
                return callDirect();
            }
        } catch (Throwable t) {
            // 安全网：捕获所有未被内部方法处理的意外错误
            if (log.isDebugEnabled())
                log.debug("任务发生不可预期错误: tag={}, method={}, error={}", getTag(), getMethod(), t.getMessage(), t);
            try {
                TagTaskExecutor.recordCompletion(this, 0, false, "Unexpected: " + t.getMessage());
            } catch (Exception re) {
                // 忽略
            }
            return null;
        } finally {
            // 终极清理保障
            this.executionThread = null;
            TagTaskExecutor.remove(this);
        }
    }

    public V exe() throws Exception {
        if (log.isDebugEnabled()) {
            log.debug("执行任务: tag={}, time={}, thread={}", getTag(), time, getName());
        }
        return null;
    }

    // ======================== 直接执行 ========================

    private V callDirect() throws Exception {
        long start = System.currentTimeMillis();
        boolean success = true;
        String errorMsg = null;

        try {
            return exe();
        } catch (Throwable t) {
            success = false;
            if (cancelled || Thread.currentThread().isInterrupted()) {
                errorMsg = "任务被中断取消";
                if (log.isDebugEnabled())
                    log.debug("任务被中断: tag={}, method={}", getTag(), getMethod());
            } else {
                errorMsg = t.getMessage();
                if (log.isDebugEnabled())
                    log.debug("任务执行异常: tag={}, method={}, error={}", getTag(), getMethod(), t.getMessage(), t);
            }
            return null;
        } finally {
            long duration = System.currentTimeMillis() - start;
            if (log.isDebugEnabled()) {
                log.debug("任务完成: tag={}, method={}, success={}, 耗时={}ms", getTag(), getMethod(), success, duration);
            }
            TagTaskExecutor.recordCompletion(this, duration, success, errorMsg);
            this.executionThread = null;
            TagTaskExecutor.remove(this);
        }
    }

    // ======================== 分布式锁执行 ========================

    /**
     * 使用 Redisson 分布式锁执行任务（行为与 TagRunnable.runWithDistributedLock() 一致）。
     */
    private V callWithDistributedLock() throws Exception {
        RedissonClient client = DistributedLockHelper.getRedissonClient();
        if (client == null) {
            if (log.isDebugEnabled())
                log.debug("RedissonClient 不可用，跳过需要分布式锁的任务: tag={}, method={}", getTag(), getMethod());
            TagTaskExecutor.recordSkipped(this, "Redis不可用");
            return null;
        }

        TagValue value = getTagValue();
        String lockKey = DistributedLockHelper.buildLockKey(value, getClass());
        long leaseSeconds = (value != null) ? Math.max(1, value.time()) : 10;
        RLock rlock = client.getLock(lockKey);

        boolean acquired = false;
        try {
            acquired = rlock.tryLock(0, leaseSeconds, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            if (log.isDebugEnabled())
                log.debug("获取分布式锁被中断: key={}, tag={}", lockKey, getTag());
            TagTaskExecutor.recordSkipped(this, "获取锁被中断");
            return null;
        } catch (Exception e) {
            if (log.isDebugEnabled())
                log.debug("获取分布式锁异常: key={}, error={}", lockKey, e.getMessage(), e);
            TagTaskExecutor.recordSkipped(this, "获取锁异常: " + e.getMessage());
            return null;
        }

        if (!acquired) {
            if (log.isDebugEnabled()) {
                log.debug("分布式锁未获取（其他节点执行中）: key={}, tag={}", lockKey, getTag());
            }
            TagTaskExecutor.recordSkipped(this, "未获取到锁(其他节点执行中)");
            return null;
        }
        long start = System.currentTimeMillis();
        boolean success = true;
        String errorMsg = null;
        try {
            if (log.isDebugEnabled())
                log.debug("分布式锁已获取，开始执行: key={}, tag={}, lease={}s", lockKey, getTag(), leaseSeconds);
            V result = exe();
            if (log.isDebugEnabled())
                log.debug("任务执行成功: key={}, tag={}, 耗时={}ms", lockKey, getTag(), System.currentTimeMillis() - start);
            return result;
        } catch (Throwable t) {
            success = false;
            if (cancelled || Thread.currentThread().isInterrupted()) {
                errorMsg = "任务被中断取消";
                if (log.isDebugEnabled())
                    log.debug("分布式锁任务被中断: key={}, tag={}", lockKey, getTag());
            } else {
                errorMsg = t.getMessage();
                if (log.isDebugEnabled())
                    log.debug("任务执行失败: key={}, tag={}, error={}", lockKey, getTag(), t.getMessage(), t);
            }
            DistributedLockHelper.unlockSafely(rlock, lockKey);
            return null;
        } finally {
            long duration = System.currentTimeMillis() - start;
            TagTaskExecutor.recordCompletion(this, duration, success, errorMsg);
            this.executionThread = null;
            TagTaskExecutor.remove(this);
        }
    }

    // ======================== 错峰延迟 ========================

    /**
     * 应用错峰延迟。
     *
     * @return true 延迟完成，可以继续执行；false 延迟被中断，应退出
     */
    private boolean applyStaggerDelay() {
        long effectiveDelay = getEffectiveDelay();
        if (effectiveDelay <= 0) return true;
        long delayMs = ThreadLocalRandom.current().nextLong(effectiveDelay * 1000L);
        if (delayMs <= 0) return true;
        this.delaying = true;
        try {
            if (log.isDebugEnabled()) {
                log.debug("错峰延迟: tag={}, method={}, 延迟={}ms, 窗口={}s", getTag(), getMethod(), delayMs, effectiveDelay);
            }
            Thread.sleep(delayMs);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            if (log.isDebugEnabled())
                log.debug("错峰延迟被中断: tag={}, method={}", getTag(), getMethod());
            return false;
        } finally {
            this.delaying = false;
        }
    }

    private long getEffectiveDelay() {
        if (this.delay > 0) return this.delay;
        return TagTaskExecutor.getGlobalStaggerSeconds();
    }

    // ======================== @TagValue 解析 ========================

    private void resolveTagValueOnce() {
        if (tagValueResolved) return;
        tagValueResolved = true;

        TagValue value = getTagValue();
        if (value == null) return;

        if (StringUtils.isNotBlank(value.name()))
            this.name = value.name();
        if (StringUtils.isNotBlank(value.tag()))
            this.tag = value.tag();
        if (StringUtils.isNotBlank(value.method()))
            this.method = value.method();
        if (value.type() != String.class)
            this.type = value.type();
        this.timeout = Math.max(0, value.timeout());
        this.delay = Math.max(0, value.delay());
        this.lock = value.lock();
    }

    public TagValue getTagValue() {
        if (null != tagValue)
            return tagValue;
        try {
            Method mt = getClass().getDeclaredMethod("exe");
            tagValue = mt.getDeclaredAnnotation(TagValue.class);
            return tagValue;
        } catch (Exception e) {
            return null;
        }
    }

    // ======================== Tag 接口实现: 任务标识（全部 null 安全） ========================

    @Override
    public String getId() {
        return id != null ? id : "";
    }

    public void setId(String id) {
        this.id = id != null ? id : "";
    }

    @Override
    public String getName() {
        if (StringUtils.isNotBlank(name)) {
            return name;
        }
        TagValue t = getTagValue();
        if (t != null && StringUtils.isNotBlank(t.name())) {
            return t.name();
        }
        Thread thread = this.executionThread;
        if (thread != null) {
            return thread.getName();
        }
        return getClass().getSimpleName();
    }

    @Override
    public void setName(String name) {
        this.name = name != null ? name : "";
    }

    @Override
    public Date getTime() {
        return time;
    }

    @Override
    public void setTime(Date time) {
        if (time != null) {
            this.time = time;
        }
    }

    @Override
    public String getTag() {
        if (StringUtils.isNotBlank(tag)) {
            return tag;
        }
        TagValue t = getTagValue();
        if (t != null && StringUtils.isNotBlank(t.tag())) {
            return t.tag();
        }
        return "";
    }

    @Override
    public void setTag(String tag) {
        this.tag = tag != null ? tag : "";
    }

    @Override
    public String getMethod() {
        if (StringUtils.isNotBlank(method)) {
            return method;
        }
        TagValue t = getTagValue();
        if (t != null && StringUtils.isNotBlank(t.method())) {
            return t.method();
        }
        return "";
    }

    @Override
    public void setMethod(String method) {
        this.method = method != null ? method : "";
    }

    @Override
    public Class<?> getType() {
        if (type != null) {
            return type;
        }
        TagValue t = getTagValue();
        if (t != null) {
            return t.type();
        }
        return String.class;
    }

    @Override
    public void setType(Class<?> type) {
        this.type = type;
    }

    // ======================== Tag 接口实现: 运行时管理 ========================

    @Override
    public Thread getThread() {
        return executionThread;
    }

    @Override
    public void setThread(Thread thread) {
        this.executionThread = thread;
    }

    @Override
    public boolean isCancelled() {
        if (cancelled) return true;
        Thread t = this.executionThread;
        return t != null && t.isInterrupted();
    }

    @Override
    public boolean cancel() {
        this.cancelled = true;
        Thread t = this.executionThread;
        if (t != null && t.isAlive()) {
            t.interrupt();
            if (log.isDebugEnabled())
                log.debug("已发送中断信号: tag={}, method={}, thread={}, delaying={}", getTag(), getMethod(), t.getName(), delaying);
            return true;
        }
        return false;
    }

    @Override
    public String getThreadState() {
        if (delaying) return "DELAYING";
        Thread t = this.executionThread;
        if (t == null) return "NOT_STARTED";
        try {
            return t.getState().name();
        } catch (Exception e) {
            return "UNKNOWN";
        }
    }

    @Override
    public String getStackTrace(int maxDepth) {
        Thread t = this.executionThread;
        if (t == null || !t.isAlive()) return "";
        if (maxDepth <= 0) return "";

        try {
            StackTraceElement[] stack = t.getStackTrace();
            if (stack == null || stack.length == 0) return "";

            StringBuilder sb = new StringBuilder();
            int depth = Math.min(maxDepth, stack.length);
            for (int i = 0; i < depth; i++) {
                if (i > 0) sb.append("\n");
                sb.append("  at ").append(stack[i].toString());
            }
            if (stack.length > depth) {
                sb.append("\n  ... ").append(stack.length - depth).append(" more");
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    @Override
    public long getTimeout() {
        return timeout;
    }

    @Override
    public boolean isTimeout() {
        if (timeout <= 0) return false;
        if (delaying) return false;
        Date t = this.time;
        if (t == null) return false;
        long elapsed = System.currentTimeMillis() - t.getTime();
        return elapsed > timeout * 1000L;
    }

    @Override
    public boolean isLocked() {
        return lock;
    }

    @Override
    public boolean isDelaying() {
        return delaying;
    }

    @Override
    public long getDelay() {
        long effective = this.delay;
        if (effective <= 0) {
            effective = TagTaskExecutor.getGlobalStaggerSeconds();
        }
        return Math.max(0, effective);
    }

    // ======================== 系统状态检查 ========================

    public boolean can() {
        if (null == upgradeFactory) {
            try {
                upgradeFactory = (UpgradeFactory) Config.getBean(UpgradeFactory.class);
            } catch (Exception e) {
                // Bean 未就绪
            }
        }
        return null == upgradeFactory || upgradeFactory.isDone();
    }
}
