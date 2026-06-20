package cn.org.autumn.entity;

/**
 * 长整型第二主键契约：业务侧统一列名仍为 {@code uuid}，类型 {@link Long}（雪花算法，全局唯一、近似时序）。
 * <p>
 * 与 {@link UuidBased} 的职责划分相同——关联、对外 ID、缓存与消息引用<strong>只用本列</strong>，
 * <strong>禁止</strong>使用自增 {@link IdBased#getId() id}。列名统一为 {@code uuid} 是为保持
 * 跨实体、跨模块的命名一致，类型由实体声明区分（{@code String} vs {@code Long}）。
 * <p>
 * <strong>实体约定</strong>：
 * <pre>{@code
 * @TableId
 * @Column(isKey = true, type = "bigint", isAutoIncrement = true, ...)
 * private Long id;
 *
 * @Column(type = "bigint", isUnique = true, comment = "订单:全局唯一业务主键")
 * private Long uuid;
 *
 * @Column(type = "bigint", comment = "用户:所属用户uuid")
 * @Index
 * private Long user;  // 外键：新代码优先 {concept}；存量 {concept}Id 保持不变
 * }</pre>
 * 第二主键与外键 {@code comment} 为 <strong>{@code {概念}:说明}</strong> 或 <strong>{@code {概念}ID:说明}</strong>（新代码优先 {@code {概念}}）。详见 {@code docs/AI_DUAL_KEY.md} §3.1～§3.2。
 * <p>
 * <strong>赋值</strong>：插入前由 {@link cn.org.autumn.service.AutoIdService} 调用
 * {@link cn.org.autumn.utils.Snow#uuid()} 自动填充（仅当 {@code uuid} 为 {@code null} 或 {@code 0L} 时）。
 * 多节点部署须为各 JVM 配置 {@code -Dautumn.snowflake.worker-id=} /
 * {@code -Dautumn.snowflake.datacenter-id=}，见 {@link cn.org.autumn.utils.Snow}。
 *
 * @see IdBased
 * @see UuidBased
 * @see cn.org.autumn.service.AutoIdService
 * @see cn.org.autumn.utils.Snow
 */
public interface SnowBased extends IdBased {
    Long getUuid();

    void setUuid(Long uuid);
}
