package cn.org.autumn.utils;

import lombok.Getter;

import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

/**
 * 雪花算法 ID 生成器，用作 {@link cn.org.autumn.entity.SnowBased} 实体的第二主键（列名仍为 {@code uuid}，类型 {@code Long}）。
 * <p>
 * 64 位 ID 结构：1 位符号（0）+ 41 位毫秒时间戳 + 5 位数据中心 + 5 位机器 + 12 位毫秒内序列。
 * 自增 {@code Long id} 仅用于后台生成 CRUD；业务关联与对外标识使用 {@code uuid} 列（本类输出值）。
 * <p>
 * <strong>调用</strong>：{@link #uuid()} 等价于默认单例 {@code new Snow().next()}，风格对齐 {@link Uuid#uuid()}。
 * 通常由 {@link cn.org.autumn.service.AutoIdService} 在插入前自动调用。
 * <p>
 * <strong>多节点</strong>：生产环境为各 JVM 显式配置
 * {@code -Dautumn.snowflake.worker-id=} / {@code -Dautumn.snowflake.datacenter-id=}；
 * 未配置时由进程号、网卡 MAC、主机 IP 推导（单机开发可用，集群须人工分配避免碰撞）。
 * <p>
 * {@link SnowflakeId} 已废弃，内部委托本类；新代码统一使用 {@link #uuid()} 或 {@link #next()}。
 *
 * @see cn.org.autumn.entity.SnowBased
 * @see cn.org.autumn.service.AutoIdService
 * @see Uuid
 */
public class Snow {

    private static final Snow snow = new Snow();

    // 开始时间戳（2023-01-01 00:00:00 UTC）
    private final long startTimestamp = 1672531200000L;

    // 机器ID所占位数
    private final long machineIdBits = 5L;

    // 数据中心ID所占位数
    private final long dataCenterIdBits = 5L;

    // 序列号所占位数
    private final long sequenceBits = 12L;

    // 支持的最大机器ID
    private final long maxMachineId = ~(-1L << machineIdBits);

    // 支持的最大数据中心ID
    private final long maxDataCenterId = ~(-1L << dataCenterIdBits);

    // 机器ID向左移12位
    private final long machineIdShift = sequenceBits;

    // 数据中心ID向左移17位
    private final long dataCenterIdShift = sequenceBits + machineIdBits;

    // 时间戳向左移22位
    private final long timestampLeftShift = sequenceBits + machineIdBits + dataCenterIdBits;

    // 序列号掩码，用于限定序列号的最大值4095
    private final long sequenceMask = ~(-1L << sequenceBits);

    // 工作机器ID
    private final long machineId;

    // 数据中心ID
    private final long dataCenterId;

    // 毫秒内序列号
    private long sequence = 0L;

    // 上次生成ID的时间戳
    private long lastTimestamp = -1L;

    /**
     * 构造函数
     *
     * @param machineId    机器ID (0~31)
     * @param dataCenterId 数据中心ID (0~31)
     */
    public Snow(long machineId, long dataCenterId) {
        if (machineId > maxMachineId || machineId < 0) {
            throw new IllegalArgumentException("机器ID不能大于" + maxMachineId + "或小于0");
        }
        if (dataCenterId > maxDataCenterId || dataCenterId < 0) {
            throw new IllegalArgumentException("数据中心ID不能大于" + maxDataCenterId + "或小于0");
        }
        this.machineId = machineId;
        this.dataCenterId = dataCenterId;
    }

    /**
     * 默认构造函数：优先读取 JVM 参数 {@code -Dautumn.snowflake.worker-id=} /
     * {@code -Dautumn.snowflake.datacenter-id=}，未配置时由本机进程、网卡与主机信息推导。
     */
    public Snow() {
        this(resolveMachineId(), resolveDataCenterId());
    }

