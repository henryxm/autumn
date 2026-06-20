package cn.org.autumn.entity;

/**
 * 字符串型第二主键契约：业务侧统一列名为 {@code uuid}，类型 {@link String}，32 位无连字符小写十六进制。
 * <p>
 * <strong>职责</strong>：表间外键、对外 API 资源 ID、缓存键、消息引用等<strong>全部业务标识</strong>均使用本列，
 * <strong>禁止</strong>使用自增 {@link IdBased#getId() id}。
 * <p>
 * <strong>实体约定</strong>：
 * <pre>{@code
 * @TableId
 * @Column(isKey = true, type = "bigint", isAutoIncrement = true, ...)
 * private Long id;
 *
 * @Column(length = 32, isUnique = true, comment = "机器人:全局唯一业务主键")
 * private String uuid;
 *
 * @Column(length = 32, comment = "用户:所属用户uuid")
 * @Index
 * private String owner;  // 外键：新代码优先 {concept}；存量 {concept}Id 保持不变
 * }</pre>
 * 第二主键与外键 {@code comment} 为 <strong>{@code {概念}:说明}</strong> 或 <strong>{@code {概念}ID:说明}</strong>（新代码优先 {@code {概念}}）；{@code String} 默认 {@code length = 32}。
 * 详见 {@code docs/AI_DUAL_KEY.md} §3.1～§3.2。
 * <p>
 * {@code isUnique=true} 的列<strong>禁止</strong>再叠加 {@code @Index}（见 {@code docs/AI_STANDARDS.md} §10.2）。
 *
 * @see IdBased
 * @see SnowBased
 * @see cn.org.autumn.service.AutoIdService
 * @see cn.org.autumn.utils.Uuid
 */
public interface UuidBased extends IdBased {
    String getUuid();

    void setUuid(String uuid);
}
