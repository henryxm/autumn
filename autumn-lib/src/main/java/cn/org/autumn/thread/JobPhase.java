package cn.org.autumn.thread;

/**
 * 本机异步调度相位（单飞闸门 / 内存队列 drain）。
 * <p>
 * 表示「是否已向 {@link TagTaskExecutor} 提交任务」，<b>不是</b>跨节点分布式锁。
 * 须在 {@link TagRunnable#onFinished(FinishStatus)} 中置回 {@link #IDLE}，
 * 禁止仅在 {@code exe()} 的 {@code finally} 释放（见 {@code docs/AI_ASYNC_TASK.md}）。
 * </p>
 * <p>
 * 业务侧请使用本枚举，勿再在各 Service 内重复定义同名私有 enum。
 * 配套工具见 {@link JobPhaseGate}。
 * </p>
 */
public enum JobPhase {

    /**
     * 无进行中的本机调度（可再次 CAS 提交）。
     */
    IDLE,

    /**
     * 已提交异步任务，尚未在 {@link TagRunnable#onFinished(FinishStatus)} 中收尾。
     */
    DISPATCHING
}
