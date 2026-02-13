package cn.org.autumn.annotation;

import java.lang.annotation.*;

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
     * 当有多个不同的字段都有唯一值的时候，可以使用name来区分
     *
     * @return 返回一个指定字段的名字
     */
    String name() default "";

    /**
     * 指明缓存的数据是否具有唯一性，如果不是唯一的，应返回数据格式
     *
     * @return 是否是唯一的缓存对象，否则返回数据
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
