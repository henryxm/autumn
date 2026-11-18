package cn.org.autumn.table.annotation;

import java.lang.annotation.*;

/**
 * 组合（或多列）索引中的一列，用于 {@link Index#fields()}。
 * <p>
 * <b>数组顺序 = 落库列序</b>。单列索引请用字段上的 {@link Index}，不要为本字段再套一层
 * {@code @IndexField(field = "本字段")}。
 */
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface IndexField {
    /**
     * Java 属性名（转下划线列名）。与 {@link #expression()} 二选一；表达式优先。
     */
    String field() default "";

    /** 索引前缀长度；0 表示整列（再经 IndexPrefixRules 按列类型收敛）。 */
    int length() default 0;

    /** 列排序方向，默认 ASC。应对齐该索引服务的主查询 ORDER BY。 */
    IndexOrder order() default IndexOrder.ASC;

    /**
     * 表达式索引列（如 {@code lower(email)}）。非空时忽略 {@link #field()}。
     */
    String expression() default "";

    /**
     * 列级 collation（模型已收集；各方言 DDL 发射未统一，优先用字段 {@link Column#collation()}）。
     */
    String collation() default "";
}
