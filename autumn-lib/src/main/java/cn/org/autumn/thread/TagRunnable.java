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
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * 可追踪、可管理的异步任务基类。
 *
 * <h3>核心能力</h3>
 * <ul>
 *   <li><b>线程绑定</b>：{@code run()} 执行时自动绑定当前线程引用，支持外部查询线程状态和堆栈</li>
 *   <li><b>元数据自解析</b>：启动时自动从 {@link TagValue} 注解解析任务名称、标记、来源等信息</li>
 *   <li><b>错峰延迟</b>：通过 {@link TagValue#delay()} 或全局配置，任务执行前随机延迟，避免整点资源争抢</li>
 *   <li><b>声明式分布式锁</b>：通过 {@link TagValue#lock()}{@code = true} 自动获取 Redisson 分布式锁，
 *       无需继承 {@link LockOnce}；租约时间由 {@link TagValue#time()} 配置</li>
 *   <li><b>协作式取消</b>：{@link #cancel()} 设置中断标志并 interrupt 执行线程；
 *       长耗时业务应在循环中检查 {@link #isCancelled()} 或 {@code Thread.interrupted()}</li>
 *   <li><b>超时检测</b>：通过 {@link TagValue#timeout()} 配置阈值，管理界面可识别超时任务</li>
 *   <li><b>执行记录</b>：自动向 {@link TagTaskExecutor} 上报完成/失败/中断统计</li>
 *   <li><b>全面 null 安全</b>：所有 getter 方法保证不返回 null，返回空字符串或默认值</li>
 * </ul>
 *
 * <h3>用法示例</h3>
 * <pre>
 * asyncTaskExecutor.execute(new TagRunnable("unique-id") {
 *     &#64;Override
 *     &#64;TagValue(tag = "批量导入", type = MyService.class, method = "batchImport", timeout = 600, delay = 30)
 *     public void exe() {
 *         for (Item item : items) {
 *             if (isCancelled()) {
 *                 log.info("任务被取消，已处理 {} 条", count);
 *                 return;
 *             }
 *             process(item);
 *         }
 *     }
 * });
 * </pre>
 */
public abstract class TagRunnable implements Runnable, Tag {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    // ======================== 任务元数据 ========================
    private String name = "";
    private Date time = new Date();
    private String tag = "";
    private String id = "";
    private String method = "";
    private Class<?> type = null;
    private TagValue tagValue = null;

    /**
     * 标记 @TagValue 是否已解析（仅解析一次）
     */
    private volatile boolean tagValueResolved = false;

    // ======================== 运行时状态 ========================

    /**
     * 实际执行此任务的线程（run() 入口时绑定）
     */
    private volatile Thread executionThread;

    /**
     * 取消标志（volatile 保证可见性）
     */
    private volatile boolean cancelled = false;

    /**
     * 超时阈值（秒），0 = 不限制。从 @TagValue.timeout() 解析。
     */
    private long timeout = 0;

    /**
     * 错峰延迟窗口（秒），0 = 不延迟。从 @TagValue.delay() 或全局配置解析。
     */
    private long delay = 0;

    /**
     * 是否需要分布式锁。从 @TagValue.lock() 解析。
     * <p>为 true 时，exe() 执行前自动获取 Redisson 分布式锁。</p>
     */
    private boolean lock = false;

    /**
     * 是否正处于错峰延迟等待中
     */
    private volatile boolean delaying = false;

    // ======================== 系统依赖 ========================

    static UpgradeFactory upgradeFactory;

    // ======================== 构造器 ========================

    public TagRunnable() {
    }

    public TagRunnable(String id) {
        this.id = id != null ? id : "";
    }

    // ======================== 核心执行流程 ========================

    @Override
    public void run() {
        // 绑定执行线程 + 解析元数据（必须在 try 块之前，确保 clearThread/remove 能正确识别任务）
        bindThread();
        try {
            // 1. 检查系统就绪状态
            if (!can())
                return;
            // 2. 错峰延迟
            if (!applyStaggerDelay())
                return;
            // 3. 分支：需要分布式锁 vs 直接执行
            if (this.lock) {
                runWithDistributedLock();
            } else {
                runDirect();
            }
        } catch (Throwable t) {
            if (log.isDebugEnabled())
                // 安全网：捕获所有未被内部方法处理的意外错误（如 Error、NoClassDefFoundError 等）
                log.debug("任务发生不可预期错误: tag={}, method={}, error={}", getTag(), getMethod(), t.getMessage(), t);
            try {
                TagTaskExecutor.recordCompletion(this, 0, false, "Unexpected: " + t.getMessage());
            } catch (Exception re) {
                // 忽略记录失败
            }
        } finally {
            // 终极清理保障：无论任何代码路径，都确保线程引用和 running 列表被清理
            // clearThread() 和 remove() 均为幂等操作，重复调用无害
            clearThread();
            TagTaskExecutor.remove(this);
        }
    }

    /**
     * 任务业务逻辑入口 — 子类必须覆写此方法。
     * <p>长耗时任务应在循环中检查 {@link #isCancelled()}，以支持协作式取消。</p>
     */
    public void exe() {
        if (log.isDebugEnabled()) {
            log.debug("执行任务: tag={}, time={}, thread={}", getTag(), time, getName());
        }
    }

    // ======================== 直接执行 ========================

    /**
     * 无分布式锁的直接执行路径。
     */
    private void runDirect() {
        long start = System.currentTimeMillis();
        boolean success = true;
        String errorMsg = null;
        try {
            exe();
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
        } finally {
            long duration = System.currentTimeMillis() - start;
            if (log.isDebugEnabled()) {
                log.debug("任务完成: tag={}, method={}, success={}, 耗时={}ms, 线程={}", getTag(), getMethod(), success, duration, getName());
            }
            TagTaskExecutor.recordCompletion(this, duration, success, errorMsg);
            clearThread();
            TagTaskExecutor.remove(this);
        }
    }

    // ======================== 分布式锁执行 ========================

    /**
     * 使用 Redisson 分布式锁执行任务。
     * <p>当 {@code @TagValue(lock=true)} 时自动调用。行为与 {@link LockOnce} 一致：</p>
     * <ul>
     *   <li>成功执行后<b>不释放锁</b>（防止时间窗口内重复执行）</li>
     *   <li>执行失败后<b>主动释放锁</b>（允许其他节点故障转移重试）</li>
     *   <li>Redis 不可用时跳过执行</li>
     * </ul>
     */
    private void runWithDistributedLock() {
        RedissonClient client = DistributedLockHelper.getRedissonClient();
        if (client == null) {
            if (log.isDebugEnabled())
                log.warn("RedissonClient 不可用，跳过需要分布式锁的任务: tag={}, method={}", getTag(), getMethod());
            clearThread();
            TagTaskExecutor.remove(this);
            return;
        }
        TagValue value = getTagValue();
        String lockKey = DistributedLockHelper.buildLockKey(value, getClass());
        long leaseMinutes = (value != null) ? Math.max(1, value.time()) : 10;
        RLock rlock = client.getLock(lockKey);
        boolean acquired;
        try {
            acquired = rlock.tryLock(0, leaseMinutes, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            if (log.isDebugEnabled())
                log.warn("获取分布式锁被中断: key={}, tag={}", lockKey, getTag());
            clearThread();
            TagTaskExecutor.remove(this);
            return;
        } catch (Exception e) {
            if (log.isDebugEnabled())
                log.error("获取分布式锁异常: key={}, error={}", lockKey, e.getMessage(), e);
            clearThread();
            TagTaskExecutor.remove(this);
            return;
        }
        if (!acquired) {
            if (log.isDebugEnabled()) {
                log.debug("分布式锁未获取（其他节点执行中）: key={}, tag={}", lockKey, getTag());
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
                log.info("分布式锁已获取，开始执行: key={}, tag={}, lease={}min", lockKey, getTag(), leaseMinutes);
            exe();
            if (log.isDebugEnabled())
                log.info("任务执行成功: key={}, tag={}, 耗时={}ms", lockKey, getTag(), System.currentTimeMillis() - start);
            // 成功后不释放锁 — 让锁自然过期，防止同一时间窗口内重复执行
        } catch (Throwable t) {
            success = false;
            if (cancelled || Thread.currentThread().isInterrupted()) {
                errorMsg = "任务被中断取消";
                if (log.isDebugEnabled())
                    log.info("分布式锁任务被中断: key={}, tag={}", lockKey, getTag());
            } else {
                errorMsg = t.getMessage();
                if (log.isDebugEnabled())
                    log.error("任务执行失败: key={}, tag={}, error={}", lockKey, getTag(), t.getMessage(), t);
            }
            // 失败后主动释放锁 — 允许其他节点故障转移重试
            DistributedLockHelper.unlockSafely(rlock, lockKey);
        } finally {
            long duration = System.currentTimeMillis() - start;
            TagTaskExecutor.recordCompletion(this, duration, success, errorMsg);
            clearThread();
            TagTaskExecutor.remove(this);
        }
    }

    // ======================== 线程绑定（子类如 LockOnce 也需调用） ========================

    /**
     * 绑定当前线程并解析 @TagValue 元数据。
     * <p>子类覆写 {@code run()} 时必须在入口调用此方法，否则线程管理功能（cancel/interrupt/stacktrace）将失效。</p>
     */
    protected void bindThread() {
        this.executionThread = Thread.currentThread();
        this.name = Thread.currentThread().getName();
        resolveTagValue();
    }

    /**
     * 清除线程引用（任务结束时调用，帮助 GC，防止线程泄漏）。
     */
    protected void clearThread() {
        this.executionThread = null;
    }

    // ======================== 错峰延迟 ========================

    /**
     * 应用错峰延迟。在任务真正执行前，随机等待 [0, effectiveDelay) 秒。
     *
     * <p>延迟优先级：</p>
     * <ol>
     *   <li>@TagValue.delay() &gt; 0 → 使用注解值</li>
     *   <li>否则使用 TagTaskExecutor.globalStaggerSeconds</li>
     * </ol>
     *
     * <p>延迟期间任务状态为 "DELAYING"，可被 cancel() 中断。</p>
     *
     * @return true 延迟完成，可以继续执行；false 延迟被中断，应退出
     */
    protected boolean applyStaggerDelay() {
        long effectiveDelay = getEffectiveDelay();
        if (effectiveDelay <= 0) return true;
        // 生成 [0, effectiveDelay) 秒的随机延迟
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

    /**
     * 获取实际生效的延迟窗口（秒）。优先取 @TagValue，其次取全局配置。
     */
    private long getEffectiveDelay() {
        // 优先使用 @TagValue.delay()
        if (this.delay > 0) return this.delay;
        // 降级到全局配置
        return TagTaskExecutor.getGlobalStaggerSeconds();
    }

    // ======================== @TagValue 元数据解析 ========================

    /**
     * 解析 {@code exe()} 方法上的 {@link TagValue} 注解，自动填充任务元数据。
     * <p>仅在第一次调用时解析，结果缓存。</p>
     */
    private void resolveTagValue() {
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
        // 最终降级：返回线程名或类名
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
        // 防止 null — 如果传入 null，保持当前值
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
        // 最终降级：永不返回 null
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
        // 如果正在延迟等待，返回自定义状态
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
            if (stack.length == 0) return "";
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
        // 延迟等待期间不算超时
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

    /**
     * 检查系统是否已完成启动，未启动完成时不执行定时任务
     */
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
