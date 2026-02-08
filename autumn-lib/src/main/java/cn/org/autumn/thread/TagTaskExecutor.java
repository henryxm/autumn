package cn.org.autumn.thread;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
public class TagTaskExecutor extends ThreadPoolTaskExecutor {

    static List<Tag> running = new CopyOnWriteArrayList<>();

    static Set<String> ids = new CopyOnWriteArraySet<>();

    /**
     * 已完成任务历史记录（有容量限制）
     */
    static List<TaskRecord> history = new CopyOnWriteArrayList<>();

    /**
     * 最大历史记录数，超过后自动裁剪最旧条目。可通过管理界面动态调整。
     */
    private static volatile int maxHistorySize = 500;

    /**
     * 历史记录裁剪锁 — 防止多线程同时裁剪（非阻塞，tryLock 失败则跳过本次裁剪）
     */
    private static final ReentrantLock historyTrimLock = new ReentrantLock();

    /**
     * 统计计数器
     */
    private static final AtomicLong totalSubmitted = new AtomicLong(0);
    private static final AtomicLong totalCompleted = new AtomicLong(0);
    private static final AtomicLong totalFailed = new AtomicLong(0);
    private static final AtomicLong totalRejected = new AtomicLong(0);
    private static final AtomicLong totalInterrupted = new AtomicLong(0);
    private static final AtomicLong totalSkipped = new AtomicLong(0);
    private static final AtomicLong totalExecutionTime = new AtomicLong(0);

    /**
     * 清理线程上次清理的死条目数（用于监控展示）
     */
    private static volatile int lastSweepRemovedCount = 0;

    /**
     * 累计清理的死条目数
     */
    private static final AtomicLong totalSweptCount = new AtomicLong(0);

    /**
     * 全局错峰延迟窗口（秒）。所有未通过 @TagValue.delay() 配置的任务使用此值。
     * <p>设为 0 表示不延迟（默认）。可通过管理界面动态调整。</p>
     */
    private static volatile long globalStaggerSeconds = 0;

    /**
     * 死条目清理线程
     */
    private static volatile ScheduledExecutorService sweeper;
    private static final Object sweeperLock = new Object();

    /**
     * 启动时间
     */
    private static final Date startTime = new Date();

    public static void remove(Tag task) {
        if (task == null) return;
        try {
            String taskId = task.getId();
            if (StringUtils.isNotBlank(taskId)) {
                ids.remove(taskId);
            }
        } catch (Exception e) {
            // 忽略获取 ID 时的异常
        }
        running.remove(task);
    }

    /**
     * 记录任务完成（由TagRunnable/TagCallable调用）
     */
    public static void recordCompletion(Tag task, long duration, boolean success, String errorMessage) {
        if (task == null)
            return;
        try {
            if (success) {
                totalCompleted.incrementAndGet();
            } else {
                boolean taskCancelled = false;
                try {
                    taskCancelled = task.isCancelled();
                } catch (Exception e) {
                    // 忽略 — 状态查询失败视为非取消
                }
                if (taskCancelled) {
                    totalInterrupted.incrementAndGet();
                } else {
                    totalFailed.incrementAndGet();
                }
            }
            totalExecutionTime.addAndGet(Math.max(0, duration));
            boolean taskCancelled = false;
            try {
                taskCancelled = task.isCancelled();
            } catch (Exception e) {
                // 忽略
            }
            String status = success ? "COMPLETED" : (taskCancelled ? "INTERRUPTED" : "FAILED");
            TaskRecord record = TaskRecord.fromTag(task, duration, status, errorMessage);
            history.add(record);
            // 超出上限的 25% 缓冲时触发批量裁剪（摊销 CopyOnWriteArrayList 的复制开销）
            int currentMax = maxHistorySize;
            if (history.size() > currentMax + Math.max(currentMax / 4, 10)) {
                trimHistoryIfNeeded(currentMax);
            }
        } catch (Exception e) {
            if (log.isDebugEnabled())
                log.debug("记录任务完成状态失败: {}", e.getMessage());
        }
    }

    /**
     * 记录拒绝的任务
     */
    public static void recordRejected() {
        totalRejected.incrementAndGet();
    }

