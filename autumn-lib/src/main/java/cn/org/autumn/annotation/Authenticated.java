package cn.org.autumn.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 认证上下文注入/校验标记。
 * <p>
 * 可标注在参数、方法、类上，表示当前入口需要认证上下文参与处理。
 * 具体解析逻辑由认证参数解析器与拦截链实现。
 */
@Target({ElementType.PARAMETER, ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Authenticated {
    /**
     * 是否要求认证结果非空。
     * <p>
     * true：认证信息缺失时按异常处理；false：允许匿名上下文继续执行。
     */
    boolean notNull() default true;
}