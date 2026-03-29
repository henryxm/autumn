package cn.org.autumn.table.annotation;

import java.lang.annotation.*;

/**
 * 表索引声明。若某列已在对应字段上使用 {@link Column#isUnique()}，则以 {@code isUnique} 为准：
 * 字段级 {@code @Index} 会被忽略；类级 {@code fields} 中含该列时会在 {@link cn.org.autumn.table.data.TableInfo} 中剔除并打告警。
 */
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.ANNOTATION_TYPE, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface Index {

    String name() default "";

    IndexField[] fields() default {};

    IndexTypeEnum indexType() default IndexTypeEnum.NORMAL;

    IndexMethodEnum indexMethod() default IndexMethodEnum.BTREE;

    String comment() default "";
}
