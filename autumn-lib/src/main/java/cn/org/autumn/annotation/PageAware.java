package cn.org.autumn.annotation;

import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;

/**
 * 页面/SPM 元数据注解。
 * <p>
 * 标注在站点定义类字段上，用于描述页面所属的 site/page/channel/product 维度，
 * 以及资源路径、URL 别名和是否需要登录。框架会据此生成/匹配 SPM 映射关系。
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface PageAware {

    /**
     * 资源标识（resourceId）。
     */
    String resource() default "";

    /**
     * {@link #resource()} 的别名。
     */
    @AliasFor(attribute = "resource")
    String url() default "";

    /**
     * 页面标识（pageId）。
     */
    String page() default "";

    /**
     * 渠道标识（channelId），默认 "0"。
     */
    String channel() default "0";

    /**
     * 产品标识（productId），默认 "0"。
     */
    String product() default "0";

    /**
     * 是否需要登录访问该页面。
     */
    boolean login() default false;
}
