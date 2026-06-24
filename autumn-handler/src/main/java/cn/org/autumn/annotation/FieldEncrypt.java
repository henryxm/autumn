package cn.org.autumn.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记实体 {@code String} 字段：落库加密、读库解密（存储 at-rest，与 HTTP 传输加密无关）。
 * <p>
 * 完整说明与易混概念见 {@code docs/AI_FIELD_ENCRYPT.md} §0。
 * <p>
 * <strong>Service 纪律</strong>：带本注解的实体，其模块 Service 须继承
 * {@code cn.org.autumn.base.EncryptModuleService}；{@code baseMapper} / Dao 手写 SQL 须 {@code afterRead(...)} 等（见 {@code docs/AI_FIELD_ENCRYPT.md} §0.5）。
 *
 * @see #vector()
 * @see #searchable()
 * @see #hashField()
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface FieldEncrypt {

    /**
     * GCM 初始化向量（IV），Base64 编码的 <strong>12 字节</strong>。
     * <ul>
     *   <li><strong>空（默认，生产推荐）</strong>：每次加密随机 IV；IV 与密文打包写入 {@code ENC$v1$...}，
     *       解密时从密文字符串拆出，<em>不依赖</em>本注解值。</li>
     *   <li><strong>非空</strong>：固定 IV，同一明文产生相同密文；仅建议开发/联调，<em>不能</em>替代 {@link #searchable()}。</li>
     * </ul>
     * 与 {@link #hashField()} 无关：IV 只作用于<strong>加密列</strong>，不参与盲索引。
     */
    String vector() default "";

    /**
     * 是否启用盲索引等值查询。
     * <ul>
     *   <li>{@code true}：写入加密<strong>开启</strong>时，除加密 {@code mobile} 外，还维护
     *       {@link #hashField()} 指向列的 HMAC（{@code autumn.crypto.field.hash-key}）。</li>
     *   <li>列表条件：{@code BaseService.getCondition} 将 {@code mobile=} 改写为 {@code mobile_hash=}。</li>
     *   <li><strong>不会</strong>自动创建 hash 列；须在实体中<strong>手写</strong> {@code @Column} 字段
     *       （默认名 {@code {本字段名}Hash}）。缺字段则 WARN，hash 不写入、无法盲索引查。</li>
     *   <li>关写入加密或 {@code searchable=false}：<strong>不会</strong>自动 DROP hash 列，仅停止维护/改写查询。</li>
     * </ul>
     */
    boolean searchable() default false;

    /**
     * 盲索引列的 <strong>Java 字段名</strong>（非 DB 列名自动 DDL）。
     * <ul>
     *   <li>空：默认 {@code {加密字段名}Hash}，如 {@code mobile} → {@code mobileHash}。</li>
     *   <li>该字段须在同实体类中显式声明，并带 {@code @Column}，表结构由实体扫描同步。</li>
     *   <li>本属性<strong>不</strong>随运行时写入开关增删；改注解也不触发自动删列。</li>
     * </ul>
     */
    String hashField() default "";
}
