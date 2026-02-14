package cn.org.autumn.thread;

import java.util.Date;

/**
 * 线程任务标识接口 — 所有可追踪、可管理的任务实体必须实现此接口。
 * <p>
 * 提供任务元数据（名称、标记、来源）和运行时管理（线程引用、取消、堆栈诊断、错峰延迟）两类能力。
 * </p>
 */
public interface Tag {

    // ======================== 任务标识 ========================

    String getId();

    String getName();

    void setName(String name);

    Date getTime();

    void setTime(Date time);

    String getTag();

    void setTag(String tag);

    String getMethod();

    void setMethod(String method);

    Class<?> getType();

    void setType(Class<?> type);

    // ======================== 运行时管理 ========================

    /**
     * 获取任务实际执行所在的线程。
     * <p>在 {@code run()/call()} 启动前返回 null，启动后返回执行线程。</p>
     */
    Thread getThread();

    /**
     * 绑定执行线程（由框架在 run/call 入口自动调用）
     */
    void setThread(Thread thread);

    /**
     * 任务是否已被取消/中断
     */
    boolean isCancelled();

    /**
     * 请求取消任务。
     * <p>实现应设置取消标志并中断执行线程（{@link Thread#interrupt()}）。
     * 任务的 {@code exe()} 实现应检查中断状态以实现协作式取消。</p>
     *
     * @return true 如果成功发出中断信号
     */
    boolean cancel();

    /**
     * 获取执行线程的当前状态描述
     *
     * @return 线程状态字符串，如 "RUNNABLE"、"WAITING"、"BLOCKED" 等；线程不存在时返回 "NOT_STARTED"
     */
    String getThreadState();

    /**
     * 获取执行线程的当前堆栈摘要（用于诊断卡死/死锁）
     *
     * @param maxDepth 最大堆栈深度
     * @return 堆栈字符串，线程不存在时返回空字符串
     */
    String getStackTrace(int maxDepth);

    /**
     * 获取 @TagValue 注解配置的超时时间（秒）
     *
     * @return 超时秒数，0 表示不限制
     */
    long getTimeout();

    /**
     * 判断任务是否已超时
     */
    boolean isTimeout();

    // ======================== 分布式锁 ========================

    /**
     * 任务是否配置了分布式锁（{@code @TagValue(lock=true)} 或使用 {@link LockOnce}）。
     *
     * @return true 表示任务需要/正在使用分布式锁
     */
    boolean isLocked();

    /**
     * 获取分布式锁的租约时间（秒）。
     * <p>
     * 在此时间窗口内，无论有多少个节点或多少次调度触发，任务最多执行一次。
     * 仅在 {@link #isLocked()} 为 true 时有意义。
     * </p>
     *
     * @return 锁租约秒数，默认 0（未配置锁时）
     * @see TagValue#time()
     */
    long getLockLeaseTime();

    // ======================== 错峰延迟 ========================

    /**
     * 任务当前是否正处于错峰延迟等待中（尚未开始执行业务逻辑）。
     * <p>用于管理界面区分"延迟等待"和"实际执行"两种状态。</p>
     *
     * @return true 如果任务正在延迟等待阶段
     */
    boolean isDelaying();

    /**
     * 获取此任务配置的错峰延迟窗口（秒），优先取 @TagValue.delay()，其次取全局配置。
     *
     * @return 延迟窗口秒数，0 = 不延迟
     */
    long getDelay();
}
