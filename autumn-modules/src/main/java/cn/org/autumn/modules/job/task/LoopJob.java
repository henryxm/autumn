package cn.org.autumn.modules.job.task;

import cn.org.autumn.annotation.JobAssign;
import cn.org.autumn.annotation.JobMeta;
import cn.org.autumn.bean.EnvBean;
import cn.org.autumn.config.Config;
import cn.org.autumn.site.Factory;
import cn.org.autumn.site.LoadFactory;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Component
public class LoopJob extends Factory implements LoadFactory.Must {

    @Autowired
    EnvBean envBean;

    /**
     * 已禁用的任务ID集合，key 格式为 category|className
     */
    static final Set<String> disabledJobIds = ConcurrentHashMap.newKeySet();

    // ========== 服务器标签 ==========

    /**
     * 当前服务器标签，从环境变量 server.tag 读取。
     * 用于判断定时任务是否应该在当前服务器上执行。
     * 值为 "*" 时表示运行所有任务（忽略分配限制）。
     */
    @Getter
    private static volatile String serverTag = "";

    /**
     * 服务器分配功能是否已初始化（数据库配置已加载）
     */
    private static volatile boolean assignInitialized = false;

    static {
        // 从环境变量或系统属性读取服务器标签
        String tag = Config.getEnv("server.tag");
        if (StringUtils.isNotBlank(tag)) {
            serverTag = tag.trim();
        }
    }

    // ========== 全局管理状态 ==========

    private static volatile boolean globalPaused = false;

    private static final Map<String, Boolean> categoryEnabled = new ConcurrentHashMap<>();

    private static final Map<String, JobInfo> jobInfoMap = new ConcurrentHashMap<>();

    /**
     * 分类名称与显示名称的映射
     */
    private static final LinkedHashMap<String, String> CATEGORY_DISPLAY = new LinkedHashMap<>();

    static {
        CATEGORY_DISPLAY.put("OneSecond", "1秒");
        CATEGORY_DISPLAY.put("ThreeSecond", "3秒");
        CATEGORY_DISPLAY.put("FiveSecond", "5秒");
        CATEGORY_DISPLAY.put("TenSecond", "10秒");
        CATEGORY_DISPLAY.put("ThirtySecond", "30秒");
        CATEGORY_DISPLAY.put("OneMinute", "1分钟");
        CATEGORY_DISPLAY.put("FiveMinute", "5分钟");
        CATEGORY_DISPLAY.put("TenMinute", "10分钟");
        CATEGORY_DISPLAY.put("ThirtyMinute", "30分钟");
        CATEGORY_DISPLAY.put("OneHour", "1小时");
        CATEGORY_DISPLAY.put("TenHour", "10小时");
        CATEGORY_DISPLAY.put("ThirtyHour", "30小时");
        CATEGORY_DISPLAY.put("OneDay", "1天");
        CATEGORY_DISPLAY.put("OneWeek", "1周");
    }

    /**
     * 每个分类对应的触发间隔（毫秒），用于检测执行超限
     */
    private static final LinkedHashMap<String, Long> CATEGORY_INTERVAL = new LinkedHashMap<>();

    static {
        CATEGORY_INTERVAL.put("OneSecond", 1000L);
        CATEGORY_INTERVAL.put("ThreeSecond", 3000L);
        CATEGORY_INTERVAL.put("FiveSecond", 5000L);
        CATEGORY_INTERVAL.put("TenSecond", 10000L);
        CATEGORY_INTERVAL.put("ThirtySecond", 30000L);
        CATEGORY_INTERVAL.put("OneMinute", 60000L);
        CATEGORY_INTERVAL.put("FiveMinute", 300000L);
        CATEGORY_INTERVAL.put("TenMinute", 600000L);
        CATEGORY_INTERVAL.put("ThirtyMinute", 1800000L);
        CATEGORY_INTERVAL.put("OneHour", 3600000L);
        CATEGORY_INTERVAL.put("TenHour", 36000000L);
        CATEGORY_INTERVAL.put("ThirtyHour", 108000000L);
        CATEGORY_INTERVAL.put("OneDay", 86400000L);
        CATEGORY_INTERVAL.put("OneWeek", 604800000L);
    }

    // ========== 批量执行追踪 ==========

    /**
     * 分类最近一次批量执行耗时（ms）
     */
    private static final ConcurrentHashMap<String, Long> categoryLastBatchDuration = new ConcurrentHashMap<>();

    /**
     * 分类最近一次批量执行开始时间
     */
    private static final ConcurrentHashMap<String, Long> categoryLastBatchTime = new ConcurrentHashMap<>();

    /**
     * 分类批量执行超限计数（单次批量耗时 > 分类间隔）
     */
    private static final ConcurrentHashMap<String, AtomicLong> categoryOverrunCount = new ConcurrentHashMap<>();

    // ========== 并行执行引擎 ==========

    /**
     * 并行执行开关：启用后同一分类的多个任务将通过线程池并行执行，
     * 大幅降低秒级任务的批量执行耗时
     */
    private static volatile boolean parallelExecution = false;

    /**
     * 任务执行线程池，daemon 线程，自动扩缩
     */
    private static final ExecutorService jobExecutor = new ThreadPoolExecutor(
            4, Math.max(8, Runtime.getRuntime().availableProcessors() * 2),
            60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(1024),
            r -> {
                Thread t = new Thread(r, "loop-job-worker");
                t.setDaemon(true);
                return t;
            },
            new ThreadPoolExecutor.CallerRunsPolicy()
    );

    // ========== 任务信息跟踪类 ==========

    @Getter
    @Setter
    public static class JobInfo {
        private final String id;
        private final String className;
        private final String simpleName;
        private final String category;
        private final String categoryDisplayName;
        private final Job job;

        // ---- 来自 @JobMeta 注解 ----
        private String displayName;
        private String description;
        private String group;
        private int order;
        private boolean skipIfRunning;
        private long timeout;
        private int maxConsecutiveErrors;
        private String[] tags;

