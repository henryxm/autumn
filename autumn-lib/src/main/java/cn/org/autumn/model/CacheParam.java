package cn.org.autumn.model;

/**
 * 缓存参数包装类，统一处理缓存 key 的自定义序列化和透传标记。
 * <p>
 * 两种用途：
 * <ul>
 *   <li><b>自定义 key</b>：包装一个值并指定其在缓存 key 中的表示形式，
 *       实体类无需再实现接口</li>
 *   <li><b>透传参数</b>：标记一个参数不参与缓存 key 的计算，但值仍会传递给业务逻辑</li>
 * </ul>
 * <p>
 * 使用示例：
 * <pre>
 *   // 自定义 key：将实体的 id 作为缓存 key
 *   service.getEntity(CacheParam.key(entity, entity.getId().toString()));
 *
 *   // 透传参数：locale 不参与 key 计算
 *   service.getEntity(userId, deptId, CacheParam.pass(locale));
 * </pre>
 *
 * @param <T> 包装值的类型
 */
public class CacheParam<T> {

    private final T value;
    private final String key;
    private final boolean transparent;

    private CacheParam(T value, String key, boolean transparent) {
        this.value = value;
        this.key = key;
        this.transparent = transparent;
    }

    /**
     * 创建透传参数，不参与缓存 key 计算
     *
     * @param value 原始值，仍可通过 {@link #get()} 获取供业务使用
     * @param <T>   值类型
     * @return 透传参数包装
     */
    public static <T> CacheParam<T> pass(T value) {
        return new CacheParam<>(value, null, true);
    }

    /**
     * 创建自定义 key 参数，使用指定的 customKey 参与缓存 key 计算
     *
     * @param value     原始值
     * @param customKey 自定义的 key 字符串
     * @param <T>       值类型
     * @return 自定义 key 包装
     */
    public static <T> CacheParam<T> key(T value, String customKey) {
        return new CacheParam<>(value, customKey, false);
    }

    /**
     * 获取包装的原始值（供业务逻辑使用）
     */
    public T get() {
        return value;
    }

    /**
     * 获取缓存 key 表示
     *
     * @return 自定义 key 字符串；透传参数返回 null
     */
    public String toKey() {
        return key;
    }

    /**
     * 是否为透传参数
     */
    public boolean isTransparent() {
        return transparent;
    }

    @Override
    public String toString() {
        if (transparent) {
            return "CacheParam.pass(" + value + ")";
        }
        return "CacheParam.key(" + value + ", \"" + key + "\")";
    }
}
