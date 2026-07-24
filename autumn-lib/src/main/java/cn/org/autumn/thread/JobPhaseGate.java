package cn.org.autumn.thread;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

/**
 * {@link JobPhase} 本机闸门工具：CAS 进入 {@link JobPhase#DISPATCHING}、在 {@code onFinished} 回 {@link JobPhase#IDLE}、
 * 队列 drain 收尾再调度与积压补偿。
 * <p>
 * 典型用法：
 * <pre>
 * private final AtomicReference&lt;JobPhase&gt; phase = JobPhaseGate.create();
 *
 * void schedule() {
 *     if (!JobPhaseGate.tryBegin(phase))
 *         return;
 *     asyncTaskExecutor.execute(new TagRunnable() {
 *         &#64;Override
 *         protected void onFinished(FinishStatus status) {
 *             JobPhaseGate.endAndMaybeReschedule(phase, () -&gt; !queue.isEmpty(), this::schedule);
 *         }
 *         &#64;Override
 *         public void exe() { ... }
 *     });
 * }
 * </pre>
 * </p>
 *
 * @see JobPhase
 * @see TagRunnable#onFinished(FinishStatus)
 */
public final class JobPhaseGate {

    private JobPhaseGate() {
    }

    /**
     * 新建闸门，初始为 {@link JobPhase#IDLE}。
     */
    public static AtomicReference<JobPhase> create() {
        return new AtomicReference<>(JobPhase.IDLE);
    }

    /**
     * 尝试进入调度：{@code IDLE → DISPATCHING}。
     *
     * @return {@code true} 表示抢到闸门，调用方应继续 {@code execute}；{@code false} 表示已有任务在途
     */
    public static boolean tryBegin(AtomicReference<JobPhase> phase) {
        Objects.requireNonNull(phase, "phase");
        return phase.compareAndSet(JobPhase.IDLE, JobPhase.DISPATCHING);
    }

    /**
     * 强制置为 {@link JobPhase#IDLE}（通常仅用于队列为空时的复位）。
     */
    public static void resetIdle(AtomicReference<JobPhase> phase) {
        Objects.requireNonNull(phase, "phase");
        phase.set(JobPhase.IDLE);
    }

    /**
     * 任务结束：置回 {@link JobPhase#IDLE}。应在 {@link TagRunnable#onFinished(FinishStatus)} 中调用。
     */
    public static void end(AtomicReference<JobPhase> phase) {
        resetIdle(phase);
    }

    /**
     * {@link #end(AtomicReference)} 后若仍有待处理工作则再次 {@code schedule}（内存队列 drain 合并单飞）。
     *
     * @param hasPending 是否仍有积压（如 {@code !queue.isEmpty()}）
     * @param schedule   再次调度回调；可为 {@code null}（仅 end）
     */
    public static void endAndMaybeReschedule(AtomicReference<JobPhase> phase, BooleanSupplier hasPending, Runnable schedule) {
        Objects.requireNonNull(phase, "phase");
        phase.set(JobPhase.IDLE);
        if (hasPending != null && hasPending.getAsBoolean() && schedule != null)
            schedule.run();
    }

    /**
     * 同 {@link #endAndMaybeReschedule(AtomicReference, BooleanSupplier, Runnable)}，用 boolean 表达是否积压。
     */
    public static void endAndMaybeReschedule(AtomicReference<JobPhase> phase, boolean hasPending, Runnable schedule) {
        endAndMaybeReschedule(phase, () -> hasPending, schedule);
    }

    /**
     * 补偿：有积压且本机为 {@link JobPhase#IDLE} 时重新发起调度（防 SKIPPED / NOT_DISPATCHED 后无消费者）。
     */
    public static void recoverIfStalled(AtomicReference<JobPhase> phase, BooleanSupplier hasPending, Runnable schedule) {
        Objects.requireNonNull(phase, "phase");
        if (hasPending == null || schedule == null)
            return;
        if (hasPending.getAsBoolean() && phase.get() == JobPhase.IDLE)
            schedule.run();
    }

    /**
     * 同 {@link #recoverIfStalled(AtomicReference, BooleanSupplier, Runnable)}。
     */
    public static void recoverIfStalled(AtomicReference<JobPhase> phase, boolean hasPending, Runnable schedule) {
        recoverIfStalled(phase, () -> hasPending, schedule);
    }

    public static boolean isIdle(AtomicReference<JobPhase> phase) {
        Objects.requireNonNull(phase, "phase");
        return phase.get() == JobPhase.IDLE;
    }

    public static boolean isDispatching(AtomicReference<JobPhase> phase) {
        Objects.requireNonNull(phase, "phase");
        return phase.get() == JobPhase.DISPATCHING;
    }

    /**
     * 当前相位；{@code phase} 为 null 时视为 {@link JobPhase#IDLE}。
     */
    public static JobPhase get(AtomicReference<JobPhase> phase) {
        if (phase == null)
            return JobPhase.IDLE;
        JobPhase p = phase.get();
        return p != null ? p : JobPhase.IDLE;
    }
}