    /**
     * 记录跳过的任务 — 任务已提交但未实际执行业务逻辑。
     * <p>跳过原因包括：系统未就绪、错峰延迟被中断、Redis 不可用、未获取到分布式锁、获取锁异常等。</p>
     * <p>调用此方法后 {@code totalSubmitted = totalCompleted + totalFailed + totalInterrupted + totalRejected + totalSkipped + runningCount}。</p>
     *
     * @param task   被跳过的任务
     * @param reason 跳过原因描述
     */
    public static void recordSkipped(Tag task, String reason) {
        if (task == null) return;
        try {
            totalSkipped.incrementAndGet();
            TaskRecord record = TaskRecord.fromTag(task, 0, "SKIPPED", reason);
            history.add(record);
            int currentMax = maxHistorySize;
            if (history.size() > currentMax + Math.max(currentMax / 4, 10)) {
                trimHistoryIfNeeded(currentMax);
            }
        } catch (Exception e) {
            if (log.isDebugEnabled())
                log.debug("记录跳过任务状态失败: {}", e.getMessage());
        }
    }

    public void execute(TagRunnable task) {
        if (task == null) {
            if (log.isDebugEnabled())
                log.debug("尝试执行 null 任务，已忽略");
            return;
        }
        String taskId = task.getId();
        if (StringUtils.isNotBlank(taskId)) {
            if (ids.contains(taskId))
                return;
            ids.add(taskId);
        }
        totalSubmitted.incrementAndGet();
        running.add(task);
        try {
            super.execute(task);
        } catch (Exception e) {
            // 提交失败时清理状态
            running.remove(task);
            if (StringUtils.isNotBlank(taskId)) {
                ids.remove(taskId);
            }
            recordRejected();
            if (log.isDebugEnabled())
                log.debug("任务提交到线程池失败: tag={}, error={}", task.getTag(), e.getMessage());
            throw e;
        }
    }

    public Future<?> submit(TagRunnable task) {
        if (task == null) {
            if (log.isDebugEnabled())
                log.debug("尝试提交 null 任务，已忽略");
            return null;
        }
        // ID 去重（与 execute() 一致）
        String taskId = task.getId();
        if (StringUtils.isNotBlank(taskId)) {
            if (ids.contains(taskId))
                return null;
            ids.add(taskId);
        }
        totalSubmitted.incrementAndGet();
        running.add(task);
        try {
            return super.submit(task);
        } catch (Exception e) {
            running.remove(task);
            if (StringUtils.isNotBlank(taskId)) {
                ids.remove(taskId);
            }
            recordRejected();
            if (log.isDebugEnabled())
                log.debug("任务提交到线程池失败: tag={}, error={}", task.getTag(), e.getMessage());
            throw e;
        }
    }

    public <T> Future<T> submit(TagCallable<T> task) {
        if (task == null) {
            if (log.isDebugEnabled())
                log.debug("尝试提交 null 任务，已忽略");
            return null;
        }
        // ID 去重（与 execute() 一致）
        String taskId = task.getId();
        if (StringUtils.isNotBlank(taskId)) {
            if (ids.contains(taskId))
                return null;
            ids.add(taskId);
        }
        totalSubmitted.incrementAndGet();
        running.add(task);
        try {
            return super.submit(task);
        } catch (Exception e) {
            running.remove(task);
            if (StringUtils.isNotBlank(taskId)) {
                ids.remove(taskId);
            }
            recordRejected();
            if (log.isDebugEnabled())
                log.debug("任务提交到线程池失败: tag={}, error={}", task.getTag(), e.getMessage());
            throw e;
        }
    }

    public List<Tag> getRunning() {
        return running;
    }

    // ======================== 任务中断管理 ========================

    /**
     * 按任务ID中断任务
     *
     * @param taskId 任务ID
     * @return 操作结果描述
     */
    public String interruptTask(String taskId) {
        if (StringUtils.isBlank(taskId)) {
            return "任务ID不能为空";
        }
        for (Tag task : running) {
            if (taskId.equals(task.getId())) {
                return doInterrupt(task);
            }
        }
        return "未找到ID为 [" + taskId + "] 的运行中任务";
    }

    /**
     * 按列表索引中断任务
     *
     * @param index 任务在运行列表中的索引（0-based）
     * @return 操作结果描述
     */
    public String interruptTaskByIndex(int index) {
        if (index < 0 || index >= running.size()) {
            return "索引越界: " + index + "，当前运行中任务数: " + running.size();
        }
        try {
            Tag task = running.get(index);
            return doInterrupt(task);
        } catch (IndexOutOfBoundsException e) {
            return "任务已结束或索引已变化";
        }
    }

