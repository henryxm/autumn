package cn.org.autumn.thread;

import java.lang.annotation.*;

/**
 * 线程任务元数据注解 — 标记在 {@code exe()} 方法上，描述任务的来源、用途和执行约束。
 *
 * <h3>用法示例</h3>
 * <pre>
 * // 普通任务
 * asyncTaskExecutor.execute(new TagRunnable() {
 *     &#64;Override
 *     &#64;TagValue(
 *         name    = "数据库备份任务",
 *         tag     = "数据库备份",
 *         type    = DatabaseBackupService.class,
 *         method  = "backupAsync",
 *         timeout = 3600,  // 最大允许执行1小时
 *         delay   = 30     // 错峰：随机延迟0~30秒后执行
 *     )
 *     public void exe() {
 *         // 业务逻辑
 *     }
 * });
 *
 * // 声明式分布式锁（无需继承 LockOnce）
 * asyncTaskExecutor.execute(new TagRunnable("task-id") {
 *     &#64;Override
 *     &#64;TagValue(lock = true, time = 60, tag = "每日清理", type = MyService.class, method = "dailyCleanup")
 *     public void exe() {
 *         // 自动获取分布式锁，60分钟内只执行一次
 *     }
 * });
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface TagValue {
    /**
     * 任务调用来源类
     */
    Class<?> type() default String.class;

    /**
     * 调用方法名
     */
    String method() default "";

    /**
     * 任务标记/描述（用于日志和管理界面显示）
     */
    String tag() default "";

    /**
     * 任务名称
     */
    String name() default "";

    /**
     * 是否需要分布式锁。
     *
     * <h4>声明式分布式锁</h4>
     * <p>设为 {@code true} 时，任务执行前自动通过 Redisson 获取分布式锁，
     * 保证多节点并行部署环境下，同一任务在 {@link #time()} 指定的时间窗口内只被执行一次。</p>
     *
     * <h4>执行策略</h4>
     * <ul>
     *   <li>使用 {@code tryLock(0, leaseTime, MINUTES)} — 非阻塞获取 + 自动过期</li>
     *   <li>执行<b>成功不释放锁</b>（防止时间窗口内重复执行）</li>
     *   <li>执行<b>失败主动释放锁</b>（允许其他节点故障转移重试）</li>
     *   <li>Redis 不可用时跳过执行</li>
     * </ul>
     *
     * <h4>适用范围</h4>
     * <p>适用于 {@link TagRunnable}、{@link TagCallable}，以及 {@link LockOnce}。
     * 如果需要自定义 Redis 不可用时的行为（如降级为本地执行），请使用 {@link LockOnce} 并覆写
     * {@link LockOnce#onRedisUnavailable()} 方法。</p>
     *
     * <h4>锁键格式</h4>
     * <p>{@code loopjob:lock:{type.simpleName}:{method}} — 由 {@link #type()} 和 {@link #method()} 决定。</p>
     */
    boolean lock() default false;

    /**
     * 锁的租约时间，单位：分钟。仅在 {@link #lock()}{@code = true} 或 {@link LockOnce} 中生效。
     * <p>在此时间窗口内，无论有多少个节点或多少次调度触发，任务最多执行一次。</p>
     */
    long time() default 10;

    /**
     * 任务最大执行时间，单位：秒。超过此时间视为超时，管理界面将标记警告。
     * <ul>
     *   <li>0 = 不限制（默认）</li>
     *   <li>&gt;0 = 超时阈值（秒）</li>
     * </ul>
     */
    long timeout() default 0;

    /**
     * 错峰延迟窗口，单位：秒。任务执行前随机延迟 [0, delay) 秒，避免整点资源争抢。
     *
     * <h4>使用场景</h4>
     * <p>定时任务在整点（如每小时、每天0点）同时触发时，大量任务同时启动会导致
     * CPU 和网络资源的瞬间拥挤。配置此参数后，每个任务会在 [0, delay) 秒内
     * 随机延迟启动，自然将负载分散到时间窗口内。</p>
     *
     * <h4>优先级</h4>
     * <ul>
     *   <li>此注解值 &gt; 0 时，使用此值作为延迟窗口</li>
     *   <li>此注解值 = 0（默认）时，使用 {@link TagTaskExecutor} 的全局错峰配置</li>
     *   <li>全局配置也为 0 时，不延迟</li>
     * </ul>
     *
     * <h4>特性</h4>
     * <ul>
     *   <li>延迟期间可被 {@code cancel()} 中断</li>
     *   <li>管理界面会显示"延迟等待中"状态</li>
     * </ul>
     */
    long delay() default 0;
}
