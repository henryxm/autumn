package cn.org.autumn.annotation;

import java.lang.annotation.*;

@Target({ElementType.FIELD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ConfigParam {
    String paramKey() default "";

    String category() default "";

    String name() default "";

    String description() default "";
}