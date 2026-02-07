package cn.org.autumn.config;

import cn.org.autumn.thread.TagTaskExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * 线程池配置 — 基于系统资源（CPU 核数、JVM 堆内存）自动计算合理参数。
 *
 * <h3>设计原则</h3>
 * <ul>
 *   <li><b>动态适配</b>：根据 {@code Runtime.availableProcessors()} 和 {@code Runtime.maxMemory()}
 *       自动计算，无需为不同服务器手动调参</li>
 *   <li><b>有界队列</b>：必须设置队列容量，否则 {@code maxPoolSize} 完全失效
 *       （{@code ThreadPoolExecutor} 只在队列满后才创建超过核心数的线程）</li>
 *   <li><b>弹性伸缩</b>：启用核心线程超时回收，系统空闲时自动释放资源；
 *       峰值时通过队列缓冲 + 弹性线程扩容应对</li>
 *   <li><b>安全兜底</b>：拒绝策略采用 CallerRunsPolicy（降级为调用方线程执行），
 *       既不丢弃任务，又对调用方产生自然背压</li>
 * </ul>
 *
 * <h3>容量模型</h3>
 * <pre>
 *   任务提交 → [核心线程处理] → [队列缓冲] → [弹性线程扩容] → [拒绝策略兜底]
 *                corePoolSize      queueCapacity    maxPoolSize     CallerRunsPolicy
 * </pre>
 *
 * <h3>参数可覆盖</h3>
 * <p>在 {@code application.yml} 中通过以下配置覆盖自动计算值（设为 0 或不配置则使用自动值）：</p>
 * <pre>
 * autumn:
 *   thread:
 *     core-pool-size: 0       # 0=自动（CPU核数*2，范围[4,64]）
 *     max-pool-size: 0        # 0=自动（core*3，受内存约束）
 *     queue-capacity: 0       # 0=自动（core*10）
 *     keep-alive-seconds: 60  # 非核心线程空闲存活时间
 * </pre>
 */
@Slf4j
@Configuration
public class ThreadConfig {
    // ======================== 可配置参数（application.yml 覆盖） ========================

    /**
     * 核心线程数：0=自动计算
     */
    @Value("${autumn.thread.core-pool-size:0}")
    private int configuredCorePoolSize;

    /**
     * 最大线程数：0=自动计算
     */
    @Value("${autumn.thread.max-pool-size:0}")
    private int configuredMaxPoolSize;

    /**
     * 队列容量：0=自动计算
     */
    @Value("${autumn.thread.queue-capacity:0}")
    private int configuredQueueCapacity;

    /**
     * 空闲线程存活时间（秒）
     */
    @Value("${autumn.thread.keep-alive-seconds:60}")
    private int keepAliveSeconds;

    // ======================== 安全边界常量 ========================

    /**
     * 核心线程数下限 — 保证最低并发度，即使单核机器也能满足基本需求
     */
    private static final int MIN_CORE = 4;

    /**
     * 核心线程数上限 — 防止过多核心线程导致上下文切换开销
     */
    private static final int MAX_CORE = 64;

    /**
     * 最大线程数绝对上限 — 防止线程过多导致 OOM（每线程约占 1MB 栈空间）
     */
    private static final int ABSOLUTE_MAX_POOL = 500;

    /**
     * 线程池内存预算占比 — 线程栈空间不超过堆内存的此比例
     */
    private static final double MEMORY_BUDGET_RATIO = 0.15;

    /**
     * 每线程预估栈空间（MB），对应 JVM 默认 -Xss512k~1m
     */
    private static final int THREAD_STACK_MB = 1;

