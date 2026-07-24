package cn.org.autumn.thread;

import cn.org.autumn.database.CrudGuard;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * 全局串行函数队列：专用守护线程 + {@link LinkedBlockingQueue#take()} 空闲等待，常驻不销毁。
 * <p>
 * 容量为<strong>软上限</strong>（不替换队列实例）。单任务硬超时：先 interrupt，宽限后仍未结束则
 * <strong>废弃旧 worker 并拉起新线程</strong>继续 {@code take()}，保证后续任务不被永久堵住。
 * 见 {@code docs/AI_FUNCTION_QUEUE.md}。
 * </p>
 */
@Slf4j
@Component
public class FunctionQueue {

    private static final String WORKER_NAME = "autumn-function-queue";
    private static final String WATCHDOG_NAME = "autumn-function-queue-watchdog";
    private static final int DEFAULT_CAPACITY = 10000;
    private static final int DEFAULT_MAX_HISTORY = 500;
    private static final long DEFAULT_SLOW_MS = 30000L;
    /** 单任务最大执行时间；超时 interrupt，再超时则硬废弃 worker */
    private static final long DEFAULT_MAX_TASK_MS = 60000L;
    /** interrupt 后仍未退出则硬废弃的宽限 */
    private static final long DEFAULT_HARD_ABANDON_MS = 5000L;
    private static final long WATCHDOG_INTERVAL_MS = 1000L;
    private static final int BACKLOG_WARN_THRESHOLD = 1000;
    private static final long STOP_JOIN_MS = 3000L;
    private static final int DEFAULT_PEEK_LIMIT = 50;
    /** 有积压且非执行中超过该时间视为卡死，尝试唤醒/重启 */
    private static final long STALL_MS = 5000L;

    private final Object offerLock = new Object();
    private final Object abandonLock = new Object();
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicLong accepted = new AtomicLong();
    private final AtomicLong executed = new AtomicLong();
    private final AtomicLong failed = new AtomicLong();
    private final AtomicLong rejected = new AtomicLong();
    private final AtomicLong dropped = new AtomicLong();
    private final AtomicLong cleared = new AtomicLong();
    private final AtomicLong interrupted = new AtomicLong();
    private final AtomicLong slowCount = new AtomicLong();
    private final AtomicLong totalExecMs = new AtomicLong();
    private final AtomicLong maxExecMs = new AtomicLong();
    private final AtomicLong recovered = new AtomicLong();
    private final AtomicLong workerRestarts = new AtomicLong();
    private final AtomicLong timedOut = new AtomicLong();
    private final AtomicLong abandoned = new AtomicLong();
    private final AtomicLong workerGeneration = new AtomicLong();
    private final AtomicLong taskIdSeq = new AtomicLong();

    private final ConcurrentLinkedDeque<FunctionTaskRecord> history = new ConcurrentLinkedDeque<>();
    /** 同名任务去重：已在排队或执行中的 name */
    private final ConcurrentHashMap<String, Boolean> namedInFlight = new ConcurrentHashMap<>();

    /** 队列实例永不替换，容量仅作软上限 */
    private final LinkedBlockingQueue<QueuedFunction> queue = new LinkedBlockingQueue<>();
    private volatile int capacity = DEFAULT_CAPACITY;
    private volatile OverflowPolicy overflowPolicy = OverflowPolicy.REJECT;
    private volatile int maxHistorySize = DEFAULT_MAX_HISTORY;
    private volatile long slowTaskThresholdMs = DEFAULT_SLOW_MS;
    private volatile long maxTaskTimeoutMs = DEFAULT_MAX_TASK_MS;
    private volatile long hardAbandonMs = DEFAULT_HARD_ABANDON_MS;

    private volatile Thread worker;
    private volatile ScheduledExecutorService watchdog;
    private volatile String currentName;
    private volatile long currentEnqueueMs;
    private volatile long currentStartMs;
    private volatile long currentTaskId;
    private final AtomicBoolean idleWaiting = new AtomicBoolean(false);
    private final AtomicBoolean timeoutInterruptSent = new AtomicBoolean(false);
    private final AtomicLong lastProgressMs = new AtomicLong(System.currentTimeMillis());
    /** 有积压且未执行时首次观测时间；正常空闲 take 不计入 */
    private final AtomicLong idleStallSinceMs = new AtomicLong(0);
    private final Date startTime = new Date();

    @PostConstruct
    public void start() {
        if (!running.compareAndSet(false, true))
            return;
        startWorkerThread();
        startWatchdog();
        log.info("FunctionQueue worker started: name={}, capacity={}, maxTaskTimeoutMs={}, hardAbandonMs={}", WORKER_NAME, capacity, maxTaskTimeoutMs, hardAbandonMs);
    }

    @PreDestroy
    public void stop() {
        if (!running.compareAndSet(true, false))
            return;
        stopWatchdog();
        workerGeneration.incrementAndGet();
        Thread t = worker;
        if (t != null)
            t.interrupt();
        if (t != null) {
            try {
                t.join(STOP_JOIN_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        int discarded;
        synchronized (offerLock) {
            discarded = queue.size();
            queue.clear();
            namedInFlight.clear();
        }
        worker = null;
        clearCurrent();
        if (discarded > 0)
            log.warn("FunctionQueue stopped with pending tasks discarded: count={}", discarded);
        else
            log.info("FunctionQueue worker stopped: name={}", WORKER_NAME);
    }

    public boolean offer(Runnable task) {
        return offer(null, task);
    }

    public boolean offer(String name, Runnable task) {
        Objects.requireNonNull(task, "task");
        if (!running.get()) {
            log.warn("FunctionQueue not running, task rejected: name={}", name);
            rejected.incrementAndGet();
            return false;
        }
        if (name != null && namedInFlight.putIfAbsent(name, Boolean.TRUE) != null) {
            // 同名已在排队或执行：合并，避免 syncProfile 等重复堆积
            return true;
        }
        CrudGuard.Snapshot snapshot = CrudGuard.capture();
        QueuedFunction item = new QueuedFunction(name, snapshot, task, System.currentTimeMillis());
        boolean ok;
        synchronized (offerLock) {
            ok = enqueueLocked(item);
        }
        if (!ok) {
            if (name != null)
                namedInFlight.remove(name);
            rejected.incrementAndGet();
            log.warn("FunctionQueue overflow rejected: name={}, capacity={}, policy={}, size={}", name, capacity, overflowPolicy, queue.size());
            return false;
        }
        accepted.incrementAndGet();
        warnIfBacklog(queue.size());
        // 入队本身会唤醒 LinkedBlockingQueue.take()；卡死/超时交由 watchdog，避免空闲后每次 offer 误报 WARN
        return true;
    }

    public <T> boolean offer(T arg, Consumer<T> action) {
        return offer(null, arg, action);
    }

    public <T> boolean offer(String name, T arg, Consumer<T> action) {
        Objects.requireNonNull(action, "action");
        return offer(name, () -> action.accept(arg));
    }

    private boolean enqueueLocked(QueuedFunction item) {
        if (queue.size() < capacity)
            return queue.offer(item);
        if (overflowPolicy == OverflowPolicy.DROP_OLDEST) {
            QueuedFunction old = queue.poll();
            if (old != null) {
                dropped.incrementAndGet();
                if (old.name != null)
                    namedInFlight.remove(old.name, Boolean.TRUE);
            }
            return queue.offer(item);
        }
        return false;
    }

    public int size() {
        return queue.size();
    }

    public boolean isEmpty() {
        return queue.isEmpty();
    }

    public int getCapacity() {
        return capacity;
    }

    public OverflowPolicy getOverflowPolicy() {
        return overflowPolicy;
    }

    public int getMaxHistorySize() {
        return maxHistorySize;
    }

    public long getSlowTaskThresholdMs() {
        return slowTaskThresholdMs;
    }

    public long getMaxTaskTimeoutMs() {
        return maxTaskTimeoutMs;
    }

    public long getHardAbandonMs() {
        return hardAbandonMs;
    }

    public int clear() {
        int n;
        synchronized (offerLock) {
            n = queue.size();
            QueuedFunction item;
            while ((item = queue.poll()) != null) {
                if (item.name != null)
                    namedInFlight.remove(item.name, Boolean.TRUE);
            }
        }
        if (n > 0) {
            cleared.addAndGet(n);
            log.warn("FunctionQueue cleared pending tasks: count={}", n);
        }
        return n;
    }

    public boolean isRunning() {
        return running.get();
    }

    public long getAccepted() {
        return accepted.get();
    }

    public long getExecuted() {
        return executed.get();
    }

    public long getFailed() {
        return failed.get();
    }

    public long getRejected() {
        return rejected.get();
    }

    public long getDropped() {
        return dropped.get();
    }

    public long getTimedOut() {
        return timedOut.get();
    }

    public long getAbandoned() {
        return abandoned.get();
    }

    public String interruptCurrent() {
        Thread t = worker;
        if (t == null || !running.get())
            return "worker not running";
        if (currentName == null)
            return "no current task";
        t.interrupt();
        interrupted.incrementAndGet();
        log.warn("FunctionQueue interrupt requested: task={}", currentName);
        return "interrupt sent: " + currentName;
    }

    public String getWorkerStackTrace(int depth) {
        Thread t = worker;
        if (t == null)
            return "worker not available";
        int max = Math.max(1, Math.min(depth, 200));
        StackTraceElement[] stack = t.getStackTrace();
        StringBuilder sb = new StringBuilder();
        sb.append("Thread: ").append(t.getName()).append(" state=").append(t.getState()).append('\n');
        int n = Math.min(max, stack.length);
        for (int i = 0; i < n; i++)
            sb.append("  at ").append(stack[i]).append('\n');
        return sb.toString();
    }

    /**
     * 自愈入口：忙碌超时（interrupt / 硬废弃）+ 空闲积压唤醒。
     * 管理端「解除卡死」在有当前任务时直接硬废弃，确保立刻进入下一任务。
     */
    public String recoverIfStalled() {
        return recoverIfStalled(false);
    }

    /**
     * @param forceAbandonBusy true 时若有当前任务则立即硬废弃（管理端）
     */
    public String recoverIfStalled(boolean forceAbandonBusy) {
        if (!running.get())
            return "not running";
        if (forceAbandonBusy && currentName != null)
            return forceAbandonCurrent("manual recover");
        String busy = recoverBusyTimeout();
        if (busy != null)
            return busy;
        return recoverIdleStall();
    }

    public List<Map<String, Object>> peekPending(int limit) {
        recoverIfStalled();
        int lim = limit <= 0 ? DEFAULT_PEEK_LIMIT : Math.min(limit, 200);
        List<Map<String, Object>> list = new ArrayList<>();
        long now = System.currentTimeMillis();
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        int i = 0;
        for (QueuedFunction item : queue) {
            if (i >= lim)
                break;
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("index", i);
            map.put("name", item.name != null ? item.name : "anonymous");
            map.put("enqueueTime", fmt.format(new Date(item.enqueueTime)));
            map.put("waitMs", Math.max(0, now - item.enqueueTime));
            map.put("waitTime", FunctionTaskRecord.formatDuration(Math.max(0, now - item.enqueueTime)));
            list.add(map);
            i++;
        }
        return list;
    }

    public Map<String, Object> getCurrent() {
        Map<String, Object> map = new LinkedHashMap<>();
        String name = currentName;
        long start = currentStartMs;
        long enq = currentEnqueueMs;
        map.put("busy", name != null);
        map.put("name", name != null ? name : "");
        if (name != null && start > 0) {
            long now = System.currentTimeMillis();
            long runMs = Math.max(0, now - start);
            map.put("startTime", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(start)));
            map.put("enqueueTime", enq > 0 ? new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(enq)) : "");
            map.put("runningMs", runMs);
            map.put("runningTime", FunctionTaskRecord.formatDuration(runMs));
            map.put("waitMs", enq > 0 ? Math.max(0, start - enq) : 0);
            map.put("slow", runMs >= slowTaskThresholdMs);
            map.put("overTimeout", runMs >= maxTaskTimeoutMs);
            map.put("timeoutInterruptSent", timeoutInterruptSent.get());
        } else {
            map.put("runningMs", 0);
            map.put("runningTime", "-");
            map.put("slow", false);
            map.put("overTimeout", false);
            map.put("timeoutInterruptSent", false);
        }
        Thread t = worker;
        map.put("threadName", t != null ? t.getName() : "");
        map.put("threadState", t != null ? String.valueOf(t.getState()) : "");
        map.put("idleWaiting", idleWaiting.get());
        return map;
    }

    public List<FunctionTaskRecord> getHistory() {
        return new ArrayList<>(history);
    }

    public void clearHistory() {
        history.clear();
    }

    public void resetStats() {
        accepted.set(0);
        executed.set(0);
        failed.set(0);
        rejected.set(0);
        dropped.set(0);
        cleared.set(0);
        interrupted.set(0);
        slowCount.set(0);
        totalExecMs.set(0);
        maxExecMs.set(0);
        recovered.set(0);
        workerRestarts.set(0);
        timedOut.set(0);
        abandoned.set(0);
    }

    public void updateConfig(Integer newCapacity, OverflowPolicy policy, Integer newMaxHistory, Long slowMs) {
        updateConfig(newCapacity, policy, newMaxHistory, slowMs, null, null);
    }

    public void updateConfig(Integer newCapacity, OverflowPolicy policy, Integer newMaxHistory, Long slowMs, Long maxTaskMs, Long abandonMs) {
        synchronized (offerLock) {
            if (policy != null)
                overflowPolicy = policy;
            if (newMaxHistory != null) {
                maxHistorySize = Math.max(10, Math.min(100000, newMaxHistory));
                trimHistory();
            }
            if (slowMs != null)
                slowTaskThresholdMs = Math.max(1000L, Math.min(3600000L, slowMs));
            if (maxTaskMs != null)
                maxTaskTimeoutMs = Math.max(1000L, Math.min(3600000L, maxTaskMs));
            if (abandonMs != null)
                hardAbandonMs = Math.max(500L, Math.min(300000L, abandonMs));
            if (newCapacity != null) {
                int cap = Math.max(1, Math.min(1000000, newCapacity));
                if (cap != capacity) {
                    capacity = cap;
                    while (queue.size() > capacity) {
                        QueuedFunction drop = queue.poll();
                        if (drop == null)
                            break;
                        dropped.incrementAndGet();
                        if (drop.name != null)
                            namedInFlight.remove(drop.name, Boolean.TRUE);
                    }
                    log.info("FunctionQueue capacity updated: capacity={}, pending={}", capacity, queue.size());
                }
            }
        }
        recoverIfStalled();
    }

    public Map<String, Object> getStatus() {
        recoverIfStalled();
        Map<String, Object> info = new LinkedHashMap<>();
        int size = queue.size();
        info.put("running", running.get());
        info.put("queueSize", size);
        info.put("capacity", capacity);
        info.put("remainingCapacity", Math.max(0, capacity - size));
        info.put("accepted", accepted.get());
        info.put("executed", executed.get());
        info.put("failed", failed.get());
        info.put("rejected", rejected.get());
        info.put("dropped", dropped.get());
        info.put("busy", currentName != null);
        info.put("currentName", currentName != null ? currentName : "");
        long start = currentStartMs;
        if (currentName != null && start > 0)
            info.put("currentRunningMs", Math.max(0, System.currentTimeMillis() - start));
        else
            info.put("currentRunningMs", 0);
        info.put("idleWaiting", idleWaiting.get());
        info.put("recovered", recovered.get());
        info.put("workerRestarts", workerRestarts.get());
        info.put("timedOut", timedOut.get());
        info.put("abandoned", abandoned.get());
        return info;
    }

    public Map<String, Object> getInfo() {
        Map<String, Object> info = getStatus();
        info.put("cleared", cleared.get());
        info.put("interrupted", interrupted.get());
        info.put("slowCount", slowCount.get());
        info.put("overflowPolicy", overflowPolicy.name());
        info.put("maxHistorySize", maxHistorySize);
        info.put("historyCount", history.size());
        info.put("slowTaskThresholdMs", slowTaskThresholdMs);
        info.put("maxTaskTimeoutMs", maxTaskTimeoutMs);
        info.put("hardAbandonMs", hardAbandonMs);
        long done = executed.get() + failed.get();
        info.put("avgExecutionTime", done > 0 ? totalExecMs.get() / done : 0);
        info.put("maxExecutionTime", maxExecMs.get());
        info.put("totalExecMs", totalExecMs.get());
        info.put("startTime", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(startTime));
        info.put("uptime", formatUptime(System.currentTimeMillis() - startTime.getTime()));
        info.put("workerName", WORKER_NAME);
        Thread t = worker;
        info.put("threadState", t != null ? String.valueOf(t.getState()) : "");
        info.put("namedInFlight", namedInFlight.size());
        info.put("workerGeneration", workerGeneration.get());
        info.put("current", getCurrent());
        return info;
    }

    private void warnIfBacklog(int size) {
        if (size >= BACKLOG_WARN_THRESHOLD && size % BACKLOG_WARN_THRESHOLD == 0)
            log.warn("FunctionQueue backlog high: size={}, accepted={}, executed={}, failed={}, rejected={}", size, accepted.get(), executed.get(), failed.get(), rejected.get());
    }

    private void startWatchdog() {
        ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, WATCHDOG_NAME);
            t.setDaemon(true);
            return t;
        });
        watchdog = exec;
        exec.scheduleWithFixedDelay(() -> {
            try {
                if (running.get())
                    recoverIfStalled(false);
            } catch (Throwable t) {
                log.error("FunctionQueue watchdog error:{}", t.getMessage(), t);
            }
        }, WATCHDOG_INTERVAL_MS, WATCHDOG_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    private void stopWatchdog() {
        ScheduledExecutorService exec = watchdog;
        watchdog = null;
        if (exec != null) {
            exec.shutdownNow();
            try {
                exec.awaitTermination(2, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void startWorkerThread() {
        long gen = workerGeneration.get();
        Thread t = new Thread(() -> loop(gen), WORKER_NAME);
        t.setDaemon(true);
        worker = t;
        lastProgressMs.set(System.currentTimeMillis());
        t.start();
    }

    private synchronized void restartWorkerThread(String reason) {
        if (!running.get())
            return;
        workerGeneration.incrementAndGet();
        Thread old = worker;
        if (old != null && old.isAlive())
            old.interrupt();
        workerRestarts.incrementAndGet();
        startWorkerThread();
        log.warn("FunctionQueue worker restarted: reason={}", reason);
    }

    private void loop(long generation) {
        while (running.get() && workerGeneration.get() == generation) {
            try {
                idleWaiting.set(true);
                QueuedFunction item;
                try {
                    item = queue.take();
                } finally {
                    idleWaiting.set(false);
                }
                if (!running.get() || workerGeneration.get() != generation)
                    break;
                lastProgressMs.set(System.currentTimeMillis());
                idleStallSinceMs.set(0);
                runOne(item, generation);
                lastProgressMs.set(System.currentTimeMillis());
                idleStallSinceMs.set(0);
            } catch (InterruptedException e) {
                idleWaiting.set(false);
                if (!running.get() || workerGeneration.get() != generation)
                    break;
                Thread.interrupted();
            } catch (Throwable t) {
                idleWaiting.set(false);
                log.error("FunctionQueue worker loop error:{}", t.getMessage(), t);
            }
        }
    }

    private void runOne(QueuedFunction item, long generation) {
        String name = item.name != null ? item.name : "anonymous";
        // 已出队：允许执行中再次 offer 同名续跑（分批）；仍阻止「仅排队」重复
        if (item.name != null)
            namedInFlight.remove(item.name, Boolean.TRUE);
        long start = System.currentTimeMillis();
        long taskId = taskIdSeq.incrementAndGet();
        currentName = name;
        currentEnqueueMs = item.enqueueTime;
        currentStartMs = start;
        currentTaskId = taskId;
        timeoutInterruptSent.set(false);
        String status = "COMPLETED";
        String error = null;
        try {
            CrudGuard.with(item.snapshot, item.task);
            if (Thread.currentThread().isInterrupted())
                Thread.interrupted();
            executed.incrementAndGet();
        } catch (Throwable e) {
            boolean wasTimeout = timeoutInterruptSent.get() || isInterruptRelated(e);
            if (wasTimeout) {
                status = "TIMED_OUT";
                error = e.getMessage() != null ? e.getMessage() : "task timed out";
                timedOut.incrementAndGet();
                failed.incrementAndGet();
                log.warn("FunctionQueue task timed out: name={}, error={}", name, error);
            } else {
                status = "FAILED";
                error = e.getMessage();
                failed.incrementAndGet();
                log.error("FunctionQueue task failed: name={}, error={}", name, e.getMessage(), e);
            }
        } finally {
            if (!isStillOwner(generation, taskId)) {
                log.warn("FunctionQueue abandoned task finished late: name={}", name);
                return;
            }
            long end = System.currentTimeMillis();
            long duration = Math.max(0, end - start);
            totalExecMs.addAndGet(duration);
            updateMax(duration);
            if (duration >= slowTaskThresholdMs) {
                slowCount.incrementAndGet();
                log.warn("FunctionQueue slow task: name={}, durationMs={}, thresholdMs={}", name, duration, slowTaskThresholdMs);
            }
            addHistory(FunctionTaskRecord.of(name, item.enqueueTime, start, end, status, error));
            clearCurrent();
        }
    }

    private boolean isStillOwner(long generation, long taskId) {
        return running.get() && workerGeneration.get() == generation && currentTaskId == taskId && currentName != null;
    }

    private static boolean isInterruptRelated(Throwable e) {
        if (e instanceof InterruptedException)
            return true;
        Throwable c = e;
        while (c != null) {
            if (c instanceof InterruptedException)
                return true;
            c = c.getCause();
        }
        return Thread.currentThread().isInterrupted();
    }

    /**
     * 忙碌超时：超过 maxTaskTimeoutMs → interrupt；再超过 hardAbandonMs → 硬废弃并换 worker。
     *
     * @return 动作描述；无需处理时返回 null
     */
    private String recoverBusyTimeout() {
        String name = currentName;
        long start = currentStartMs;
        long taskId = currentTaskId;
        if (name == null || start <= 0)
            return null;
        long runMs = System.currentTimeMillis() - start;
        long max = maxTaskTimeoutMs;
        if (runMs < max)
            return null;
        Thread t = worker;
        if (!timeoutInterruptSent.getAndSet(true)) {
            if (t != null)
                t.interrupt();
            interrupted.incrementAndGet();
            recovered.incrementAndGet();
            log.warn("FunctionQueue task timeout interrupt: name={}, runMs={}, maxMs={}", name, runMs, max);
            return "timeout interrupt: " + name;
        }
        if (runMs >= max + hardAbandonMs) {
            if (currentTaskId != taskId || currentStartMs != start)
                return "already finished";
            return forceAbandonCurrent("hard timeout after interrupt");
        }
        return "timeout waiting abandon: " + name;
    }

    private String recoverIdleStall() {
        // 队列空 = 正常空闲，绝不当作卡死（否则每 10s offer 会刷 WARN）
        if (queue.isEmpty()) {
            idleStallSinceMs.set(0);
            return "ok";
        }
        if (currentName != null) {
            idleStallSinceMs.set(0);
            return "busy";
        }
        Thread t = worker;
        if (t == null || !t.isAlive()) {
            idleStallSinceMs.set(0);
            restartWorkerThread("worker dead");
            return "restarted: worker dead";
        }
        long now = System.currentTimeMillis();
        long since = idleStallSinceMs.updateAndGet(v -> v <= 0 ? now : v);
        long stuckMs = now - since;
        // 有积压却迟迟未进入执行：给 take() 信号留出宽限，持续卡住再处理
        if (stuckMs < STALL_MS)
            return "ok";
        recovered.incrementAndGet();
        idleStallSinceMs.set(0);
        if (idleWaiting.get()) {
            t.interrupt();
            if (log.isDebugEnabled())
                log.debug("FunctionQueue stall wake: size={}, stuckMs={}, idleWaiting=true", queue.size(), stuckMs);
            if (stuckMs >= STALL_MS * 3) {
                restartWorkerThread("stall timeout");
                return "restarted: stall timeout";
            }
            return "woke idle worker";
        }
        restartWorkerThread("worker not progressing");
        return "restarted: worker not progressing";
    }

    /**
     * 硬废弃当前任务：记 ABANDONED 历史，抬升 generation，拉起新 worker 继续消费队列。
     * 旧线程若仍阻塞在不可中断 I/O 上，将成为孤儿守护线程（无法 100% 杀掉 JVM 线程），但队列不再被它堵住。
     */
    private String forceAbandonCurrent(String reason) {
        synchronized (abandonLock) {
            String name = currentName;
            long start = currentStartMs;
            long enq = currentEnqueueMs;
            long taskId = currentTaskId;
            if (name == null || start <= 0)
                return "no current task";
            long end = System.currentTimeMillis();
            long duration = Math.max(0, end - start);
            addHistory(FunctionTaskRecord.of(name, enq > 0 ? enq : start, start, end, "ABANDONED", "abandoned: " + reason));
            totalExecMs.addAndGet(duration);
            updateMax(duration);
            failed.incrementAndGet();
            abandoned.incrementAndGet();
            timedOut.incrementAndGet();
            recovered.incrementAndGet();
            clearCurrent();
            // 抬升 generation，使旧 loop/runOne 不再认领后续任务
            workerGeneration.incrementAndGet();
            Thread old = worker;
            if (old != null)
                old.interrupt();
            workerRestarts.incrementAndGet();
            startWorkerThread();
            log.warn("FunctionQueue task abandoned: name={}, runMs={}, reason={}", name, duration, reason);
            return "abandoned: " + name + " (" + reason + ")";
        }
    }

    private void clearCurrent() {
        currentName = null;
        currentEnqueueMs = 0;
        currentStartMs = 0;
        currentTaskId = 0;
        timeoutInterruptSent.set(false);
    }

    private void updateMax(long duration) {
        long prev;
        do {
            prev = maxExecMs.get();
            if (duration <= prev)
                return;
        } while (!maxExecMs.compareAndSet(prev, duration));
    }

    private void addHistory(FunctionTaskRecord record) {
        history.addLast(record);
        trimHistory();
    }

    private void trimHistory() {
        int max = maxHistorySize;
        while (history.size() > max)
            history.pollFirst();
    }

    private static String formatUptime(long millis) {
        long seconds = millis / 1000;
        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        if (days > 0)
            return days + "天" + hours + "小时" + minutes + "分";
        if (hours > 0)
            return hours + "小时" + minutes + "分" + secs + "秒";
        if (minutes > 0)
            return minutes + "分" + secs + "秒";
        return secs + "秒";
    }

    private static final class QueuedFunction {
        private final String name;
        private final CrudGuard.Snapshot snapshot;
        private final Runnable task;
        private final long enqueueTime;

        private QueuedFunction(String name, CrudGuard.Snapshot snapshot, Runnable task, long enqueueTime) {
            this.name = name;
            this.snapshot = snapshot;
            this.task = task;
            this.enqueueTime = enqueueTime;
        }
    }
}
