package cn.org.autumn.annotation;

import java.lang.annotation.*;

/**
 * {@link Cache} 的容器注解，用于在同一类型上声明多个缓存索引配置。
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Caches {
    /**
     * 缓存配置数组。
     */
    Cache[] value();
}
