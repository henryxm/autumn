package cn.org.autumn.annotation;

import java.lang.annotation.*;

/**
 * 定时任务服务器分配注解
 * <p>
 * 用于指定定时任务应该在哪些服务器上执行。通过环境变量 {@code server.tag}
 * 读取当前服务器的标签，与注解中指定的标签进行匹配，只有匹配的服务器才会执行该任务。
 * <p>
 * 支持两种标注方式：
 * <ul>
 *   <li><b>方法级别</b>（推荐）—— 标注在 onFiveSecond()、onOneMinute() 等方法上，
 *       对该分类任务单独配置，优先级高于类级别</li>
 *   <li><b>类级别</b> —— 标注在实现类上，作为该类所有任务的默认配置</li>
 * </ul>
 * <p>
 * 规则：
 * <ul>
 *   <li>未标注此注解 —— 任务在所有服务器上运行（默认行为）</li>
 *   <li>标注了此注解 —— 只在标签匹配的服务器上运行</li>
 *   <li>服务器标签为 {@code *} —— 运行所有任务（忽略分配限制）</li>
 *   <li>数据库中的分配配置优先于注解配置（注解值作为默认值）</li>
 * </ul>
 * <p>
 * 使用示例：
 * <pre>{@code
 * @Service
 * @JobAssign({"master"})  // 该类所有任务默认只在 master 服务器上运行
 * public class DataSyncService implements LoopJob.FiveMinute, LoopJob.OneHour {
 *
 *     @Override
 *     @JobAssign({"master", "backup"})  // 方法级别覆盖类级别
 *     public void onFiveMinute() {
 *         syncData();
 *     }
 *
 *     @Override
 *     public void onOneHour() {  // 使用类级别配置: master
 *         fullSync();
 *     }
 * }
 * }</pre>
 *
 * @see JobMeta
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface JobAssign {

    /**
     * 允许执行此任务的服务器标签列表
     * <p>
     * 与环境变量 {@code server.tag} 的值进行匹配（不区分大小写）。
     * 空数组表示在所有服务器上运行（默认）。
     * <p>
     * 示例：{@code @JobAssign({"master", "worker-1"})}
     */
    String[] value() default {};
}
