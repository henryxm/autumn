package cn.org.autumn.annotation;

import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;

@Target({ElementType.FIELD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SearchType {

    @AliasFor("type")
    String value() default "";

    String type() default "";

    String name() default "";

    String alias() default "";

    String describe() default "";

    int order() default 0;

    boolean admin() default false;

    boolean debug() default false;

    boolean show() default true;
}