    /**
     * 执行中断操作
     */
    private String doInterrupt(Tag task) {
        if (task == null) {
            return "任务对象为空";
        }
        String desc = "tag=" + safe(task.getTag()) + ", method=" + safe(task.getMethod()) + ", id=" + safe(task.getId());
        Thread t = task.getThread();
        if (t == null) {
            return "任务尚未绑定执行线程: " + desc;
        }
        if (!t.isAlive()) {
            remove(task);
            return "任务线程已结束: " + desc;
        }
        boolean sent = task.cancel();
        if (sent) {
            if (log.isDebugEnabled())
                log.debug("已中断任务: {}, threadName={}, threadState={}", desc, t.getName(), task.getThreadState());
            return "已发送中断信号: " + desc;
        } else {
            return "中断信号发送失败: " + desc;
        }
    }

    /**
     * 获取指定任务的线程堆栈（用于诊断卡死）
     *
     * @param index    任务索引
     * @param maxDepth 最大堆栈深度
     * @return 堆栈信息
     */
    public String getTaskStackTrace(int index, int maxDepth) {
        if (index < 0 || index >= running.size()) {
            return "索引越界";
        }
        try {
            Tag task = running.get(index);
            String stack = task.getStackTrace(maxDepth);
            return StringUtils.isNotBlank(stack) ? stack : "无法获取堆栈（任务可能已结束）";
        } catch (IndexOutOfBoundsException e) {
            return "任务已结束";
        }
    }

    // ======================== 历史与统计 ========================

    public List<TaskRecord> getHistory() {
        return history;
    }

    public List<TaskRecord> getHistory(int page, int size) {
        List<TaskRecord> reversed = new ArrayList<>(history);
        Collections.reverse(reversed);

        int from = Math.min(page * size, reversed.size());
        int to = Math.min(from + size, reversed.size());
        if (from >= reversed.size()) {
            return Collections.emptyList();
        }
        return reversed.subList(from, to);
    }

    public int getHistoryTotal() {
        return history.size();
    }

    public void clearHistory() {
        history.clear();
    }

    /**
     * 获取线程池详细信息
     */
    public Map<String, Object> getPoolInfo() {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("activeCount", getActiveCount());
        info.put("corePoolSize", getCorePoolSize());
        info.put("maxPoolSize", getMaxPoolSize());
        info.put("poolSize", getPoolSize());
        ThreadPoolExecutor executor = getThreadPoolExecutor();
        info.put("queueSize", executor.getQueue().size());
        info.put("queueCapacity", executor.getQueue().remainingCapacity() + executor.getQueue().size());
        info.put("completedTaskCount", executor.getCompletedTaskCount());
        info.put("taskCount", executor.getTaskCount());
        info.put("largestPoolSize", executor.getLargestPoolSize());
        info.put("keepAliveSeconds", executor.getKeepAliveTime(TimeUnit.SECONDS));
        info.put("totalSubmitted", totalSubmitted.get());
        info.put("totalCompleted", totalCompleted.get());
        info.put("totalFailed", totalFailed.get());
        info.put("totalInterrupted", totalInterrupted.get());
        info.put("totalRejected", totalRejected.get());
        info.put("totalSkipped", totalSkipped.get());
        info.put("runningCount", running.size());
        info.put("historyCount", history.size());
        info.put("startTime", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(startTime));
        info.put("uptime", formatUptime(System.currentTimeMillis() - startTime.getTime()));
        long completed = totalCompleted.get() + totalFailed.get() + totalInterrupted.get();
        if (completed > 0) {
            info.put("avgExecutionTime", totalExecutionTime.get() / completed);
        } else {
            info.put("avgExecutionTime", 0);
        }
        // 超时和延迟中任务数（安全遍历，忽略单个任务异常）
        int timeoutCount = 0;
        int delayingCount = 0;
        for (Tag task : running) {
            try {
                if (task != null) {
                    if (task.isTimeout()) timeoutCount++;
                    if (task.isDelaying()) delayingCount++;
                }
            } catch (Exception e) {
                // 忽略
            }
        }
        info.put("timeoutCount", timeoutCount);
        info.put("delayingCount", delayingCount);
        info.put("globalStaggerSeconds", globalStaggerSeconds);
        info.put("maxHistorySize", maxHistorySize);
        info.put("totalSweptCount", totalSweptCount.get());
        info.put("lastSweepRemovedCount", lastSweepRemovedCount);
        info.put("idsCount", ids.size());
        // 队列和线程回收配置
        info.put("allowCoreThreadTimeOut", executor.allowsCoreThreadTimeOut());
        // 系统资源信息
        Runtime rt = Runtime.getRuntime();
        info.put("cpuCores", rt.availableProcessors());
        info.put("heapMaxMB", rt.maxMemory() / (1024 * 1024));
        info.put("heapUsedMB", (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024));
        info.put("heapTotalMB", rt.totalMemory() / (1024 * 1024));
        return info;
    }

