package cn.org.autumn.annotation;

import java.lang.annotation.*;

/**
 * 任务触发器元数据注解（Quartz/Cron 维度）。
 * <p>
 * 标注在任务触发方法上，描述调度表达式、运行环境和备注信息。
 * 主要用于调度任务注册与展示，不替代 {@link JobMeta} 的运行治理能力。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface TaskAware {
    /**
     * 任务参数（字符串透传，按业务自行解析）。
     */
    String params() default "";

    /**
     * Cron 表达式，默认每 30 秒触发一次。
     */
    String cronExpression() default "0/30 * * * * ? *";

    /**
     * 任务状态，约定值由调度模块解释（例如 0=启用）。
     */
    int status() default 0;

    /**
     * 任务说明，用于管理界面展示。
     */
    String remark() default "定时任务说明";

    /**
     * 生效环境列表（逗号分隔），如 {@code prod,test} 或 {@code all}。
     */
    String mode() default "prod,test";
}