    @Bean
    public TagTaskExecutor asyncTaskExecutor(List<RejectedHandler> rejectedHandlers) {
        // ================================================================
        // 1. 探测系统资源
        // ================================================================
        int cpuCores = Runtime.getRuntime().availableProcessors();
        long maxHeapBytes = Runtime.getRuntime().maxMemory();
        long maxHeapMB = maxHeapBytes / (1024 * 1024);
        // ================================================================
        // 2. 计算核心线程数
        //    任务特征：IO 密集型为主（数据库、Redis、HTTP、文件IO），混合少量 CPU 计算
        //    经验公式：corePoolSize = CPU核数 × 2
        //    直觉：IO 等待期间 CPU 空闲，双倍核数可充分利用等待间隙
        // ================================================================
        int coreSize = configuredCorePoolSize > 0 ? configuredCorePoolSize : clamp(cpuCores * 2, MIN_CORE, MAX_CORE);
        // ================================================================
        // 3. 计算最大线程数
        //    目的：应对突发峰值（如整点大量定时任务同时触发）
        //    公式：core × 3，但受内存上限约束
        //    内存约束：线程栈总空间 ≤ 堆内存 × 15%
        //      例：堆 512MB → 线程上限 ≈ 512 × 0.15 / 1 = 76
        //      例：堆 2GB  → 线程上限 ≈ 2048 × 0.15 / 1 = 307
        // ================================================================
        int memoryBasedLimit = (int) (maxHeapMB * MEMORY_BUDGET_RATIO / THREAD_STACK_MB);
        int maxSize = configuredMaxPoolSize > 0 ? configuredMaxPoolSize : clamp(coreSize * 3, coreSize, Math.min(memoryBasedLimit, ABSOLUTE_MAX_POOL));
        // ================================================================
        // 4. 计算队列容量
        //    ★ 关键：必须使用有界队列！★
        //    原因：ThreadPoolExecutor 的执行顺序是：
        //      (1) < corePoolSize → 创建核心线程
        //      (2) 核心线程满 → 放入队列
        //      (3) 队列满 → 创建非核心线程（直到 maxPoolSize）
        //      (4) 全满 → 触发拒绝策略
        //    如果队列无界（默认 Integer.MAX_VALUE），步骤(3)永远不会发生，
        //    maxPoolSize 形同虚设，所有任务只由 corePoolSize 个线程处理。
        //
        //    队列大小取 core × 10：
        //    - 足以缓冲短时突发（如整点批量调度）
        //    - 不至于过大导致任务积压和响应延迟
        // ================================================================
        int queueCapacity = configuredQueueCapacity > 0 ? configuredQueueCapacity : coreSize * 10;
        // ================================================================
        // 5. 创建线程池
        // ================================================================
        TagTaskExecutor executor = new TagTaskExecutor();
        // 线程名前缀 — 方便日志和 jstack 中识别线程归属
        executor.setThreadNamePrefix("Executor-");
        // 核心线程数：常驻线程，任务到来时优先使用
        executor.setCorePoolSize(coreSize);
        // 最大线程数：队列满后弹性扩容上限
        executor.setMaxPoolSize(maxSize);
        // 有界队列容量：核心线程满后的缓冲区
        executor.setQueueCapacity(queueCapacity);
        // 非核心线程空闲存活时间：超时后回收，释放资源
        executor.setKeepAliveSeconds(keepAliveSeconds);
        // ★ 允许核心线程超时回收 ★
        // 系统空闲时（如凌晨无定时任务），核心线程也会被回收至 0
        // 新任务到来时自动重建，适合非持续满负荷的业务场景
        // 代价：空闲后首个任务有微量延迟（创建线程），对定时任务系统影响可忽略
        executor.setAllowCoreThreadTimeOut(true);
        // ★ 优雅关闭 ★
        // 应用停止时等待正在执行的任务完成，避免任务被中途打断
        // 最多等待 30 秒，超时后强制终止
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        // ================================================================
        // 6. 拒绝策略
        //    当核心线程满 + 队列满 + 弹性线程满 时触发
        //    策略：CallerRunsPolicy — 由提交任务的线程直接执行
        //    优势：(1) 不丢弃任务 (2) 自然背压，减缓任务提交速度
        //    风险：会阻塞提交线程（如 Tomcat 请求线程），但优于丢任务
        // ================================================================
        if (null != rejectedHandlers && !rejectedHandlers.isEmpty()) {
            // 使用自定义拒绝处理器链（如：告警、降级、重试等）
            executor.setRejectedExecutionHandler((r, pool) -> {
                TagTaskExecutor.recordRejected();
                if (log.isDebugEnabled())
                    log.debug("线程池已饱和，触发自定义拒绝策略: active={}/{}, queue={}", pool.getActiveCount(), pool.getMaximumPoolSize(), pool.getQueue().size());
                for (RejectedHandler handler : rejectedHandlers) {
                    handler.rejectedExecution(r, pool);
                }
            });
        } else {
            // 默认兜底：CallerRunsPolicy + 记录统计 + 日志告警
            executor.setRejectedExecutionHandler((r, pool) -> {
                if (pool.isShutdown()) {
                    return; // 线程池已关闭，静默丢弃
                }
                TagTaskExecutor.recordRejected();
                if (log.isDebugEnabled())
                    log.debug("线程池已饱和，降级为调用方线程执行: active={}/{}, queue={}", pool.getActiveCount(), pool.getMaximumPoolSize(), pool.getQueue().size());
                r.run(); // CallerRunsPolicy：在提交线程中直接执行
            });
        }
        // ================================================================
        // 7. 输出配置摘要
        // ================================================================
        if (log.isDebugEnabled())
            log.debug("线程池已初始化 — core={}, max={}, queue={}, keepAlive={}s, coreTimeout=true" + " | 系统: cpu={}, heap={}MB, memLimit={}threads" + " | 来源: core={}, max={}, queue={}",
                    coreSize, maxSize, queueCapacity, keepAliveSeconds,
                    cpuCores, maxHeapMB, memoryBasedLimit,
                    configuredCorePoolSize > 0 ? "yml配置" : "自动计算",
                    configuredMaxPoolSize > 0 ? "yml配置" : "自动计算",
                    configuredQueueCapacity > 0 ? "yml配置" : "自动计算");
        return executor;
    }

    /**
     * 将值限制在 [min, max] 范围内
     */
    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}