package cn.org.autumn.annotation;

import java.lang.annotation.*;

/**
 * 加密端点注解
 * 用于标注加密相关的接口端点
 * 可以标注在类或方法上
 * 当 hidden = true 时，该接口（或整个类）不会出现在 /rsa/api/v1/endpoints 接口的返回列表中
 * 默认情况下（hidden = false 或不标注此注解），接口会出现在列表中
 * <p>
 * 优先级规则：
 * - 方法级别的注解优先级高于类级别
 * - 如果类级别 hidden = true，但方法级别 hidden = false，则该方法会出现在列表中
 * - 如果类级别 hidden = false，但方法级别 hidden = true，则该方法不会出现在列表中
 *
 * @author Autumn
 */
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.PARAMETER})
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
     * 强制加密数据：
     * 如果标注在参数body上，如果请求的body未包含加密内容，或者session为空，则抛出异常
     * 如果标注在接口方法上，则表明返回值必须强制加密，如果session为空，则抛出异常
     *
     * @return 是否强制加密
     */
    boolean force() default false;

    /**
     * 兼容加密返回标记：
     * 当接口返回类型本身不实现 Encrypt 时，可通过该标记声明“返回值支持加密兼容”。
     * 响应是否实际加密仍由请求头（如 X-Encrypt-Session）决定，
     * 老客户端不携带加密头时仍返回原始明文对象。
     *
     * @return 是否支持兼容加密返回
     */
    boolean compatible() default false;

    /**
     * 隐藏原因（可选）
     * 当 hidden = true 时，用于说明为什么隐藏该端点，便于代码维护
     *
     * @return 隐藏原因
     */
    String reason() default "";
}