        // ---- 来自 @JobAssign 注解 / 数据库配置 ----
        /**
         * 当前生效的服务器分配标签（逗号分隔），为空表示在所有服务器运行。
         * 初始值来自注解，启动后可被数据库配置覆盖。
         */
        private volatile String assignTag = "";
        /**
         * 注解中定义的默认分配标签，不可通过管理界面修改，仅供参考。
         */
        private String defaultAssignTag = "";

        // ---- 运行时统计 ----
        private final AtomicLong executionCount = new AtomicLong(0);
        private final AtomicLong errorCount = new AtomicLong(0);
        private final AtomicLong skippedCount = new AtomicLong(0);
        private final AtomicLong timeoutCount = new AtomicLong(0);
        private final AtomicLong consecutiveErrorCount = new AtomicLong(0);
        private final AtomicBoolean running = new AtomicBoolean(false);
        private volatile long lastExecutionTime;
        private volatile long lastExecutionDuration;
        private volatile long lastErrorTime;
        private volatile String lastErrorMessage;
        private volatile boolean autoDisabled;

        public JobInfo(String category, Job job) {
            Class<?> userClass = getUserClass(job);
            this.id = category + "|" + userClass.getName();
            this.className = userClass.getName();
            this.simpleName = userClass.getSimpleName();
            this.category = category;
            this.categoryDisplayName = CATEGORY_DISPLAY.getOrDefault(category, category);
            this.job = job;
            // 默认值
            this.displayName = userClass.getSimpleName();
            this.description = "";
            this.group = "";
            this.order = 100;
            this.skipIfRunning = true;
            this.timeout = 0;
            this.maxConsecutiveErrors = 0;
            this.tags = new String[0];
            // 解析注解
            resolveAnnotation(this, job, category);
        }

        public boolean isEnabled() {
            return !disabledJobIds.contains(id);
        }

        public boolean isRunning() {
            return running.get();
        }

        public Map<String, Object> toMap() {
            boolean enabled = isEnabled();
            boolean catEnabled = isCategoryEnabled(category);
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", id);
            map.put("className", className);
            map.put("simpleName", simpleName);
            map.put("displayName", displayName);
            map.put("description", description);
            map.put("group", group);
            map.put("order", order);
            map.put("category", category);
            map.put("categoryDisplayName", categoryDisplayName);
            map.put("enabled", enabled);
            map.put("categoryEnabled", catEnabled);
            map.put("effectiveEnabled", enabled && catEnabled && !globalPaused);
            map.put("skipIfRunning", skipIfRunning);
            map.put("timeout", timeout);
            map.put("maxConsecutiveErrors", maxConsecutiveErrors);
            map.put("tags", tags);
            map.put("assignTag", assignTag);
            map.put("defaultAssignTag", defaultAssignTag);
            map.put("assignedToThisServer", isAssignedToThisServer(this));
            map.put("running", running.get());
            map.put("autoDisabled", autoDisabled);
            map.put("executionCount", executionCount.get());
            map.put("errorCount", errorCount.get());
            map.put("skippedCount", skippedCount.get());
            map.put("timeoutCount", timeoutCount.get());
            map.put("consecutiveErrorCount", consecutiveErrorCount.get());
            map.put("lastExecutionTime", lastExecutionTime);
            map.put("lastExecutionDuration", lastExecutionDuration);
            map.put("lastErrorTime", lastErrorTime);
            map.put("lastErrorMessage", lastErrorMessage);
            return map;
        }
    }

    // ========== 工具方法 ==========

    private static Class<?> getUserClass(Job job) {
        return ClassUtils.getUserClass(job.getClass());
    }

    /**
     * 解析 @JobMeta 和 @JobAssign 注解，方法级别优先于类级别
     */
    private static void resolveAnnotation(JobInfo info, Job job, String category) {
        Class<?> userClass = getUserClass(job);
        // 类级别 @JobMeta
        JobMeta classMeta = userClass.getAnnotation(JobMeta.class);
        if (classMeta != null) {
            applyAnnotation(info, classMeta, userClass.getSimpleName());
        }
        // 类级别 @JobAssign
        JobAssign classAssign = userClass.getAnnotation(JobAssign.class);
        if (classAssign != null && classAssign.value().length > 0) {
            String tags = String.join(",", classAssign.value());
            info.defaultAssignTag = tags;
            info.assignTag = tags;
        }
        String methodName = "on" + category;
        try {
            Method method = userClass.getMethod(methodName);
            // 方法级别 @JobMeta
            JobMeta methodMeta = method.getAnnotation(JobMeta.class);
            if (methodMeta != null) {
                applyAnnotation(info, methodMeta, userClass.getSimpleName() + "." + methodName);
            }
            // 方法级别 @JobAssign（覆盖类级别）
            JobAssign methodAssign = method.getAnnotation(JobAssign.class);
            if (methodAssign != null) {
                if (methodAssign.value().length > 0) {
                    String tags = String.join(",", methodAssign.value());
                    info.defaultAssignTag = tags;
                    info.assignTag = tags;
                } else {
                    // 方法上标注了空 @JobAssign，清除类级别的分配（表示在所有服务器运行）
                    info.defaultAssignTag = "";
                    info.assignTag = "";
                }
            }
        } catch (NoSuchMethodException ignored) {
        }
    }

    private static void applyAnnotation(JobInfo info, JobMeta meta, String fallbackName) {
        if (!meta.name().isEmpty()) {
            info.displayName = meta.name();
        } else if (info.displayName == null || info.displayName.equals(info.simpleName)) {
            info.displayName = fallbackName;
        }
        if (!meta.description().isEmpty()) info.description = meta.description();
        if (!meta.group().isEmpty()) info.group = meta.group();
        info.order = meta.order();
        info.skipIfRunning = meta.skipIfRunning();
        info.timeout = meta.timeout();
        info.maxConsecutiveErrors = meta.maxConsecutiveErrors();
        if (meta.tags().length > 0) info.tags = meta.tags();
    }

    // ========== 任务注册 ==========

    private static void registerJob(Job job, String category) {
        String key = category + "|" + getUserClass(job).getName();
        jobInfoMap.computeIfAbsent(key, k -> new JobInfo(category, job));
    }

    // ========== 任务执行器（带统计追踪） ==========

