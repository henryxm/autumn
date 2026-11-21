package cn.org.autumn.table.annotation;

import java.lang.annotation.*;

/**
 * 表索引声明（注解驱动建表 / 同步），或 DaoSql Provider 上的 Query→Index <strong>发现标记</strong>。
 * <p>
 * <b>怎么选写法</b>
 * <ul>
 *   <li><b>单列（Entity 字段）</b>：标在字段上 {@code @Index}（默认索引本字段、ASC）。需要方向/前缀用
 *       {@code @Index(order = IndexOrder.DESC)} / {@code @Index(length = n)}，不必再写 {@code fields}。</li>
 *   <li><b>组合 / FULLTEXT 等（Entity 类）</b>：类上 {@link Indexes} + {@code @Index(fields = { @IndexField... })}，
 *       数组顺序即落库列序；每列方向用 {@link IndexField#order()}。</li>
 *   <li><b>Query→Index（DaoSql 方法）</b>：在无参 {@code public String} Provider 上标<strong>空</strong>
 *       {@code @Index} 仅作启动发现白名单；索引列序/UNIQUE/name 等由 {@code IndexedSelect} 声明。
 *       方法上的 {@code fields}/{@code name}/… <strong>不解析</strong>。见 docs/AI_INDEX_ANNOTATIONS.md §1。</li>
 *   <li>已 {@link Column#isUnique()} 的字段禁止再标字段级 {@code @Index}（见 docs/AI_STANDARDS.md §10.2）。</li>
 * </ul>
 * 字段级 {@code order}/{@code length}/{@code collation} 仅当标在<strong>字段</strong>上且 {@link #fields()} 为空时作用于本列；
 * 类级请用 {@link IndexField}。METHOD 目标下属性载荷忽略。
 */
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.ANNOTATION_TYPE, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface Index {

    /**
     * 索引物理名。推荐小写压缩列语义（如 {@code usertagplace}），可无下划线。
     * <ul>
     *   <li>可空：字段级默认用本列 underline 名；组合按列规格派生后再截断。</li>
     *   <li>超长（默认 64）启动时截断并 WARN，不阻断初始化。</li>
     *   <li>仅改名、列序/方向不变：按定义指纹配对后 {@code RENAME INDEX}（见
     *       {@code docs/AI_INDEX_ANNOTATIONS.md} §7）。</li>
     * </ul>
     */
    String name() default "";

    /**
     * 组合索引列定义（有序）。单列字段级快捷时保持默认空数组。
     */
    IndexField[] fields() default {};

    IndexTypeEnum indexType() default IndexTypeEnum.NORMAL;

    IndexMethodEnum indexMethod() default IndexMethodEnum.BTREE;

    String comment() default "";

    /**
     * 部分索引条件（PG / SQL Server / SQLite 等）；MySQL 等不支持时 WARN 并忽略。
     */
    String where() default "";

    /**
     * 覆盖列 / INCLUDE（PG、SQL Server）；不支持时 WARN 并忽略。
     */
    String[] include() default {};

    /** FULLTEXT parser 名，如 ngram。 */
    String parser() default "";

    /** MySQL 8+ INVISIBLE；不支持时忽略。 */
    boolean invisible() default false;

    /**
     * 单表达式快捷（{@link #fields()} 为空时），如 {@code lower(email)}；可与 {@link #order()} 联用。
     */
    String expression() default "";

    /**
     * 字段级单列快捷：本列方向。仅当标在字段上且 {@link #fields()} 为空时生效；默认 ASC。
     * 类级 {@code @Index}/{@code @Indexes} 请用 {@link IndexField#order()}。
     */
    IndexOrder order() default IndexOrder.ASC;

    /**
     * 字段级单列快捷：索引前缀长度。0 表示按 {@link Column#length()} / {@code IndexPrefixRules} 收敛；
     * 仅当标在字段上且 {@link #fields()} 为空时生效。
     */
    int length() default 0;

    /**
     * 字段级单列快捷：列级 collation（模型已收集；各方言 DDL 发射未统一，优先用 {@link Column#collation()}）。
     * 仅字段级且 {@link #fields()} 为空时生效。
     */
    String collation() default "";
}
