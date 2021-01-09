package cn.org.autumn.annotation;

import java.lang.annotation.*;

/**
 * 登录效验
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Login {
}
