package cn.org.autumn.modules.job.task;

import cn.org.autumn.annotation.JobMeta;
import cn.org.autumn.site.Factory;
import cn.org.autumn.site.LoadFactory;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Component
public class LoopJob extends Factory implements LoadFactory.Must {

    /**
     * 已禁用的任务ID集合，key 格式为 category|className
     * <p>
     * 使用 jobId 而非对象 hashCode 作为标识，实现同一个类中不同分类任务的独立启停控制。
     */
    static final Set<String> disabledJobIds = ConcurrentHashMap.newKeySet();

    // ========== 全局管理状态 ==========

    private static volatile boolean globalPaused = false;

    private static final Map<String, Boolean> categoryEnabled = new ConcurrentHashMap<>();

    private static final Map<String, JobInfo> jobInfoMap = new ConcurrentHashMap<>();

    /**
     * 分类名称与显示名称的映射（有序）
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

        // ---- 来自 @LoopJobMeta 注解 ----
        private String displayName;
        private String description;
        private String group;
        private int order;
        private boolean skipIfRunning;
        private long timeout;
        private int maxConsecutiveErrors;
        private String[] tags;

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
            this.skipIfRunning = false;
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

    /**
     * 获取 Job 的原始类（去除 Spring CGLIB 动态代理后缀）
     */
    private static Class<?> getUserClass(Job job) {
        return ClassUtils.getUserClass(job.getClass());
    }

    /**
     * 解析 @LoopJobMeta 注解，方法级别优先于类级别
     */
    private static void resolveAnnotation(JobInfo info, Job job, String category) {
        Class<?> userClass = getUserClass(job);

        // 1. 读取类级别注解作为默认值
        JobMeta classMeta = userClass.getAnnotation(JobMeta.class);
        if (classMeta != null) {
            applyAnnotation(info, classMeta, userClass.getSimpleName());
        }

        // 2. 尝试读取方法级别注解（优先级更高，覆盖类级别）
        String methodName = "on" + category;
        try {
            Method method = userClass.getMethod(methodName);
            JobMeta methodMeta = method.getAnnotation(JobMeta.class);
            if (methodMeta != null) {
                applyAnnotation(info, methodMeta, userClass.getSimpleName() + "." + methodName);
            }
        } catch (NoSuchMethodException ignored) {
            // 该方法不存在（可能是通过 runJob() 注册的），忽略
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

        // 防重入检查：上次任务尚未完成则跳过
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

            // 连续错误计数，达到阈值自动禁用
            long consecutive = info.consecutiveErrorCount.incrementAndGet();
            if (info.maxConsecutiveErrors > 0 && consecutive >= info.maxConsecutiveErrors) {
                disabledJobIds.add(key);
                info.autoDisabled = true;
                log.error("Job [{}] auto-disabled after {} consecutive errors: {}",
                        key, consecutive, e.getMessage());
            }
            print(job, e);
        } finally {
            info.executionCount.incrementAndGet();
            info.lastExecutionTime = start;
            info.lastExecutionDuration = System.currentTimeMillis() - start;

            // 释放运行标记
            if (info.skipIfRunning) {
                info.running.set(false);
            }
        }
    }

