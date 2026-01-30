package cn.org.autumn.annotation;

import java.lang.annotation.*;

@Target({ElementType.FIELD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Cache {
    /**
     * 当注解作用在类上时，指定用于缓存的字段名数组
     * 多个字段将组合成复合 key 并且按照顺序进行缓存
     *
     * @return 字段名数组
     */
    String[] value() default {};
}
