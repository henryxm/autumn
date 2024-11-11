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

    //线程加锁，未获取到锁，不执行
    boolean lock() default false;

    //锁定时间，单位分钟
    long time() default 10;
}