    /**
     * 根据分类执行任务的具体方法
     */
    private static void runJobByCategory(Job job, String category) {
        switch (category) {
            case "OneSecond":
                if (job instanceof OneSecond) ((OneSecond) job).onOneSecond();
                else job.runJob();
                break;
            case "ThreeSecond":
                if (job instanceof ThreeSecond) ((ThreeSecond) job).onThreeSecond();
                else job.runJob();
                break;
            case "FiveSecond":
                if (job instanceof FiveSecond) ((FiveSecond) job).onFiveSecond();
                else job.runJob();
                break;
            case "TenSecond":
                if (job instanceof TenSecond) ((TenSecond) job).onTenSecond();
                else job.runJob();
                break;
            case "ThirtySecond":
                if (job instanceof ThirtySecond) ((ThirtySecond) job).onThirtySecond();
                else job.runJob();
                break;
            case "OneMinute":
                if (job instanceof OneMinute) ((OneMinute) job).onOneMinute();
                else job.runJob();
                break;
            case "FiveMinute":
                if (job instanceof FiveMinute) ((FiveMinute) job).onFiveMinute();
                else job.runJob();
                break;
            case "TenMinute":
                if (job instanceof TenMinute) ((TenMinute) job).onTenMinute();
                else job.runJob();
                break;
            case "ThirtyMinute":
                if (job instanceof ThirtyMinute) ((ThirtyMinute) job).onThirtyMinute();
                else job.runJob();
                break;
            case "OneHour":
                if (job instanceof OneHour) ((OneHour) job).onOneHour();
                else job.runJob();
                break;
            case "TenHour":
                if (job instanceof TenHour) ((TenHour) job).onTenHour();
                else job.runJob();
                break;
            case "ThirtyHour":
                if (job instanceof ThirtyHour) ((ThirtyHour) job).onThirtyHour();
                else job.runJob();
                break;
            case "OneDay":
                if (job instanceof OneDay) ((OneDay) job).onOneDay();
                else job.runJob();
                break;
            case "OneWeek":
                if (job instanceof OneWeek) ((OneWeek) job).onOneWeek();
                else job.runJob();
                break;
            default:
                job.runJob();
                break;
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

    /**
     * 启用指定任务（按 jobId 精确控制，同一个类的不同分类可独立启停）
     */
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

    /**
     * 禁用指定任务（按 jobId 精确控制，同一个类的不同分类可独立启停）
     */
    public static boolean disableJob(String jobId) {
        if (!jobInfoMap.containsKey(jobId)) return false;
        disabledJobIds.add(jobId);
        if (log.isDebugEnabled())
            log.info("Job [{}] disabled", jobId);
        return true;
    }

    /**
     * 动态更新任务运行时配置参数
     */
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

        // 防重入检查
        if (info.skipIfRunning && info.running.get()) {
            log.warn("Manual trigger skipped for Job [{}] (still running)", jobId);
            return false;
        }

        long start = System.currentTimeMillis();
        try {
            if (log.isDebugEnabled())
                log.info("Manual trigger Job: {} [{}]", getUserClass(job).getSimpleName(), category);
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
     * 获取任务列表，可按分类过滤，按 order 排序
     */
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

    /**
     * 获取分类列表及统计信息
     */
    public static List<Map<String, Object>> getCategoryList() {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Map.Entry<String, String> entry : CATEGORY_DISPLAY.entrySet()) {
            String category = entry.getKey();
            String displayName = entry.getValue();
            long jobCount = 0;
            long enabledCount = 0;
            long totalExec = 0;
            long totalErrors = 0;
            long totalSkipped = 0;
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
            list.add(catMap);
        }
        return list;
    }

    /**
     * 获取全局统计信息
     */
    public static Map<String, Object> getStats() {
        long totalJobs = jobInfoMap.size();
        long enabledJobs = 0;
        long disabledJobs = 0;
        long totalExec = 0;
        long totalErrors = 0;
        long totalSkipped = 0;
        long totalTimeouts = 0;
        long runningCount = 0;
        long autoDisabledCount = 0;
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
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("globalPaused", globalPaused);
        stats.put("totalJobs", totalJobs);
        stats.put("enabledJobs", enabledJobs);
        stats.put("disabledJobs", disabledJobs);
        stats.put("runningCount", runningCount);
        stats.put("autoDisabledCount", autoDisabledCount);
        stats.put("totalExecutions", totalExec);
        stats.put("totalErrors", totalErrors);
        stats.put("totalSkipped", totalSkipped);
        stats.put("totalTimeouts", totalTimeouts);
        stats.put("totalCategories", CATEGORY_DISPLAY.size());
        stats.put("activeCategoryCount", activeCategoryCount);
        return stats;
    }

    // ========== 原有代码 ==========

    public Set<String> getDisabledJobIds() {
        return Collections.unmodifiableSet(disabledJobIds);
    }

    @Override
    public void must() {
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
        default void runJob() {
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

    // ========== 定时任务执行方法（带统计跟踪） ==========

    public static void runOneSecondJob() {
        for (Job job : oneSecondJobList) {
            executeJob(job, "OneSecond", () -> {
                if (job instanceof OneSecond) ((OneSecond) job).onOneSecond();
                else job.runJob();
            });
        }
    }

    public static void runThreeSecondJob() {
        for (Job job : threeSecondJobList) {
            executeJob(job, "ThreeSecond", () -> {
                if (job instanceof ThreeSecond) ((ThreeSecond) job).onThreeSecond();
                else job.runJob();
            });
        }
    }

    public static void runFiveSecondJob() {
        for (Job job : fiveSecondJobList) {
            executeJob(job, "FiveSecond", () -> {
                if (job instanceof FiveSecond) ((FiveSecond) job).onFiveSecond();
                else job.runJob();
            });
        }
    }

    public static void runTenSecondJob() {
        for (Job job : tenSecondJobList) {
            executeJob(job, "TenSecond", () -> {
                if (job instanceof TenSecond) ((TenSecond) job).onTenSecond();
                else job.runJob();
            });
        }
    }

    public static void runThirtySecondJob() {
        for (Job job : thirtySecondJobList) {
            executeJob(job, "ThirtySecond", () -> {
                if (job instanceof ThirtySecond) ((ThirtySecond) job).onThirtySecond();
                else job.runJob();
            });
        }
    }

    public static void runOneMinuteJob() {
        for (Job job : oneMinuteJobList) {
            executeJob(job, "OneMinute", () -> {
                if (job instanceof OneMinute) ((OneMinute) job).onOneMinute();
                else job.runJob();
            });
        }
    }

    public static void runFiveMinuteJob() {
        for (Job job : fiveMinuteJobList) {
            executeJob(job, "FiveMinute", () -> {
                if (job instanceof FiveMinute) ((FiveMinute) job).onFiveMinute();
                else job.runJob();
            });
        }
    }

    public static void runTenMinuteJob() {
        for (Job job : tenMinuteJobList) {
            executeJob(job, "TenMinute", () -> {
                if (job instanceof TenMinute) ((TenMinute) job).onTenMinute();
                else job.runJob();
            });
        }
    }

    public static void runThirtyMinuteJob() {
        for (Job job : thirtyMinuteJobList) {
            executeJob(job, "ThirtyMinute", () -> {
                if (job instanceof ThirtyMinute) ((ThirtyMinute) job).onThirtyMinute();
                else job.runJob();
            });
        }
    }

    public static void runOneHourJob() {
        for (Job job : oneHourJobList) {
            executeJob(job, "OneHour", () -> {
                if (job instanceof OneHour) ((OneHour) job).onOneHour();
                else job.runJob();
            });
        }
    }

    public static void runTenHourJob() {
        for (Job job : tenHourJobList) {
            executeJob(job, "TenHour", () -> {
                if (job instanceof TenHour) ((TenHour) job).onTenHour();
                else job.runJob();
            });
        }
    }

    public static void runThirtyHourJob() {
        for (Job job : thirtyHourJobList) {
            executeJob(job, "ThirtyHour", () -> {
                if (job instanceof ThirtyHour) ((ThirtyHour) job).onThirtyHour();
                else job.runJob();
            });
        }
    }

    public static void runOneDayJob() {
        for (Job job : oneDayJobList) {
            executeJob(job, "OneDay", () -> {
                if (job instanceof OneDay) ((OneDay) job).onOneDay();
                else job.runJob();
            });
        }
    }

    public static void runOneWeekJob() {
        for (Job job : oneWeekJobList) {
            executeJob(job, "OneWeek", () -> {
                if (job instanceof OneWeek) ((OneWeek) job).onOneWeek();
                else job.runJob();
            });
        }
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
