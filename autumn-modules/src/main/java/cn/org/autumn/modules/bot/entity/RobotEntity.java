package cn.org.autumn.modules.bot.entity;

import cn.org.autumn.annotation.Cache;
import cn.org.autumn.config.AccountHandler;
import cn.org.autumn.entity.UuidBased;
import cn.org.autumn.model.UserContext;
import cn.org.autumn.modules.sys.entity.User;
import cn.org.autumn.table.annotation.Column;
import cn.org.autumn.table.annotation.Index;
import cn.org.autumn.table.annotation.Table;
import cn.org.autumn.table.data.DataType;
import com.baomidou.mybatisplus.annotations.TableId;
import com.baomidou.mybatisplus.annotations.TableName;
import java.util.Date;
import lombok.Getter;
import lombok.Setter;

/**
 * 机器人 API 调用身份（{@code bot_robot}），与 {@link cn.org.autumn.modules.sys.entity.SysUserEntity} 为框架层<strong>不同类型</strong>。
 * <p>
 * 二者 {@code uuid} 由 {@link cn.org.autumn.modules.sys.service.UuidNamespaceService} 统一分配，<strong>禁止相同</strong>。
 * 业务层常将机器人视作用户：业务表 {@code user} 列可存本实体 {@code uuid} 或真人 {@code uuid}（见 {@code docs/AI_DUAL_KEY.md} §1.1）。
 * {@code owner} 仅表示主人真人 {@code sys_user.uuid}。实现 {@link cn.org.autumn.model.UserContext}，{@link #isRobot()} 为 {@code true}。
 */
@Getter
@Setter
@TableName("bot_robot")
@Table(value = "bot_robot", comment = "机器人:系统用户创建的API调用身份")
public class RobotEntity implements UuidBased, AccountHandler.Account, UserContext {
    private static final long serialVersionUID = 1L;

    public static final int STATUS_ACTIVE = 1;
    public static final int STATUS_DISABLED = 0;
    public static final int STATUS_DELETED = -1;
    public static final int STATUS_DESTROYED = -2;

    public static final String ACCESS_PRIVATE = "private";
    public static final String ACCESS_PUBLIC = "public";
    public static final String ACCESS_SUBSCRIBE = "subscribe";

    @TableId
    @Column(isKey = true, type = DataType.BIGINT, length = 20, isNull = false, isAutoIncrement = true, comment = "id")
    private Long id;

    @Cache
    @Column(length = 32, comment = "机器人:全局唯一业务主键", isUnique = true)
    private String uuid;

    @Column(length = 32, comment = "用户:主人sys_user.uuid")
    @Index
    private String owner;

    @Column(length = 100, comment = "名称:机器人展示名")
    @Index
    private String nickname;

    @Column(type = DataType.TEXT, comment = "描述:用途说明")
    private String description;

    @Column(comment = "头像:图标地址")
    private String icon;

    @Column(comment = "HASH:头像HASH")
    @Index
    private String hash;

    @Column(comment = "状态:1正常;0停用;-1删除;-2销毁", defaultValue = "1")
    private int status = STATUS_ACTIVE;

    @Column(comment = "访问:private仅主人,public任意用户,subscribe需订阅", length = 16, defaultValue = "private")
    private String access = ACCESS_PRIVATE;

    @Column(comment = "拉黑:平台级封禁", defaultValue = "0")
    private boolean black = false;

    @Column(length = 500, comment = "权限:API权限点CSV")
    private String scopes;

    @Column(type = "datetime", comment = "创建:创建时间")
    private Date createTime;

    @Column(type = "datetime", comment = "更新:更新时间")
    private Date updateTime;

    @Column(type = "datetime", comment = "删除:软删除时间")
    private Date deleteTime;

    @Column(type = "datetime", comment = "销毁:不可逆销毁时间")
    private Date destroyTime;

    @Column(type = "datetime", comment = "活跃:最近API调用时间")
    private Date lastUsedTime;

    public boolean isActive() {
        return status == STATUS_ACTIVE;
    }

    public String resolvedAccess() {
        if (access == null || access.trim().isEmpty()) {
            return ACCESS_PRIVATE;
        }
        String mode = access.trim().toLowerCase();
        if (ACCESS_PUBLIC.equals(mode) || ACCESS_SUBSCRIBE.equals(mode)) {
            return mode;
        }
        return ACCESS_PRIVATE;
    }

    @Override
    public boolean isRobot() {
        return true;
    }

    @Override
    public int getVerify() {
        return UserContext.super.getVerify();
    }

    public User toUser() {
        return new User(this);
    }

    public User toSimple() {
        User user = new User();
        user.setUuid(uuid);
        user.setNickname(nickname);
        user.setIcon(icon);
        user.setRobot(true);
        return user;
    }
}
