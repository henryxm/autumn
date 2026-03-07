package cn.org.autumn.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * JSON Map 映射声明。
 * <p>
 * 标注在类型上，声明该类型按 Map 结构解析/封装时的目标元素类型。
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface JsonMap {
    /**
     * Map 值的目标类型，默认 {@link Object}。
     */
    Class<?> type() default Object.class;
}