    /**
     * 获取运行中任务的详细列表（含线程状态、超时信息）
     * <p>注意：不主动采集堆栈信息（开销大），堆栈通过 {@link #getTaskStackTrace(int, int)} 按需获取。</p>
     */
    public List<Map<String, Object>> getRunningDetails() {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        List<Map<String, Object>> list = new ArrayList<>();
        long now = System.currentTimeMillis();
        int index = 0;
        for (Tag task : running) {
            try {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("index", index++);
                map.put("name", safe(task.getName()));
                map.put("id", safe(task.getId()));
                map.put("tag", safe(task.getTag()));
                map.put("method", safe(task.getMethod()));
                Class<?> type = task.getType();
                map.put("type", type != null ? type.getSimpleName() : "Unknown");
                Date time = task.getTime();
                map.put("time", time != null ? format.format(time) : "");
                // 运行时长
                long elapsed = time != null ? now - time.getTime() : 0;
                map.put("runningTime", formatDuration(elapsed));
                map.put("runningMs", elapsed);
                // 线程状态
                map.put("threadState", safe(task.getThreadState()));
                Thread thread = task.getThread();
                map.put("threadName", thread != null ? thread.getName() : "");
                // 超时信息
                map.put("timeout", task.getTimeout());
                map.put("isTimeout", task.isTimeout());
                // 取消状态
                map.put("cancelled", task.isCancelled());
                // 分布式锁信息
                map.put("isLocked", task.isLocked());
                // 错峰延迟信息
                map.put("isDelaying", task.isDelaying());
                map.put("delay", task.getDelay());

                list.add(map);
            } catch (Exception e) {
                // 单个任务出错不影响整体列表
                log.debug("获取任务详情失败: index={}, error={}", index - 1, e.getMessage());
            }
        }
        return list;
    }

    /**
     * null 安全的字符串
     */
    private static String safe(String value) {
        return value != null ? value : "";
    }

    // ======================== 动态配置 ========================

    public void updateCorePoolSize(int corePoolSize) {
        setCorePoolSize(corePoolSize);
        ThreadPoolExecutor executor = getThreadPoolExecutor();
        executor.setCorePoolSize(corePoolSize);
        if (log.isDebugEnabled())
            log.debug("核心线程数已更新为: {}", corePoolSize);
    }

    public void updateMaxPoolSize(int maxPoolSize) {
        setMaxPoolSize(maxPoolSize);
        ThreadPoolExecutor executor = getThreadPoolExecutor();
        executor.setMaximumPoolSize(maxPoolSize);
        if (log.isDebugEnabled())
            log.debug("最大线程数已更新为: {}", maxPoolSize);
    }

    /**
     * 动态调整非核心线程空闲存活时间。
     * <p>运行时立即生效，影响后续空闲线程的回收判断。</p>
     *
     * @param seconds 存活时间（秒），最小 1
     */
    public void updateKeepAliveSeconds(int seconds) {
        int old = (int) getThreadPoolExecutor().getKeepAliveTime(TimeUnit.SECONDS);
        setKeepAliveSeconds(seconds);
        getThreadPoolExecutor().setKeepAliveTime(seconds, TimeUnit.SECONDS);
        if (log.isDebugEnabled())
            log.debug("保活时间已更新: {}s -> {}s", old, seconds);
    }

