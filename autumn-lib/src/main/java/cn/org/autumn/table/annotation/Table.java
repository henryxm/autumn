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
     * 表注释（会写入库表 COMMENT 与生成实体等）。
     * <p>与 {@link Column#comment()} 相同：若含英文冒号 {@code :}，{@code BaseService#getLanguageItemsInternal()} /
     * {@code getMenuItemsInternal()} 等对<strong>第一个冒号之前</strong>作为菜单/多语言展示短标题，之后为详述。
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
