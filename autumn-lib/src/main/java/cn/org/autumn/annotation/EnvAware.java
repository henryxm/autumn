package cn.org.autumn.annotation;

import java.lang.annotation.*;

/**
 * 环境配置注入标记。
 * <p>
 * 标注在字段上，声明该字段值来自配置中心/环境变量中的指定键。
 * 常用于 {@code EnvBean} 这类统一配置载体。
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface EnvAware {
    /**
     * 配置键名，例如 {@code site.domain}、{@code node.tag}。
     */
    String value() default "";
}