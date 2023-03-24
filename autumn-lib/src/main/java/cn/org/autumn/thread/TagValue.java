package cn.org.autumn.thread;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface TagValue {
    Class<?> type() default String.class;

    String method() default "";

    String tag() default "";

    String name() default "";
}