    private static long resolveMachineId() {
        Long configured = Long.getLong("autumn.snowflake.worker-id");
        if (configured != null) {
            return configured;
        }
        try {
            String jvmName = ManagementFactory.getRuntimeMXBean().getName();
            int at = jvmName.indexOf('@');
            if (at > 0) {
                return Long.parseLong(jvmName.substring(0, at)) & ~(-1L << 5L);
            }
        } catch (Exception ignored) {
        }
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
                NetworkInterface ni = en.nextElement();
                if (ni.isLoopback() || ni.isVirtual() || !ni.isUp()) {
                    continue;
                }
                byte[] mac = ni.getHardwareAddress();
                if (mac != null && mac.length >= 2) {
                    return ((mac[mac.length - 1] & 0xFFL) | ((mac[mac.length - 2] & 0xFFL) << 8)) & ~(-1L << 5L);
                }
            }
        } catch (Exception ignored) {
        }
        return 1L;
    }

    private static long resolveDataCenterId() {
        Long configured = Long.getLong("autumn.snowflake.datacenter-id");
        if (configured != null) {
            return configured;
        }
        try {
            String host = InetAddress.getLocalHost().getHostAddress();
            return ((long) host.hashCode() & 0xFFFFFFFFL) & ~(-1L << 5L);
        } catch (Exception ignored) {
        }
        return 1L;
    }

    /**
     * 生成下一个雪花业务主键（与 {@link Uuid#uuid()} 静态风格一致）。
     */
    public static long uuid() {
        return snow.next();
    }

    /**
     * 生成下一个ID
     *
     * @return 唯一ID
     */
    public synchronized long next() {
        long timestamp = System.currentTimeMillis();
        // 如果当前时间小于上一次ID生成的时间戳，说明系统时钟回退过
        if (timestamp < lastTimestamp) {
            throw new RuntimeException("系统时钟回退，拒绝生成ID");
        }
        // 如果是同一时间生成的，则进行毫秒内序列
        if (lastTimestamp == timestamp) {
            sequence = (sequence + 1) & sequenceMask;
            // 毫秒内序列溢出
            if (sequence == 0) {
                // 阻塞到下一个毫秒
                timestamp = tilNextMillis(lastTimestamp);
            }
        } else {
            // 时间戳改变，毫秒内序列重置
            sequence = 0L;
        }
        lastTimestamp = timestamp;
        // 组合成64位的ID
        return ((timestamp - startTimestamp) << timestampLeftShift) | (dataCenterId << dataCenterIdShift) | (machineId << machineIdShift) | sequence;
    }

    /**
     * 阻塞到下一个毫秒，直到获得新的时间戳
     *
     * @param lastTimestamp 上次生成ID的时间戳
     * @return 新的时间戳
     */
    private long tilNextMillis(long lastTimestamp) {
        long timestamp = System.currentTimeMillis();
        while (timestamp <= lastTimestamp) {
            timestamp = System.currentTimeMillis();
        }
        return timestamp;
    }

    /**
     * 解析ID
     *
     * @param id 雪花算法生成的ID
     * @return ID信息
     */
    public Info parse(long id) {
        long timestamp = (id >> timestampLeftShift) + startTimestamp;
        long dataCenterId = (id >> dataCenterIdShift) & maxDataCenterId;
        long machineId = (id >> machineIdShift) & maxMachineId;
        long sequence = id & sequenceMask;
        return new Info(timestamp, dataCenterId, machineId, sequence);
    }

    /**
     * ID信息类
     */
    @Getter
    public static class Info {
        private final long timestamp;
        private final long dataCenterId;
        private final long machineId;
        private final long sequence;

        public Info(long timestamp, long dataCenterId, long machineId, long sequence) {
            this.timestamp = timestamp;
            this.dataCenterId = dataCenterId;
            this.machineId = machineId;
            this.sequence = sequence;
        }

        @Override
        public String toString() {
            return String.format("IdInfo{timestamp=%d, dataCenterId=%d, machineId=%d, sequence=%d}", timestamp, dataCenterId, machineId, sequence);
        }
    }
}