    /**
     * 动态切换核心线程超时回收。
     * <p>{@code true}: 核心线程空闲超过 keepAliveSeconds 后被回收（节省资源）。</p>
     * <p>{@code false}: 核心线程永不回收（低延迟）。</p>
     *
     * @param allow 是否允许核心线程超时
     */
    public void updateAllowCoreThreadTimeOut(boolean allow) {
        boolean old = getThreadPoolExecutor().allowsCoreThreadTimeOut();
        getThreadPoolExecutor().allowCoreThreadTimeOut(allow);
        if (log.isDebugEnabled())
            log.debug("核心线程超时回收已更新: {} -> {}", old, allow);
    }

    public void resetStats() {
        totalSubmitted.set(0);
        totalCompleted.set(0);
        totalFailed.set(0);
        totalInterrupted.set(0);
        totalRejected.set(0);
        totalSkipped.set(0);
        totalExecutionTime.set(0);
        if (log.isDebugEnabled())
            log.debug("线程池统计数据已重置");
    }

    // ======================== 全局错峰延迟 ========================

    /**
     * 获取全局错峰延迟窗口（秒）。
     * <p>所有未通过 @TagValue.delay() 配置的任务使用此值。</p>
     *
     * @return 延迟窗口秒数，0 = 不延迟
     */
    public static long getGlobalStaggerSeconds() {
        return globalStaggerSeconds;
    }

    /**
     * 设置全局错峰延迟窗口（秒）。运行时立即生效，影响后续所有新提交的任务。
     * <p>已在运行或延迟中的任务不受影响。</p>
     *
     * @param seconds 延迟窗口秒数，0 = 不延迟
     */
    public static void setGlobalStaggerSeconds(long seconds) {
        long old = globalStaggerSeconds;
        globalStaggerSeconds = Math.max(0, seconds);
        if (log.isDebugEnabled())
            log.debug("全局错峰延迟已更新: {}s -> {}s", old, globalStaggerSeconds);
    }

    // ======================== 格式化工具 ========================

    static String formatDuration(long millis) {
        if (millis < 1000) {
            return millis + "ms";
        } else if (millis < 60000) {
            return String.format("%.1fs", millis / 1000.0);
        } else if (millis < 3600000) {
            long min = millis / 60000;
            long sec = (millis % 60000) / 1000;
            return min + "m" + sec + "s";
        } else {
            long hours = millis / 3600000;
            long min = (millis % 3600000) / 60000;
            return hours + "h" + min + "m";
        }
    }

    private static String formatUptime(long millis) {
        long seconds = millis / 1000;
        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        if (days > 0) {
            return days + "天" + hours + "小时" + minutes + "分";
        } else if (hours > 0) {
            return hours + "小时" + minutes + "分" + secs + "秒";
        } else if (minutes > 0) {
            return minutes + "分" + secs + "秒";
        } else {
            return secs + "秒";
        }
    }

    // ======================== 历史记录管理 ========================

    /**
     * 获取最大历史记录数
     */
    public static int getMaxHistorySize() {
        return maxHistorySize;
    }

    /**
     * 设置最大历史记录数。运行时立即生效，下次记录写入时触发裁剪。
     *
     * @param size 最大记录数，最小 10，最大 100000
     */
    public static void setMaxHistorySize(int size) {
        int old = maxHistorySize;
        maxHistorySize = Math.max(10, Math.min(100000, size));
        if (log.isDebugEnabled())
            log.debug("最大历史记录数已更新: {} -> {}", old, maxHistorySize);
        // 如果新上限比当前记录数小，立即触发裁剪
        if (history.size() > maxHistorySize) {
            trimHistoryIfNeeded(maxHistorySize);
        }
    }

    /**
     * 批量裁剪历史记录 — 移除最旧的超出部分。
     * <p>使用 {@code subList(0, excess).clear()} 实现单次数组复制，
     * 比逐条 {@code remove(0)} 高效得多（后者每次都复制整个数组）。</p>
     *
     * @param targetMax 目标上限
     */
    private static void trimHistoryIfNeeded(int targetMax) {
        if (!historyTrimLock.tryLock()) return; // 其他线程正在裁剪，跳过
        try {
            int size = history.size();
            if (size <= targetMax) return;
            int excess = size - targetMax;
            try {
                // subList().clear() 在 CopyOnWriteArrayList 上是单次原子操作
                history.subList(0, excess).clear();
                if (log.isDebugEnabled()) {
                    log.debug("历史记录已裁剪: 移除最旧 {} 条，剩余 {}", excess, history.size());
                }
            } catch (Exception e) {
                log.debug("历史记录裁剪异常: {}", e.getMessage());
            }
        } finally {
            historyTrimLock.unlock();
        }
    }