    private static void executeJob(Job job, String category, Runnable action) {
        if (globalPaused) return;
        if (!isCategoryEnabled(category)) return;
        Class<?> userClass = getUserClass(job);
        String key = category + "|" + userClass.getName();
        if (disabledJobIds.contains(key)) return;
        JobInfo info = jobInfoMap.get(key);
        if (info == null) return;
        // 服务器分配检查
        if (!isAssignedToThisServer(info)) {
            if (log.isDebugEnabled())
                log.debug("Skip {} Job:{} (not assigned to server tag '{}')", category, userClass.getSimpleName(), serverTag);
            return;
        }
        // 防重入检查
        if (info.skipIfRunning) {
            if (!info.running.compareAndSet(false, true)) {
                info.skippedCount.incrementAndGet();
                if (log.isDebugEnabled())
                    log.debug("Skip {} Job:{} (still running)", category, userClass.getSimpleName());
                return;
            }
        }
        long start = System.currentTimeMillis();
        try {
            if (log.isDebugEnabled())
                log.debug("Run {} Job:{}", category, userClass.getSimpleName());
            action.run();
            // 执行成功：重置连续错误计数
            info.consecutiveErrorCount.set(0);
            // 超时警告检查
            long duration = System.currentTimeMillis() - start;
            if (info.timeout > 0 && duration > info.timeout) {
                info.timeoutCount.incrementAndGet();
                log.warn("Job [{}] exceeded timeout: {}ms > {}ms", key, duration, info.timeout);
            }
        } catch (Exception e) {
            info.errorCount.incrementAndGet();
            info.lastErrorTime = System.currentTimeMillis();
            info.lastErrorMessage = e.getMessage();
            long consecutive = info.consecutiveErrorCount.incrementAndGet();
            if (info.maxConsecutiveErrors > 0 && consecutive >= info.maxConsecutiveErrors) {
                disabledJobIds.add(key);
                info.autoDisabled = true;
                log.error("Job [{}] auto-disabled after {} consecutive errors: {}", key, consecutive, e.getMessage());
            }
            print(job, e);
        } finally {
            info.executionCount.incrementAndGet();
            info.lastExecutionTime = start;
            info.lastExecutionDuration = System.currentTimeMillis() - start;
            if (info.skipIfRunning) {
                info.running.set(false);
            }
        }
    }

    /**
     * 根据分类调用具体接口方法
     */
    private static void runJobByCategory(Job job, String category) {
        switch (category) {
            case "OneSecond":
                if (job instanceof OneSecond) ((OneSecond) job).onOneSecond();
                else job.onJob();
                break;
            case "ThreeSecond":
                if (job instanceof ThreeSecond) ((ThreeSecond) job).onThreeSecond();
                else job.onJob();
                break;
            case "FiveSecond":
                if (job instanceof FiveSecond) ((FiveSecond) job).onFiveSecond();
                else job.onJob();
                break;
            case "TenSecond":
                if (job instanceof TenSecond) ((TenSecond) job).onTenSecond();
                else job.onJob();
                break;
            case "ThirtySecond":
                if (job instanceof ThirtySecond) ((ThirtySecond) job).onThirtySecond();
                else job.onJob();
                break;
            case "OneMinute":
                if (job instanceof OneMinute) ((OneMinute) job).onOneMinute();
                else job.onJob();
                break;
            case "FiveMinute":
                if (job instanceof FiveMinute) ((FiveMinute) job).onFiveMinute();
                else job.onJob();
                break;
            case "TenMinute":
                if (job instanceof TenMinute) ((TenMinute) job).onTenMinute();
                else job.onJob();
                break;
            case "ThirtyMinute":
                if (job instanceof ThirtyMinute) ((ThirtyMinute) job).onThirtyMinute();
                else job.onJob();
                break;
            case "OneHour":
                if (job instanceof OneHour) ((OneHour) job).onOneHour();
                else job.onJob();
                break;
            case "TenHour":
                if (job instanceof TenHour) ((TenHour) job).onTenHour();
                else job.onJob();
                break;
            case "ThirtyHour":
                if (job instanceof ThirtyHour) ((ThirtyHour) job).onThirtyHour();
                else job.onJob();
                break;
            case "OneDay":
                if (job instanceof OneDay) ((OneDay) job).onOneDay();
                else job.onJob();
                break;
            case "OneWeek":
                if (job instanceof OneWeek) ((OneWeek) job).onOneWeek();
                else job.onJob();
                break;
            default:
                job.onJob();
                break;
        }
    }

    // ========== 批量执行引擎（核心优化） ==========

    /**
     * 统一的分类批量执行方法。
     * <p>
     * 1. 支持并行/串行两种模式，parallelExecution=true 时利用线程池并行执行同分类所有任务。
     * 2. 追踪每次批量执行的总耗时，与分类间隔对比检测执行超限。
     * 3. CallerRunsPolicy 背压策略：线程池队列满时在调用线程执行，防止任务丢失。
     *
     * @param category 分类名称
     * @param jobList  该分类的任务列表
     */
    private static void runCategoryJobs(String category, List<Job> jobList) {
        long batchStart = System.currentTimeMillis();
        if (parallelExecution && jobList.size() > 1) {
            // 并行执行：使用 CompletableFuture + 线程池
            List<CompletableFuture<Void>> futures = new ArrayList<>(jobList.size());
            for (Job job : jobList) {
                futures.add(CompletableFuture.runAsync(
                        () -> executeJob(job, category, () -> runJobByCategory(job, category)),
                        jobExecutor));
            }
            try {
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            } catch (Exception e) {
                log.error("Parallel batch error [{}]: {}", category, e.getMessage());
            }
        } else {
            // 串行执行
            for (Job job : jobList) {
                executeJob(job, category, () -> runJobByCategory(job, category));
            }
        }
        long batchDuration = System.currentTimeMillis() - batchStart;
        // 记录批量执行耗时
        categoryLastBatchDuration.put(category, batchDuration);
        categoryLastBatchTime.put(category, batchStart);
        // 超限检测
        Long interval = CATEGORY_INTERVAL.get(category);
        if (interval != null && batchDuration > interval) {
            categoryOverrunCount.computeIfAbsent(category, k -> new AtomicLong(0)).incrementAndGet();
            log.warn("Category [{}] batch overrun: {}ms > {}ms interval", category, batchDuration, interval);
        }
    }

