package cn.org.autumn.annotation;

import cn.org.autumn.job.JobDuty;

import java.lang.annotation.*;

/**
 * 定时任务治理元数据注解。
 * <p>
 * 用于标注实现 {@code LoopJob.*} 的类或方法，为任务提供可观测、可治理、可分配的运行参数：
 * 展示名、分组、优先级、防重入、超时观测、连续错误自动禁用、服务器分配、延迟与异步执行、
 * 集群职责 {@link #duty()}。
 * <p>
 * 标注层级与覆盖规则：
 * <ul>
 *   <li><b>类级别</b>：作为该 Bean 所实现的<strong>每一个</strong> {@code LoopJob.*} 周期的默认配置</li>
 *   <li><b>方法级别</b>：标在 {@code onXxx()} 上，覆盖对应周期（如 {@code onOneMinute()}）</li>
 *   <li><b>duty</b>：方法级仅当显式非 {@link JobDuty#ALL} 时覆盖类级；仅写 {@code name} 等不会把类级 SINGLETON 打回 ALL</li>
 * </ul>
 * 同一类实现多个周期接口时，每个周期独立 {@code JobInfo}（id={@code Category|FQCN}）；详见 {@code docs/AI_CLUSTER_JOB_ORCHESTRATION.md} §1.3。
 * 建议：共用 duty/lock/group 放类级；周期差异（含不同 duty）放方法级；「多数 ALL、少数 SINGLETON」用类缺省 ALL + 方法级非 ALL。
 * <p>
 * 使用示例（多周期 + 差异 duty）：
 * <pre>{@code
 * @Service
 * @JobMeta(name = "防御护盾", group = "security", assign = {"master"})
 * public class ShieldService implements LoopJob.FiveSecond, LoopJob.OneMinute, LoopJob.OneDay {
 *
 *     @Override
 *     @JobMeta(name = "清空访问记录", skipIfRunning = true) // duty 沿用类级缺省 ALL
 *     public void onFiveSecond() {
 *         visit.clear();
 *     }
 *
 *     @Override
 *     @JobMeta(name = "刷新URI规则", duty = JobDuty.SINGLETON, lock = "wall:uris", timeout = 5000)
 *     public void onOneMinute() {
 *         uris = null;
 *     }
 *
 *     @Override
 *     @JobMeta(name = "清空IP白名单", maxConsecutiveErrors = 3) // 沿用类级 assign；duty 仍为 ALL
 *     public void onOneDay() {
 *         ips.clear();
 *     }
 * }
 * }</pre>
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface JobMeta {

    /**
     * 任务名称（展示用）
     * <p>
     * 未设置时自动使用 "类名.方法名" 格式
     */
    String name() default "";

    /**
     * 任务描述
     */
    String description() default "";

    /**
     * 任务分组标签（用于管理界面分组展示和筛选）
     */
    String group() default "";

    /**
     * 执行优先级
     * <p>
     * 值越小优先级越高，同一分类内按此值排序执行。
     * 默认值 100，可通过设置较小的值使任务优先执行。
     */
    int order() default 100;

    /**
     * 防重入：当上次任务尚未执行完成时，跳过本次触发
     * <p>
     * 适用于执行耗时可能超过触发间隔的任务（如1秒任务但执行需2秒），
     * 避免同一个任务实例被并发抢占式执行。
     * <p>
     * 默认 true（开启防重入：正在执行时跳过本次触发）。
     */
    boolean skipIfRunning() default true;

    /**
     * 执行超时时间（毫秒）
     * <p>
     * 当单次执行耗时超过此值时，记录警告日志并计入超时次数。
     * 注意：不会强制中断任务，仅用于监控告警。
     * <p>
     * 0 表示不限制（默认）。
     */
    long timeout() default 0;

    /**
     * 连续错误自动禁用阈值
     * <p>
     * 当连续执行异常达到此数量后，自动禁用该任务，避免持续报错。
     * 被自动禁用的任务可在管理界面手动重新启用。
     * <p>
     * 0 表示不自动禁用（默认）。
     */
    int maxConsecutiveErrors() default 0;

    /**
     * 标签（用于管理界面的灵活筛选和分类）
     * <p>
     * 支持多个标签，例如 {"core", "security", "cleanup"}
     */
    String[] tags() default {};

    /**
     * 允许执行此任务的服务器标签列表（服务器分配）
     * <p>
     * 用于指定定时任务应该在哪些服务器上执行。通过环境变量 {@code server.tag}
     * 读取当前服务器的标签，与注解中指定的标签进行匹配，只有匹配的服务器才会执行该任务。
     * <p>
     * 与环境变量 {@code server.tag} 的值进行匹配（不区分大小写）。
     * 空数组表示在所有服务器上运行（默认）。
     * <p>
     * 规则：
     * <ul>
     *   <li>未设置（空数组） —— 任务在所有服务器上运行（默认行为）</li>
     *   <li>设置了值 —— 只在标签匹配的服务器上运行</li>
     *   <li>服务器标签为 {@code *} —— 运行所有任务（忽略分配限制）</li>
     *   <li>数据库中的分配配置优先于注解配置（注解值作为默认值）</li>
     * </ul>
     * <p>
     * 示例：{@code @JobMeta(name = "数据同步", assign = {"master", "worker-1"})}
     */
    String[] assign() default {};

    /**
     * 集群任务职责；缺省 {@link JobDuty#ALL} 与历史行为完全兼容（本机执行、框架不加集群锁）。
     * <p>
     * 方法级 {@code @JobMeta} 仅当 {@code duty} 为非 {@link JobDuty#ALL} 时覆盖类级，
     * 避免仅写 {@code name} 等把类级 {@code SINGLETON}/{@code SEQUENTIAL} 打回 ALL。
     */
    JobDuty duty() default JobDuty.ALL;

    /**
     * 任务所需节点角色；空数组不过角色闸。仅当本机 {@code node-profile.json} 中 {@code roles} 已手动非空时生效。
     */
    String[] roles() default {};

    /**
     * 分布式锁键（仅 {@link JobDuty#SINGLETON}/{@link JobDuty#SEQUENTIAL}）；空则使用 {@code autumn:job:{jobId}}。
     */
    String lock() default "";

    /**
     * 周期栅栏（仅 {@link JobDuty#SINGLETON}）：同一逻辑周期全集群只跑一次。
     * <p>
     * 用 Redis {@code TIME} 按 LoopJob 分类间隔分桶，{@code SETNX} 占桶后再抢互斥锁。
     * 解决「先到节点跑完释放锁后，同周期晚到节点再次获锁再跑」。
     * <p>
     * 缺省 {@code false}（仅互斥锁，与历史兼容）。方法级缺省 false <b>不会</b>把类级 true 打回；
     * 方法级写 {@code true} 可打开。
     */
    boolean oncePerPeriod() default false;

    /**
     * 延迟执行时间（秒）
     * <p>
     * 当值大于 0 时，任务将通过 TagTaskExecutor 异步提交，并在延迟指定秒数后执行，
     * 不阻塞调度线程。
     * <p>
     * 注意：{@code delay > 0} 会自动强制异步执行（隐含 {@code async=true}）。
     * <p>
     * 0 表示不延迟（默认），任务按正常流程执行。
     * <p>
     * 示例：{@code @JobMeta(name = "数据清理", delay = 10)} 表示延迟 10 秒后异步执行
     */
    int delay() default 0;

    /**
     * 异步执行：将<strong>整段</strong>（含 JobDuty 抢锁/周期栅栏 + 业务）提交到 TagTaskExecutor，不阻塞调度线程。
     * <p>
     * {@link JobDuty#SINGLETON} 仍在<strong>同一 worker 线程</strong>内持锁执行业务（Redisson 锁不可跨线程持有）；
     * 不是「调度线程抢锁后再异步跑业务」。
     * <p>
     * 规则：
     * <ul>
     *   <li>{@code async=true} —— 任务一定异步执行</li>
     *   <li>{@code delay > 0} —— 隐含异步，等同于同时设置 {@code async=true}，并延迟指定秒数后执行</li>
     * </ul>
     * <p>
     * 默认 false（同步执行）。
     */
    boolean async() default false;
}
