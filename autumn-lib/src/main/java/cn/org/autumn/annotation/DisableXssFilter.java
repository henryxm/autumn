package cn.org.autumn.annotation;

import java.lang.annotation.*;

/**
 * 禁用XSS过滤注解
 * 在Controller方法上添加此注解，可以跳过XSS过滤
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DisableXssFilter {

    /**
     * 是否禁用XSS过滤
     *
     * @return true-禁用XSS过滤，false-启用XSS过滤
     */
    boolean value() default true;

    /**
     * 描述信息
     *
     * @return 描述
     */
    String description() default "";
}
