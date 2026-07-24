package cn.org.autumn.node;

/**
 * 节点画像扩展点。
 * <ul>
 *   <li>{@link #onCreate}：首次创建落盘前</li>
 *   <li>{@link #onLoad}：读盘命中已有 uuid 时；返回 true 表示已改 Profile，框架将回写磁盘</li>
 * </ul>
 * 实现类用 {@link org.springframework.core.annotation.Order} 控制顺序。
 */
public interface ProfileCustomizer {

    void onCreate(Profile profile, Fingerprint.Snapshot snap);

    /**
     * 已有画像加载后补全（例如 labels 缺失）。默认不改动。
     *
     * @return true 表示 profile 已被修改，需要落盘
     */
    default boolean onLoad(Profile profile, Fingerprint.Snapshot snap) {
        return false;
    }
}
