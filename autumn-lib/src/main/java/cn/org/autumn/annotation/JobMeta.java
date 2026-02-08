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
 * @JobMeta(name = "防御护盾", group = "security")
 * public class ShieldService implements LoopJob.FiveSecond, LoopJob.OneMinute, LoopJob.OneDay {
 *
 *     @Override
 *     @JobMeta(name = "清空访问记录", skipIfRunning = true)
 *     public void onFiveSecond() {
 *         visit.clear();
 *     }
 *
 *     @Override
 *     @JobMeta(name = "刷新URI规则", timeout = 5000)
 *     public void onOneMinute() {
 *         uris = null;
 *     }
 *
 *     @Override
 *     @JobMeta(name = "清空IP白名单", maxConsecutiveErrors = 3)
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
}
