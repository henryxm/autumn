package cn.org.autumn.annotation;

import java.lang.annotation.*;

@Target({ElementType.FIELD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface NoEmoji {
    String value() default "";

    String name() default "";
}
