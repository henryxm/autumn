package cn.org.autumn.annotation;

import java.lang.annotation.*;

/**
 * 定时任务元数据注解
 * <p>
 * 用于标注实现了 LoopJob 接口的类或方法，提供任务名称、描述、分组、
 * 优先级、防重入、超时告警、连续错误自动禁用等控制能力。
 * <p>
 * 支持两种标注方式：
 * <ul>
 *   <li><b>方法级别</b>（推荐）—— 标注在 onFiveSecond()、onOneMinute() 等方法上，
 *       对该分类任务单独配置，优先级高于类级别</li>
 *   <li><b>类级别</b> —— 标注在实现类上，作为该类所有任务的默认配置</li>
 * </ul>
 * <p>
 * 使用示例：
 * <pre>{@code
 * @Service
 * @JobMeta(name = "防御护盾", group = "security", assign = {"master"})
 * public class ShieldService implements LoopJob.FiveSecond, LoopJob.OneMinute, LoopJob.OneDay {
 *
 *     @Override
 *     @JobMeta(name = "清空访问记录", skipIfRunning = true)
 *     public void onFiveSecond() {
 *         visit.clear();
 *     }
 *
 *     @Override
 *     @JobMeta(name = "刷新URI规则", timeout = 5000, assign = {"master", "backup"})
 *     public void onOneMinute() {
 *         uris = null;
 *     }
 *
 *     @Override
 *     @JobMeta(name = "清空IP白名单", maxConsecutiveErrors = 3)
 *     public void onOneDay() {  // 使用类级别 assign: master
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
     * 默认 true（不跳过，允许并发触发）。
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
     * 异步执行：任务提交到 TagTaskExecutor 线程池异步执行，不阻塞调度线程
     * <p>
     * 适用于执行耗时较长、不需要阻塞调度周期的任务。
     * 异步任务由 TagTaskExecutor 统一管理，支持任务追踪、中断、堆栈诊断等能力。
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
