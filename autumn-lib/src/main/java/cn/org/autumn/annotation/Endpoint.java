package cn.org.autumn.annotation;

import java.lang.annotation.*;

/**
 * 加密端点元数据注解。
 * <p>
 * 用于声明接口在加解密体系中的行为，可标注在类、方法、参数上。
 * 典型用途：
 * <ul>
 *   <li>控制端点是否出现在 {@code /rsa/api/v1/endpoints} 列表</li>
 *   <li>约束请求/响应是否必须携带加密会话并走密文链路</li>
 *   <li>声明普通返回对象是否支持“兼容加密”封装</li>
 * </ul>
 * <p>
 * 层级优先级：方法级配置优先于类级配置。
 * 例如类上 {@code hidden=true}，方法上 {@code hidden=false}，则该方法最终可见。
 *
 * @author Autumn
 */
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Endpoint {
    /**
     * 是否隐藏该端点。
     * <p>
     * {@code true}：接口不会出现在 endpoints 列表中。<br>
     * {@code false}：接口默认可见。
     *
     * @return 是否隐藏
     */
    boolean hidden() default false;

    /**
     * 强制加密开关。
     * <p>
     * 作用在参数（通常是请求体参数）上：请求必须携带有效加密内容与会话。<br>
     * 作用在方法上：响应必须走加密输出，缺少会话时抛出异常。
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
