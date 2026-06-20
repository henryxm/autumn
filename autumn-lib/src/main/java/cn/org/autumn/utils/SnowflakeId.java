package cn.org.autumn.utils;

/**
 * 雪花分布式 ID 兼容层，实现已统一委托 {@link Snow}。
 * <p>
 * 新代码请使用 {@link Snow#uuid()} 或 {@code new Snow(workerId, datacenterId).next()}；
 * 实体第二主键请实现 {@link cn.org.autumn.entity.SnowBased} 并由 {@link cn.org.autumn.service.AutoIdService} 自动填充。
 *
 * @deprecated 请改用 {@link Snow}；本类仅保留旧调用点的二进制与源码兼容。
 */
@Deprecated
public final class SnowflakeId {

    /** {@code null} 表示使用 {@link Snow} 默认单例（与 {@link Snow#uuid()} 相同）。 */
    private final Snow delegate;

    /**
     * @param workerId     工作机器编号，范围 0～31
     * @param datacenterId 机房/逻辑分组编号，范围 0～31
     * @deprecated 请使用 {@code new Snow(workerId, datacenterId)}
     */
    @Deprecated
    public SnowflakeId(long workerId, long datacenterId) {
        this.delegate = new Snow(workerId, datacenterId);
    }

    private SnowflakeId() {
        this.delegate = null;
    }

    /**
     * @deprecated 请使用 {@link Snow#uuid()} 或 {@code snow.next()}
     */
    @Deprecated
    public synchronized long nextId() {
        if (delegate == null)
            return Snow.uuid();
        return delegate.next();
    }

    /**
     * 使用 JVM 参数 {@code -Dautumn.snowflake.worker-id=} /
     * {@code -Dautumn.snowflake.datacenter-id=} 配置的默认实例（委托 {@link Snow} 默认单例）。
     *
     * @deprecated 请直接使用 {@link Snow#uuid()}
     */
    @Deprecated
    public static SnowflakeId getDefault() {
        return DefaultHolder.INSTANCE;
    }

    /**
     * 等价于 {@link Snow#uuid()}。
     *
     * @deprecated 请使用 {@link Snow#uuid()}
     */
    @Deprecated
    public static long next() {
        return Snow.uuid();
    }

    private static final class DefaultHolder {
        private static final SnowflakeId INSTANCE = new SnowflakeId();
    }
}