    // ========== 管理方法 ==========

    public static boolean isGlobalPaused() {
        return globalPaused;
    }

    public static void pauseAll() {
        globalPaused = true;
        if (log.isDebugEnabled())
            log.info("All loop jobs paused");
    }

    public static void resumeAll() {
        globalPaused = false;
        if (log.isDebugEnabled())
            log.info("All loop jobs resumed");
    }

    public static boolean isCategoryEnabled(String category) {
        return categoryEnabled.getOrDefault(category, true);
    }

    public static void enableCategory(String category) {
        categoryEnabled.put(category, true);
        if (log.isDebugEnabled())
            log.info("Category [{}] enabled", category);
    }

    public static void disableCategory(String category) {
        categoryEnabled.put(category, false);
        if (log.isDebugEnabled())
            log.info("Category [{}] disabled", category);
    }

    public static boolean enableJob(String jobId) {
        JobInfo info = jobInfoMap.get(jobId);
        if (info == null) return false;
        disabledJobIds.remove(jobId);
        info.autoDisabled = false;
        info.consecutiveErrorCount.set(0);
        if (log.isDebugEnabled())
            log.info("Job [{}] enabled", jobId);
        return true;
    }

    public static boolean disableJob(String jobId) {
        if (!jobInfoMap.containsKey(jobId)) return false;
        disabledJobIds.add(jobId);
        if (log.isDebugEnabled())
            log.info("Job [{}] disabled", jobId);
        return true;
    }

    public static boolean updateJobConfig(String jobId, Map<String, Object> config) {
        JobInfo info = jobInfoMap.get(jobId);
        if (info == null) return false;
        if (config.containsKey("skipIfRunning"))
            info.setSkipIfRunning(Boolean.TRUE.equals(config.get("skipIfRunning")));
        if (config.containsKey("timeout"))
            info.setTimeout(((Number) config.get("timeout")).longValue());
        if (config.containsKey("maxConsecutiveErrors"))
            info.setMaxConsecutiveErrors(((Number) config.get("maxConsecutiveErrors")).intValue());
        if (config.containsKey("order"))
            info.setOrder(((Number) config.get("order")).intValue());
        if (log.isDebugEnabled())
            log.info("Job [{}] config updated: {}", jobId, config);
        return true;
    }

    /**
     * 手动触发指定任务（忽略暂停、分类和启用状态检查，但受防重入约束）
     */
    public static boolean triggerJob(String jobId) {
        JobInfo info = jobInfoMap.get(jobId);
        if (info == null) return false;
        Job job = info.getJob();
        String category = info.getCategory();
        if (info.skipIfRunning && info.running.get()) {
            log.warn("Manual trigger skipped for Job [{}] (still running)", jobId);
            return false;
        }
        long start = System.currentTimeMillis();
        try {
            if (log.isDebugEnabled())
                log.debug("Manual trigger Job: {} [{}]", getUserClass(job).getSimpleName(), category);
            runJobByCategory(job, category);
            info.consecutiveErrorCount.set(0);
        } catch (Exception e) {
            info.errorCount.incrementAndGet();
            info.lastErrorTime = System.currentTimeMillis();
            info.lastErrorMessage = e.getMessage();
            info.consecutiveErrorCount.incrementAndGet();
            print(job, e);
        } finally {
            info.executionCount.incrementAndGet();
            info.lastExecutionTime = start;
            info.lastExecutionDuration = System.currentTimeMillis() - start;
        }
        return true;
    }

    /**
     * 重置指定任务的统计数据
     */
    public static boolean resetJobStats(String jobId) {
        JobInfo info = jobInfoMap.get(jobId);
        if (info == null) return false;
        info.executionCount.set(0);
        info.errorCount.set(0);
        info.skippedCount.set(0);
        info.timeoutCount.set(0);
        info.consecutiveErrorCount.set(0);
        info.lastExecutionTime = 0;
        info.lastExecutionDuration = 0;
        info.lastErrorTime = 0;
        info.lastErrorMessage = null;
        info.autoDisabled = false;
        disabledJobIds.remove(jobId);
        if (log.isDebugEnabled())
            log.info("Job [{}] stats reset", jobId);
        return true;
    }

    // ========== 并行执行控制 ==========

    public static boolean isParallelExecution() {
        return parallelExecution;
    }

    public static void setParallelExecution(boolean parallel) {
        parallelExecution = parallel;
        if (log.isDebugEnabled())
            log.debug("Parallel execution {}", parallel ? "enabled" : "disabled");
    }

    // ========== 服务器分配管理 ==========

