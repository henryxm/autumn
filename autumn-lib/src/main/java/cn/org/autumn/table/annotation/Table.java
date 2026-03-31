package cn.org.autumn.table.annotation;

import cn.org.autumn.table.annotation.sql.CharacterSet;
import cn.org.autumn.table.annotation.sql.Collation;
import cn.org.autumn.table.annotation.sql.Engine;

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
     * 物理表名；留空时依次尝试：MyBatis-Plus {@code @TableName} 的值 → 再按 {@link #prefix()} 与类名推导（去 {@code Entity} 后缀、驼峰转下划线）。
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
     * 存储引擎（语义见 {@link Engine}；非 MySQL 方言可能忽略或另行映射）。
     */
    Engine engine() default Engine.INNODB;

    /**
     * 表默认字符集（建表 {@code DEFAULT CHARACTER SET}，更新表时若与库中不一致则 {@code CONVERT TO}，主实现为 MySQL/MariaDB）。
     * <p>请勿使用 {@link CharacterSet#INHERIT}；若误用，框架将按 {@link CharacterSet#UTF8} 处理。
     */
    CharacterSet charset() default CharacterSet.UTF8;

    /**
     * 表默认排序规则；{@link Collation#INHERIT} 表示不写 COLLATE，由服务器按字符集默认处理。
     */
    Collation collation() default Collation.INHERIT;

    //模块名
    String module() default "";
}
