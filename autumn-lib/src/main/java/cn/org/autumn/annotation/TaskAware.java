package cn.org.autumn.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface TaskAware {
    String params() default "";

    String cronExpression() default "0 0/30 * * * ?";

    int status() default 0;

    String remark() default "定时任务说明";

    String mode() default "prod,test";
}
