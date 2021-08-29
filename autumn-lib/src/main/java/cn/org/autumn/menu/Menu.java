package cn.org.autumn.menu;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Menu {
    String name();

    String namespace() default "";

    String key() default "";

    int order() default 0;

    String ico() default "fa-file-code-o";
}