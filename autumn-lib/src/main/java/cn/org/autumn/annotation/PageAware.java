package cn.org.autumn.annotation;

import org.springframework.core.annotation.AliasFor;

import javax.validation.constraints.NotEmpty;
import java.lang.annotation.*;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface PageAware {

    String resource() default "";

    @AliasFor(attribute = "resource")
    String url() default "";

    String page() default "";

    String channel() default "0";

    String product() default "0";

    boolean login() default false;
}
