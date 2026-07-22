package cn.org.autumn.thread;

/**
 * 函数队列溢出策略。
 */
public enum OverflowPolicy {
    /** 队列满时拒绝新任务 */
    REJECT,
    /** 队列满时丢弃最旧待执行任务再入队 */
    DROP_OLDEST
}
