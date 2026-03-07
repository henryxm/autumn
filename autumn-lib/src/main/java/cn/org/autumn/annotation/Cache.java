package cn.org.autumn.annotation;

import java.lang.annotation.*;

/**
 * 实体缓存元数据注解。
 * <p>
 * 用于声明实体字段与缓存索引的映射关系，供 {@code BaseCacheService} 系列方法构建缓存键与索引。
 * 支持：
 * <ul>
 *   <li>字段级：标注单个字段作为缓存键</li>
 *   <li>类型级：通过 {@link #value()} 声明复合键字段列表</li>
 * </ul>
 * <p>
 * 与服务方法的关系：
 * <ul>
 *   <li>{@code getCache/getEntity/getNameEntity}：默认不自动创建，除非 {@code create=true}</li>
 *   <li>{@code getCached/getEntitied/getNameEntitied}：默认自动创建（语义等价于 {@code create=true}）</li>
 * </ul>
 */
@Target({ElementType.FIELD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Cache {
    /**
     * 当注解作用在类上时，指定用于缓存的字段名数组
     * 多个字段将组合成复合 key 并且按照顺序进行缓存
     *
     * @return 字段名数组
     */
    String[] value() default {};

    /**
     * 缓存索引名。
     * <p>
     * 当同一实体上存在多个可唯一定位的字段时，使用该名称区分不同缓存通道。
     *
     * @return 缓存索引名称
     */
    String name() default "";

    /**
     * 声明该索引是否唯一。
     * <p>
     * {@code true} 表示键唯一映射到单实体；{@code false} 表示可能一键多值（列表场景）。
     *
     * @return 是否唯一，默认 true
     */
    boolean unique() default true;

    /**
     * 是否在查询未命中时自动创建新实体并插入数据库
     * <p>
     * 当 create=true 时，getCache/getEntity/getNameEntity 系列方法在查询未命中时，
     * 会自动创建新实体、将 key 值设置到对应字段上，并插入数据库后返回。
     * <p>
     * 与 getCached/getEntitied/getNameEntitied 系列方法的关系：
     * <ul>
     *   <li>getCached 系列方法始终自动创建（等同于 create=true）</li>
     *   <li>getCache + @Cache(create=true) 效果与 getCached 一致</li>
     *   <li>两者互相兼容，互不冲突</li>
     * </ul>
     *
     * @return 是否自动创建，默认 false
     */
    boolean create() default false;
}
