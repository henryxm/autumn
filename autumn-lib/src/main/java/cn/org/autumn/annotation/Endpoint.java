package cn.org.autumn.annotation;

import java.lang.annotation.*;

/**
 * 加密端点注解
 * 用于标注加密相关的接口端点
 * 可以标注在类或方法上
 * 当 hidden = true 时，该接口（或整个类）不会出现在 /rsa/api/v1/endpoints 接口的返回列表中
 * 默认情况下（hidden = false 或不标注此注解），接口会出现在列表中
 * 
 * 优先级规则：
 * - 方法级别的注解优先级高于类级别
 * - 如果类级别 hidden = true，但方法级别 hidden = false，则该方法会出现在列表中
 * - 如果类级别 hidden = false，但方法级别 hidden = true，则该方法不会出现在列表中
 *
 * @author Autumn
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Endpoint {
    /**
     * 是否隐藏该端点
     * true: 该接口不会出现在 endpoints 列表中
     * false: 该接口会出现在 endpoints 列表中（默认值）
     *
     * @return 是否隐藏
     */
    boolean hidden() default false;

    /**
     * 隐藏原因（可选）
     * 当 hidden = true 时，用于说明为什么隐藏该端点，便于代码维护
     *
     * @return 隐藏原因
     */
    String reason() default "";
}