    /**
     * 判断指定任务是否分配给当前服务器执行
     *
     * @param info 任务信息
     * @return true: 应该在当前服务器执行; false: 不应该执行
     */
    private static boolean isAssignedToThisServer(JobInfo info) {
        String assignTag = info.getAssignTag();
        // 无分配限制 → 在所有服务器运行
        if (StringUtils.isBlank(assignTag)) {
            return true;
        }
        // 当前服务器标签为 "*" → 运行所有任务
        if ("*".equals(serverTag)) {
            return true;
        }
        // 当前服务器没有标签 → 不运行有分配限制的任务
        if (StringUtils.isBlank(serverTag)) {
            return false;
        }
        // 检查当前服务器标签是否在分配列表中
        String[] tags = assignTag.split(",");
        for (String tag : tags) {
            if (tag.trim().equalsIgnoreCase(serverTag.trim())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 设置服务器标签（通常从环境变量读取，也支持运行时修改）
     */
    public static void setServerTag(String tag) {
        serverTag = tag != null ? tag.trim() : "";
        if (log.isDebugEnabled())
            log.debug("Server tag set to: '{}'", serverTag);
    }

    /**
     * 标记分配功能已初始化（数据库配置已加载）
     */
    public static void markAssignInitialized() {
        assignInitialized = true;
        if (log.isDebugEnabled())
            log.debug("Schedule assign initialized, server tag: '{}'", serverTag);
    }

    /**
     * 分配功能是否已初始化
     */
    public static boolean isAssignInitialized() {
        return assignInitialized;
    }

    /**
     * 获取所有任务信息的只读视图（供 ScheduleAssignService 扫描使用）
     */
    public static Map<String, JobInfo> getJobInfoMap() {
        return Collections.unmodifiableMap(jobInfoMap);
    }

    /**
     * 更新指定任务的服务器分配标签
     *
     * @param jobId     任务ID
     * @param assignTag 分配标签（逗号分隔），为空表示在所有服务器运行
     * @return true: 更新成功; false: 未找到任务
     */
    public static boolean updateJobAssign(String jobId, String assignTag) {
        JobInfo info = jobInfoMap.get(jobId);
        if (info == null) return false;
        info.setAssignTag(assignTag != null ? assignTag.trim() : "");
        if (log.isDebugEnabled())
            log.debug("Job [{}] assign tag updated to: '{}'", jobId, info.getAssignTag());
        return true;
    }

    // ========== 查询方法 ==========

    public static List<Map<String, Object>> getJobList(String category) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (JobInfo info : jobInfoMap.values()) {
            if (category == null || category.isEmpty() || category.equals(info.getCategory())) {
                list.add(info.toMap());
            }
        }
        List<String> categoryOrder = new ArrayList<>(CATEGORY_DISPLAY.keySet());
        list.sort((a, b) -> {
            int catA = categoryOrder.indexOf(a.get("category"));
            int catB = categoryOrder.indexOf(b.get("category"));
            if (catA != catB) return Integer.compare(catA, catB);
            int orderA = (int) a.get("order");
            int orderB = (int) b.get("order");
            if (orderA != orderB) return Integer.compare(orderA, orderB);
            return ((String) a.get("className")).compareTo((String) b.get("className"));
        });
        return list;
    }

    public static List<Map<String, Object>> getCategoryList() {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Map.Entry<String, String> entry : CATEGORY_DISPLAY.entrySet()) {
            String category = entry.getKey();
            String displayName = entry.getValue();
            long jobCount = 0, enabledCount = 0, totalExec = 0, totalErrors = 0, totalSkipped = 0;
            for (JobInfo info : jobInfoMap.values()) {
                if (info.getCategory().equals(category)) {
                    jobCount++;
                    if (info.isEnabled()) enabledCount++;
                    totalExec += info.executionCount.get();
                    totalErrors += info.errorCount.get();
                    totalSkipped += info.skippedCount.get();
                }
            }
            Map<String, Object> catMap = new LinkedHashMap<>();
            catMap.put("name", category);
            catMap.put("displayName", displayName);
            catMap.put("enabled", isCategoryEnabled(category));
            catMap.put("jobCount", jobCount);
            catMap.put("enabledCount", enabledCount);
            catMap.put("totalExecutions", totalExec);
            catMap.put("totalErrors", totalErrors);
            catMap.put("totalSkipped", totalSkipped);
            // 批量执行追踪数据
            catMap.put("lastBatchDuration", categoryLastBatchDuration.getOrDefault(category, 0L));
            catMap.put("intervalMs", CATEGORY_INTERVAL.getOrDefault(category, 0L));
            long overrunCount = 0;
            AtomicLong counter = categoryOverrunCount.get(category);
            if (counter != null) overrunCount = counter.get();
            catMap.put("overrunCount", overrunCount);
            list.add(catMap);
        }
        return list;
    }

    public static Map<String, Object> getStats() {
        long totalJobs = jobInfoMap.size();
        long enabledJobs = 0, disabledJobs = 0, totalExec = 0, totalErrors = 0;
        long totalSkipped = 0, totalTimeouts = 0, runningCount = 0, autoDisabledCount = 0;
        long activeCategoryCount = 0;
        for (JobInfo info : jobInfoMap.values()) {
            if (info.isEnabled() && isCategoryEnabled(info.getCategory())) {
                enabledJobs++;
            } else {
                disabledJobs++;
            }
            totalExec += info.executionCount.get();
            totalErrors += info.errorCount.get();
            totalSkipped += info.skippedCount.get();
            totalTimeouts += info.timeoutCount.get();
            if (info.running.get()) runningCount++;
            if (info.autoDisabled) autoDisabledCount++;
        }
        for (Map.Entry<String, String> entry : CATEGORY_DISPLAY.entrySet()) {
            if (isCategoryEnabled(entry.getKey())) {
                activeCategoryCount++;
            }
        }
        long totalOverruns = 0;
        for (AtomicLong count : categoryOverrunCount.values()) totalOverruns += count.get();
        // 统计未分配到当前服务器的任务数
        long notAssignedCount = 0;
        for (JobInfo info : jobInfoMap.values()) {
            if (!isAssignedToThisServer(info)) {
                notAssignedCount++;
            }
        }

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("globalPaused", globalPaused);
        stats.put("parallelExecution", parallelExecution);
        stats.put("serverTag", serverTag);
        stats.put("assignInitialized", assignInitialized);
        stats.put("totalJobs", totalJobs);
        stats.put("enabledJobs", enabledJobs);
        stats.put("disabledJobs", disabledJobs);
        stats.put("runningCount", runningCount);
        stats.put("autoDisabledCount", autoDisabledCount);
        stats.put("totalExecutions", totalExec);
        stats.put("totalErrors", totalErrors);
        stats.put("totalSkipped", totalSkipped);
        stats.put("totalTimeouts", totalTimeouts);
        stats.put("totalOverruns", totalOverruns);
        stats.put("totalCategories", CATEGORY_DISPLAY.size());
        stats.put("activeCategoryCount", activeCategoryCount);
        stats.put("notAssignedCount", notAssignedCount);
        return stats;
    }

    // ========== 健康预警系统 ==========

    /**
     * 自动检测所有任务和分类的运行时健康状况，返回分级告警列表。
     * <p>
     * 检测维度：
     * <ul>
     *   <li><b>CATEGORY_OVERRUN (CRITICAL)</b>: 分类批量执行耗时超过触发间隔</li>
     *   <li><b>AUTO_DISABLED (CRITICAL)</b>: 任务因连续错误已自动禁用</li>
     *   <li><b>JOB_STUCK (WARNING)</b>: 任务可能卡死（running 时长 > 3 倍间隔）</li>
     *   <li><b>HIGH_ERROR_RATE (WARNING)</b>: 错误率超过 50%</li>
     *   <li><b>APPROACHING_AUTO_DISABLE (WARNING)</b>: 连续错误接近自动禁用阈值 70%</li>
     *   <li><b>LONG_DURATION (WARNING)</b>: 单次执行耗时超过分类间隔 80%</li>
     *   <li><b>FREQUENT_SKIP (INFO)</b>: 跳过率超过 30%</li>
     * </ul>
     */
    public static List<Map<String, Object>> getAlerts() {
        List<Map<String, Object>> alerts = new ArrayList<>();
        long now = System.currentTimeMillis();

        // 分类超限检测
        for (Map.Entry<String, Long> entry : CATEGORY_INTERVAL.entrySet()) {
            String category = entry.getKey();
            long interval = entry.getValue();
            Long lastDuration = categoryLastBatchDuration.get(category);
            if (lastDuration != null && lastDuration > interval) {
                Map<String, Object> alert = new LinkedHashMap<>();
                alert.put("level", "critical");
                alert.put("type", "CATEGORY_OVERRUN");
                alert.put("target", category);
                alert.put("message", "分类 [" + CATEGORY_DISPLAY.getOrDefault(category, category) + "] 批量执行耗时 " + lastDuration + "ms，超过间隔 " + interval + "ms");
                alert.put("value", lastDuration);
                alert.put("threshold", interval);
                AtomicLong cnt = categoryOverrunCount.get(category);
                alert.put("overrunCount", cnt != null ? cnt.get() : 0);
                alert.put("suggestion", "建议启用并行执行或优化慢任务");
                alerts.add(alert);
            }
        }

        // 任务级别检测
        for (JobInfo info : jobInfoMap.values()) {
            long execCount = info.executionCount.get();
            long errCount = info.errorCount.get();
            long skipCount = info.skippedCount.get();

            // 自动禁用告警
            if (info.autoDisabled) {
                Map<String, Object> alert = new LinkedHashMap<>();
                alert.put("level", "critical");
                alert.put("type", "AUTO_DISABLED");
                alert.put("target", info.id);
                alert.put("message", "任务 [" + info.displayName + "] 因连续错误 " + info.consecutiveErrorCount.get() + " 次已自动禁用");
                if (info.lastErrorMessage != null) {
                    alert.put("detail", info.lastErrorMessage);
                }
                alert.put("suggestion", "检查错误原因，修复后在管理页面重新启用");
                alerts.add(alert);
            }

            // 卡死检测
            if (info.running.get() && info.lastExecutionTime > 0) {
                Long interval = CATEGORY_INTERVAL.get(info.category);
                if (interval != null) {
                    long runningFor = now - info.lastExecutionTime;
                    if (runningFor > interval * 3) {
                        Map<String, Object> alert = new LinkedHashMap<>();
                        alert.put("level", "warning");
                        alert.put("type", "JOB_STUCK");
                        alert.put("target", info.id);
                        alert.put("message", "任务 [" + info.displayName + "] 可能卡死，已持续运行 " + (runningFor / 1000) + " 秒");
                        alert.put("value", runningFor);
                        alert.put("suggestion", "检查任务是否存在死锁、网络阻塞或无限循环");
                        alerts.add(alert);
                    }
                }
            }

            // 高错误率
            if (execCount > 10 && errCount > 0) {
                double errorRate = (double) errCount / execCount;
                if (errorRate > 0.5) {
                    Map<String, Object> alert = new LinkedHashMap<>();
                    alert.put("level", "warning");
                    alert.put("type", "HIGH_ERROR_RATE");
                    alert.put("target", info.id);
                    alert.put("message", "任务 [" + info.displayName + "] 错误率 " + String.format("%.0f%%", errorRate * 100) + " (" + errCount + "/" + execCount + ")");
                    alert.put("suggestion", "检查近期错误日志，考虑重置统计或修复根因");
                    alerts.add(alert);
                }
            }

            // 接近自动禁用阈值
            if (info.maxConsecutiveErrors > 0 && !info.autoDisabled) {
                long consecutive = info.consecutiveErrorCount.get();
                if (consecutive > 0 && (double) consecutive / info.maxConsecutiveErrors >= 0.7) {
                    Map<String, Object> alert = new LinkedHashMap<>();
                    alert.put("level", "warning");
                    alert.put("type", "APPROACHING_AUTO_DISABLE");
                    alert.put("target", info.id);
                    alert.put("message", "任务 [" + info.displayName + "] 连续错误 " + consecutive + "/" + info.maxConsecutiveErrors + "，即将触发自动禁用");
                    alert.put("suggestion", "立即检查并修复该任务");
                    alerts.add(alert);
                }
            }

            // 单次执行耗时过长
            if (info.lastExecutionDuration > 0) {
                Long interval = CATEGORY_INTERVAL.get(info.category);
                if (interval != null && info.lastExecutionDuration > interval * 0.8) {
                    Map<String, Object> alert = new LinkedHashMap<>();
                    alert.put("level", "warning");
                    alert.put("type", "LONG_DURATION");
                    alert.put("target", info.id);
                    alert.put("message", "任务 [" + info.displayName + "] 上次执行 " + info.lastExecutionDuration + "ms，接近间隔上限 " + interval + "ms");
                    alert.put("suggestion", "优化任务执行逻辑或将任务迁移到更长间隔的分类");
                    alerts.add(alert);
                }
            }

            // 频繁跳过
            long totalAttempts = execCount + skipCount;
            if (totalAttempts > 10 && skipCount > 0) {
                double skipRate = (double) skipCount / totalAttempts;
                if (skipRate > 0.3) {
                    Map<String, Object> alert = new LinkedHashMap<>();
                    alert.put("level", "info");
                    alert.put("type", "FREQUENT_SKIP");
                    alert.put("target", info.id);
                    alert.put("message", "任务 [" + info.displayName + "] 跳过率 " + String.format("%.0f%%", skipRate * 100) + "，任务执行可能过慢");
                    alert.put("suggestion", "优化任务执行速度或将任务迁移到更长间隔分类");
                    alerts.add(alert);
                }
            }
        }

        // 按严重级别排序：critical > warning > info
        alerts.sort((a, b) -> {
            int la = "critical".equals(a.get("level")) ? 0 : "warning".equals(a.get("level")) ? 1 : 2;
            int lb = "critical".equals(b.get("level")) ? 0 : "warning".equals(b.get("level")) ? 1 : 2;
            return Integer.compare(la, lb);
        });

        return alerts;
    }

    // ========== 原有代码 ==========

    public Set<String> getDisabledJobIds() {
        return Collections.unmodifiableSet(disabledJobIds);
    }

    @Override
    @Order(0)
    public void must() {
        if (StringUtils.isBlank(serverTag) && StringUtils.isNotBlank(envBean.getNodeTag()))
            serverTag = envBean.getNodeTag();
        List<OneSecond> oneSeconds = getOrderList(OneSecond.class, "onOneSecond");
        for (OneSecond oneSecond : oneSeconds) {
            onOneSecond(oneSecond);
        }

        List<ThreeSecond> threeSeconds = getOrderList(ThreeSecond.class, "onThreeSecond");
        for (ThreeSecond threeSecond : threeSeconds) {
            onThreeSecond(threeSecond);
        }

        List<FiveSecond> fiveSeconds = getOrderList(FiveSecond.class, "onFiveSecond");
        for (FiveSecond fiveSecond : fiveSeconds) {
            onFiveSecond(fiveSecond);
        }

        List<TenSecond> tenSeconds = getOrderList(TenSecond.class, "onTenSecond");
        for (TenSecond tenSecond : tenSeconds) {
            onTenSecond(tenSecond);
        }

        List<ThirtySecond> thirtySeconds = getOrderList(ThirtySecond.class, "onThirtySecond");
        for (ThirtySecond thirtySecond : thirtySeconds) {
            onThirtySecond(thirtySecond);
        }

        List<OneMinute> oneMinutes = getOrderList(OneMinute.class, "onOneMinute");
        for (OneMinute oneMinute : oneMinutes) {
            onOneMinute(oneMinute);
        }

        List<FiveMinute> fiveMinutes = getOrderList(FiveMinute.class, "onFiveMinute");
        for (FiveMinute fiveMinute : fiveMinutes) {
            onFiveMinute(fiveMinute);
        }

        List<TenMinute> tenMinutes = getOrderList(TenMinute.class, "onTenMinute");
        for (TenMinute tenMinute : tenMinutes) {
            onTenMinute(tenMinute);
        }

        List<ThirtyMinute> thirtyMinutes = getOrderList(ThirtyMinute.class, "onThirtyMinute");
        for (ThirtyMinute thirtyMinute : thirtyMinutes) {
            onThirtyMinute(thirtyMinute);
        }

        List<OneHour> oneHours = getOrderList(OneHour.class, "onOneHour");
        for (OneHour oneHour : oneHours) {
            onOneHour(oneHour);
        }

        List<TenHour> tenHours = getOrderList(TenHour.class, "onTenHour");
        for (TenHour tenHour : tenHours) {
            onTenHour(tenHour);
        }

        List<ThirtyHour> thirtyHours = getOrderList(ThirtyHour.class, "onThirtyHour");
        for (ThirtyHour thirtyHour : thirtyHours) {
            onThirtyHour(thirtyHour);
        }

        List<OneDay> oneDays = getOrderList(OneDay.class, "onOneDay");
        for (OneDay oneDay : oneDays) {
            onOneDay(oneDay);
        }

        List<OneWeek> oneWeeks = getOrderList(OneWeek.class, "onOneWeek");
        for (OneWeek oneWeek : oneWeeks) {
            onOneWeek(oneWeek);
        }
    }

    public interface Job {
        default void onJob() {
        }
    }

    public interface OneSecond extends Job {
        default void onOneSecond() {
        }
    }

    public interface ThreeSecond extends Job {
        default void onThreeSecond() {
        }
    }

    public interface FiveSecond extends Job {
        default void onFiveSecond() {
        }
    }

    public interface TenSecond extends Job {
        default void onTenSecond() {
        }
    }

    public interface ThirtySecond extends Job {
        default void onThirtySecond() {
        }
    }

    public interface OneMinute extends Job {
        default void onOneMinute() {
        }
    }

    public interface FiveMinute extends Job {
        default void onFiveMinute() {
        }
    }

    public interface TenMinute extends Job {
        default void onTenMinute() {
        }
    }

    public interface ThirtyMinute extends Job {
        default void onThirtyMinute() {
        }
    }

    public interface OneHour extends Job {
        default void onOneHour() {
        }
    }

    public interface TenHour extends Job {
        default void onTenHour() {
        }
    }

    public interface ThirtyHour extends Job {
        default void onThirtyHour() {
        }
    }

    public interface OneDay extends Job {
        default void onOneDay() {
        }
    }

    public interface OneWeek extends Job {
        default void onOneWeek() {
        }
    }

    private static final List<Job> oneSecondJobList = new CopyOnWriteArrayList<>();
    private static final List<Job> threeSecondJobList = new CopyOnWriteArrayList<>();
    private static final List<Job> fiveSecondJobList = new CopyOnWriteArrayList<>();
    private static final List<Job> tenSecondJobList = new CopyOnWriteArrayList<>();
    private static final List<Job> thirtySecondJobList = new CopyOnWriteArrayList<>();

    private static final List<Job> oneMinuteJobList = new CopyOnWriteArrayList<>();
    private static final List<Job> fiveMinuteJobList = new CopyOnWriteArrayList<>();
    private static final List<Job> tenMinuteJobList = new CopyOnWriteArrayList<>();
    private static final List<Job> thirtyMinuteJobList = new CopyOnWriteArrayList<>();

    private static final List<Job> oneHourJobList = new CopyOnWriteArrayList<>();
    private static final List<Job> tenHourJobList = new CopyOnWriteArrayList<>();
    private static final List<Job> thirtyHourJobList = new CopyOnWriteArrayList<>();

    private static final List<Job> oneDayJobList = new CopyOnWriteArrayList<>();
    private static final List<Job> oneWeekJobList = new CopyOnWriteArrayList<>();

    public static void onOneSecond(Job job) {
        if (!oneSecondJobList.contains(job))
            oneSecondJobList.add(job);
        registerJob(job, "OneSecond");
    }

    public static void onThreeSecond(Job job) {
        if (!threeSecondJobList.contains(job))
            threeSecondJobList.add(job);
        registerJob(job, "ThreeSecond");
    }

    public static void onFiveSecond(Job job) {
        if (!fiveSecondJobList.contains(job))
            fiveSecondJobList.add(job);
        registerJob(job, "FiveSecond");
    }

    public static void onTenSecond(Job job) {
        if (!tenSecondJobList.contains(job))
            tenSecondJobList.add(job);
        registerJob(job, "TenSecond");
    }

    public static void onThirtySecond(Job job) {
        if (!thirtySecondJobList.contains(job))
            thirtySecondJobList.add(job);
        registerJob(job, "ThirtySecond");
    }

    public static void onOneMinute(Job job) {
        if (!oneMinuteJobList.contains(job))
            oneMinuteJobList.add(job);
        registerJob(job, "OneMinute");
    }

    public static void onFiveMinute(Job job) {
        if (!fiveMinuteJobList.contains(job))
            fiveMinuteJobList.add(job);
        registerJob(job, "FiveMinute");
    }

    public static void onTenMinute(Job job) {
        if (!tenMinuteJobList.contains(job))
            tenMinuteJobList.add(job);
        registerJob(job, "TenMinute");
    }

    public static void onThirtyMinute(Job job) {
        if (!thirtyMinuteJobList.contains(job))
            thirtyMinuteJobList.add(job);
        registerJob(job, "ThirtyMinute");
    }

    public static void onOneHour(Job job) {
        if (!oneHourJobList.contains(job))
            oneHourJobList.add(job);
        registerJob(job, "OneHour");
    }

    public static void onTenHour(Job job) {
        if (!tenHourJobList.contains(job))
            tenHourJobList.add(job);
        registerJob(job, "TenHour");
    }

    public static void onThirtyHour(Job job) {
        if (!thirtyHourJobList.contains(job))
            thirtyHourJobList.add(job);
        registerJob(job, "ThirtyHour");
    }

    public static void onOneDay(Job job) {
        if (!oneDayJobList.contains(job))
            oneDayJobList.add(job);
        registerJob(job, "OneDay");
    }

    public static void onOneWeek(Job job) {
        if (!oneWeekJobList.contains(job))
            oneWeekJobList.add(job);
        registerJob(job, "OneWeek");
    }

    private static void print(Job job, Exception e) {
        log.error("Job(" + getUserClass(job).getName() + "):" + e.getMessage());
    }

    // ========== 定时任务执行方法（使用统一批量引擎） ==========

    public static void runOneSecondJob() {
        runCategoryJobs("OneSecond", oneSecondJobList);
    }

    public static void runThreeSecondJob() {
        runCategoryJobs("ThreeSecond", threeSecondJobList);
    }

    public static void runFiveSecondJob() {
        runCategoryJobs("FiveSecond", fiveSecondJobList);
    }

    public static void runTenSecondJob() {
        runCategoryJobs("TenSecond", tenSecondJobList);
    }

    public static void runThirtySecondJob() {
        runCategoryJobs("ThirtySecond", thirtySecondJobList);
    }

    public static void runOneMinuteJob() {
        runCategoryJobs("OneMinute", oneMinuteJobList);
    }

    public static void runFiveMinuteJob() {
        runCategoryJobs("FiveMinute", fiveMinuteJobList);
    }

    public static void runTenMinuteJob() {
        runCategoryJobs("TenMinute", tenMinuteJobList);
    }

    public static void runThirtyMinuteJob() {
        runCategoryJobs("ThirtyMinute", thirtyMinuteJobList);
    }

    public static void runOneHourJob() {
        runCategoryJobs("OneHour", oneHourJobList);
    }

    public static void runTenHourJob() {
        runCategoryJobs("TenHour", tenHourJobList);
    }

    public static void runThirtyHourJob() {
        runCategoryJobs("ThirtyHour", thirtyHourJobList);
    }

    public static void runOneDayJob() {
        runCategoryJobs("OneDay", oneDayJobList);
    }

    public static void runOneWeekJob() {
        runCategoryJobs("OneWeek", oneWeekJobList);
    }

    /**
     * 打印所有分类的任务列表
     */
    public static Map<String, List<String>> print() {
        Map<String, List<String>> map = new LinkedHashMap<>();
        map.put("OneSecondJob", collectNames(oneSecondJobList));
        map.put("ThreeSecondJob", collectNames(threeSecondJobList));
        map.put("FiveSecondJob", collectNames(fiveSecondJobList));
        map.put("TenSecondJob", collectNames(tenSecondJobList));
        map.put("ThirtySecondJob", collectNames(thirtySecondJobList));
        map.put("OneMinuteJob", collectNames(oneMinuteJobList));
        map.put("FiveMinuteJob", collectNames(fiveMinuteJobList));
        map.put("TenMinuteJob", collectNames(tenMinuteJobList));
        map.put("ThirtyMinuteJob", collectNames(thirtyMinuteJobList));
        map.put("OneHourJob", collectNames(oneHourJobList));
        map.put("TenHourJob", collectNames(tenHourJobList));
        map.put("ThirtyHourJob", collectNames(thirtyHourJobList));
        map.put("OneDayJob", collectNames(oneDayJobList));
        map.put("OneWeekJob", collectNames(oneWeekJobList));
        return map;
    }

    private static List<String> collectNames(List<Job> jobList) {
        List<String> names = new ArrayList<>();
        for (Job job : jobList) {
            names.add(getUserClass(job).getName());
        }
        return names;
    }
}
