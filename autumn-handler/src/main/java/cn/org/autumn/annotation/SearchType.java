package cn.org.autumn.annotation;

import java.lang.annotation.*;
import org.springframework.core.annotation.AliasFor;

@Target({ElementType.FIELD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SearchType {

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
