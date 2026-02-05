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

    /**
     * 当有多个不同的字段都有唯一值的时候，可以使用name来区分
     *
     * @return 返回一个指定字段的名字
     */
    String name() default "";

    /**
     * 指明缓存的数据是否具有唯一性，如果不是唯一的，应返回数据格式
     *
     * @return 是否是唯一的缓存对象，否则返回数据
     */
    boolean unique() default true;
}
