package cn.org.autumn.annotation;

import java.lang.annotation.*;

/**
 * 跳过拦截器处理注解
 * <p>
 * 用于标记控制器方法或类，使其跳过指定拦截器的处理。
 * 特别适用于API接口，避免拦截器在postHandle中添加不必要的变量。
 * <p>
 * 使用示例：
 * <pre>
 * // 跳过所有拦截器
 * {@code @SkipInterceptor}
 * public String apiMethod() { ... }
 *
 * // 跳过指定的拦截器
 * {@code @SkipInterceptor({LanguageInterceptor.class, SpmInterceptor.class})}
 * public String apiMethod() { ... }
 *
 * // 在类级别使用，跳过所有拦截器
 * {@code @SkipInterceptor}
 * @RestController
 * public class ApiController { ... }
 * </pre>
 *
 * @author Autumn
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SkipInterceptor {
    /**
     * 要跳过的拦截器类列表
     * <p>
     * 如果为空数组，表示跳过所有拦截器
     * 如果指定了拦截器类，则只跳过指定的拦截器
     *
     * @return 拦截器类数组
     */
    Class<?>[] value() default {};
}
