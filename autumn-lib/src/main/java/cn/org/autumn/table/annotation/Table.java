package cn.org.autumn.table.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * 创建表时的表名
 *
 * @author Shaohua
 * @version 2018年8月20日 下午2:11:30
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface Table {

    /**
     * 表名
     */
    String value() default "";

    /**
     * 表前缀
     */
    String prefix() default "";

    /**
     * 表注释
     */
    String comment() default "";

    /**
     * 表的引擎
     */
    String engine() default "InnoDB";

    /**
     * 表字符编码
     */
    String charset() default "utf8";

    //模块名
    String module() default "";
}
