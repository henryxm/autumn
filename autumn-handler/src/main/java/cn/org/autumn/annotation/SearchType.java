package cn.org.autumn.annotation;

import java.lang.annotation.*;

@Target({ElementType.FIELD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SearchType {

    String value() default "";

    String name() default "";

    String alias() default "";

    String describe() default "";

    boolean show() default true;
}
