package cn.org.autumn.utils;

/**
 * Snowflake（雪花）分布式 ID：64 位 Long，全局递增、可按时间排序，且不依赖数据库自增，
 * 适合作为业务主键（与其它表关联时优先于自增 {@code id}）。多节点部署时需为每台 JVM 分配唯一的
 * {@code workerId} / {@code datacenterId}（参见构造参数及 {@link #getDefault()}）。
 * <p>
 * 位划分（与常见 Twitter Snowflake 布局一致）：41 位毫秒时间戳 + 5 位数据中心 + 5 位工作节点 + 12 位序列。
 */
public final class SnowflakeId {

    /** 纪元：2020-01-01 00:00:00 UTC（毫秒） */
    private static final long EPOCH_MS = 1577836800000L;

    private static final long WORKER_ID_BITS = 5L;
    private static final long DATACENTER_ID_BITS = 5L;
    private static final long SEQUENCE_BITS = 12L;

    private static final long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS);
    private static final long MAX_DATACENTER_ID = ~(-1L << DATACENTER_ID_BITS);

    private static final long SEQUENCE_MASK = ~(-1L << SEQUENCE_BITS);

    private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;
    private static final long DATACENTER_ID_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;
    private static final long TIMESTAMP_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS + DATACENTER_ID_BITS;

    private final long workerId;
    private final long datacenterId;

    private long sequence;
    private long lastTimestampMs = -1L;

    private static volatile SnowflakeId defaultSingleton;

    /**
     * @param workerId      工作机器编号，范围 0～31
     * @param datacenterId  机房/逻辑分组编号，范围 0～31
     */
    public SnowflakeId(long workerId, long datacenterId) {
        if (workerId > MAX_WORKER_ID || workerId < 0)
            throw new IllegalArgumentException("workerId must be between 0 and " + MAX_WORKER_ID);
        if (datacenterId > MAX_DATACENTER_ID || datacenterId < 0)
            throw new IllegalArgumentException("datacenterId must be between 0 and " + MAX_DATACENTER_ID);
        this.workerId = workerId;
        this.datacenterId = datacenterId;
    }

    /**
     * 生成下一个雪花 ID。
     */
    public synchronized long nextId() {
        long ts = currentTimeMillis();
        if (ts < lastTimestampMs)
            throw new IllegalStateException(
                    "Clock moved backwards. Refusing to generate id for " + lastTimestampMs + " ms");
        if (lastTimestampMs == ts) {
            sequence = (sequence + 1) & SEQUENCE_MASK;
            if (sequence == 0L)
                ts = waitNextMillis(lastTimestampMs);
        } else {
            sequence = 0L;
        }
        lastTimestampMs = ts;
        return ((ts - EPOCH_MS) << TIMESTAMP_SHIFT)
                | (datacenterId << DATACENTER_ID_SHIFT)
                | (workerId << WORKER_ID_SHIFT)
                | sequence;
    }

    /**
     * 使用 JVM 启动参数配置的默认单例：<br>
     * {@code -Dautumn.snowflake.worker-id=0 -Dautumn.snowflake.datacenter-id=0}<br>
     * 单机开发可省略（均为 0）；生产多实例必须为不同进程配置不同的 worker/datacenter，否则可能重复。
     */
    public static SnowflakeId getDefault() {
        SnowflakeId local = defaultSingleton;
        if (local != null)
            return local;
        synchronized (SnowflakeId.class) {
            if (defaultSingleton == null) {
                long w = Long.getLong("autumn.snowflake.worker-id", 0L);
                long d = Long.getLong("autumn.snowflake.datacenter-id", 0L);
                defaultSingleton = new SnowflakeId(w, d);
            }
            return defaultSingleton;
        }
    }

    /**
     * 等价于 {@link #getDefault()}{@code .}{@link #nextId()}，便于与 {@link Uuid#uuid()} 的静态调用风格对齐。
     */
    public static long next() {
        return getDefault().nextId();
    }

    private static long waitNextMillis(long last) {
        long ts = currentTimeMillis();
        while (ts <= last)
            ts = currentTimeMillis();
        return ts;
    }

    private static long currentTimeMillis() {
        return System.currentTimeMillis();
    }
}
