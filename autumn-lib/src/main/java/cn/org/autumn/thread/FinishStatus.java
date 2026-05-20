package cn.org.autumn.thread;

/**
 * {@link TagRunnable} / {@link LockOnce} 任务在 {@link TagRunnable#onFinished(FinishStatus)} 中的结束原因。
 */
public enum FinishStatus {

    /**
     * 任务已提交并进入 {@code run()}，但 {@link TagRunnable#exe()} 未被调用
     *（系统未就绪、错峰中断、框架分布式锁未获取等）。
     */
    SKIPPED,

    /**
     * {@link TagRunnable#exe()} 已执行且正常返回。
     */
    COMPLETED,

    /**
     * {@link TagRunnable#exe()} 已执行但抛出异常（含中断取消）。
     */
    FAILED,

    /**
     * {@link TagTaskExecutor#execute(TagRunnable)} 因 id 去重等原因未将任务提交到线程池。
     */
    NOT_DISPATCHED
}
