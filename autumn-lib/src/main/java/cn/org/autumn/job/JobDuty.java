package cn.org.autumn.job;

import lombok.extern.slf4j.Slf4j;

/**
 * LoopJob 任务职责（集群编排语义）。
 * <p>
 * 注解缺省为 {@link #ALL}，与历史「过 assign 后门禁后本机执行、框架不加集群锁」完全兼容。
 * 非 {@link #ALL} 须显式声明才会改变锁/执行策略（含 {@link #LOCAL} 本机锁、{@link #SINGLETON}/{@link #SEQUENTIAL} 集群编排）。
 */
@Slf4j
public enum JobDuty {

    /** 过 assign 后每台都跑；框架不加集群锁（历史默认）。 */
    ALL,

    /**
     * 本节点本地任务：进程内本地锁，不参与集群互斥 / SEQUENTIAL / oncePerPeriod。
     * 适合缓存清理、本机资源维护等；同机抢锁失败则跳过。
     */
    LOCAL,

    /** 全集群互斥；可叠加 {@code oncePerPeriod} 保证同一逻辑周期只跑一次。未获锁/无法互斥/未占桶时跳过。 */
    SINGLETON,

    /** 成员依次持锁执行。 */
    SEQUENTIAL,

    /** 不执行。 */
    DISABLED;

    /**
     * 解析职责；空白返回 {@link #ALL}。无法识别时返回 {@link #ALL} 并打 warn。
     */
    public static JobDuty of(String raw) {
        if (raw == null || raw.isBlank()) {
            return ALL;
        }
        try {
            return JobDuty.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            log.warn("Unknown JobDuty '{}', fallback ALL", raw.trim());
            return ALL;
        }
    }

    /** 是否为注解/配置缺省语义（不改变历史行为）。 */
    public boolean isDefault() {
        return this == ALL;
    }
}