    // ======================== 死条目清理线程 (Sweeper) ========================

    /**
     * 启动死条目清理线程。
     * <p>在 Spring 容器初始化后自动启动（通过 {@link #afterPropertiesSet()}），
     * 每 60 秒扫描一次 {@code running} 列表，移除以下僵尸条目：</p>
     * <ul>
     *   <li>线程已死亡（{@code thread != null && !thread.isAlive()}）— 如 OOM/ThreadDeath 导致 finally 未执行</li>
     *   <li>线程引用为 null 且任务停留超过 30 分钟 — 任务从未被线程池拾起（如线程池关闭）</li>
     * </ul>
     */
    @Override
    public void afterPropertiesSet() {
        super.afterPropertiesSet();
        startSweeper();
    }

    @Override
    public void destroy() {
        stopSweeper();
        super.destroy();
    }

    private void startSweeper() {
        synchronized (sweeperLock) {
            if (sweeper != null) return;
            sweeper = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "tag-task-sweeper");
                t.setDaemon(true);
                return t;
            });
            sweeper.scheduleWithFixedDelay(TagTaskExecutor::sweepDeadEntries, 60, 60, TimeUnit.SECONDS);
            if (log.isDebugEnabled())
                log.debug("任务清理线程已启动（每60秒扫描一次）");
        }
    }

    private static void stopSweeper() {
        synchronized (sweeperLock) {
            if (sweeper != null) {
                sweeper.shutdownNow();
                sweeper = null;
                if (log.isDebugEnabled())
                    log.debug("任务清理线程已停止");
            }
        }
    }

    /**
     * 扫描并清理 running 列表中的死条目。
     * <p>由后台 sweeper 线程定期调用，也可手动触发。</p>
     */
    static void sweepDeadEntries() {
        try {
            long now = System.currentTimeMillis();
            List<Tag> deadTasks = new ArrayList<>();
            for (Tag task : running) {
                if (task == null) {
                    deadTasks.add(null);
                    continue;
                }
                try {
                    Thread thread = task.getThread();
                    // 情况 1：线程已绑定但已死亡
                    // 说明 finally 块未正常执行（如 OOM、ThreadDeath、Thread.stop）
                    if (thread != null && !thread.isAlive()) {
                        deadTasks.add(task);
                        continue;
                    }
                    // 情况 2：线程为 null 且任务停留时间过长
                    // 可能是：(a) 任务从未被线程池拾起 (b) clearThread() 执行了但 remove() 未执行
                    // 使用 30 分钟阈值，避免误清理正常排队中的任务
                    if (thread == null) {
                        Date taskTime = task.getTime();
                        if (taskTime != null) {
                            long elapsed = now - taskTime.getTime();
                            if (elapsed > 30 * 60 * 1000L) { // 30 分钟
                                deadTasks.add(task);
                            }
                        }
                    }
                } catch (Exception e) {
                    // 单个任务检查失败不影响整体
                }
            }
            // 批量清理
            for (Tag task : deadTasks) {
                try {
                    if (task != null) {
                        if (log.isDebugEnabled())
                            log.debug("清理死任务条目: tag={}, method={}, id={}, thread={}", safe(task.getTag()), safe(task.getMethod()), safe(task.getId()), task.getThread() != null ? task.getThread().getName() : "null");
                    }
                    remove(task);
                } catch (Exception e) {
                    // 单个清理失败不影响整体
                    try {
                        running.remove(task);
                    } catch (Exception ex) { /* ignore */ }
                }
            }
            int removed = deadTasks.size();
            lastSweepRemovedCount = removed;
            if (removed > 0) {
                totalSweptCount.addAndGet(removed);
                if (log.isDebugEnabled())
                    log.debug("本轮清理了 {} 个死任务条目，剩余运行中: {}，ID 缓存: {}", removed, running.size(), ids.size());
            }
        } catch (Exception e) {
            if (log.isDebugEnabled())
                log.debug("清理死任务时异常: {}", e.getMessage());
        }
    }
}
