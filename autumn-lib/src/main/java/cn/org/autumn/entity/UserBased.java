package cn.org.autumn.entity;

/**
 * 按用户唯一约束的第二主键契约：业务侧用 {@code user} 列（存用户 {@code uuid} 值）作为唯一业务标识，**禁止**再定义独立 {@code uuid} 第二主键。
 * <p>
 * <strong>适用</strong>：整表语义为「每个<strong>真人</strong>用户至多一行」（用户配额、用户级设置等），唯一性由 {@code user} 承担。
 * <p>
 * <strong>与非唯一业务 {@code user} 列区分</strong>：业务模块中其它表的 {@code user} 外键可存真人或机器人 {@code uuid}（§1.1）；本接口的 {@code user} <strong>仅</strong> {@code sys_user.uuid}。
 * <p>
 * <strong>实体约定</strong>：
 * <pre>{@code
 * @TableId
 * @Column(isKey = true, type = "bigint", isAutoIncrement = true, ...)
 * private Long id;
 *
 * @Column(length = 32, isUnique = true, comment = "用户:对应sys_user.uuid，唯一")
 * private String user;
 * }</pre>
 * {@code user} 须 {@code isUnique = true}，且<strong>禁止</strong>再叠 {@code @Index}（§10.2）；<strong>禁止</strong>同时存在业务第二主键列 {@code uuid}。
 * {@code user} 值由业务在插入前赋值（当前登录用户等），不经 {@link cn.org.autumn.service.AutoIdService} 自动生成。
 * <p>
 * <strong>与非唯一 {@code user} 外键区分</strong>：若表仍有独立 {@code uuid} 第二主键（{@link UuidBased} / {@link SnowBased}），可同时有非唯一的 {@code user}
 * 列表示所属用户，该列<strong>不得</strong> {@code isUnique = true}。详见 {@code docs/AI_DUAL_KEY.md} §3.3、§4.3。
 *
 * @see IdBased
 * @see UuidBased
 * @see SnowBased
 */
public interface UserBased extends IdBased {
    String getUser();

    void setUser(String user);
}
