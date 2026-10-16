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
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

/**
 * 临时用户（{@code bot_guest}），归属机器人模块，与 {@link cn.org.autumn.modules.sys.entity.SysUserEntity}、
 * {@link RobotEntity} 为框架层不同类型。
 * <p>
 * 三者 {@code uuid} 由 {@link cn.org.autumn.modules.sys.service.UuidNamespaceService} 统一分配，禁止相同。
 * 无密码、无角色、不可登录；会话过期后由 {@code GuestService} 物理删除。
 */
@Getter
@Setter
@TableName("bot_guest")
@Table(value = "bot_guest", comment = "临时用户:网页访客等短生命周期身份")
public class GuestEntity implements UuidBased, AccountHandler.Account, UserContext {
    private static final long serialVersionUID = 1L;

    public static final int STATUS_ACTIVE = 1;
    public static final int STATUS_DISABLED = 0;

    @TableId
    @Column(isKey = true, type = DataType.BIGINT, length = 20, isNull = false, isAutoIncrement = true, comment = "id")
    private Long id;

    @Cache
    @Column(length = 32, comment = "临时用户:全局唯一业务主键", isUnique = true)
    private String uuid;

    @Column(length = 64, comment = "访客标识:客户端guestId，未过期时复用", isUnique = true)
    private String guestKey;

    @Column(length = 100, comment = "昵称:展示名", defaultValue = "")
    private String nickname;

    @Column(comment = "头像:图标地址")
    private String icon;

    @Column(comment = "状态:1正常;0停用", defaultValue = "1")
    private int status = STATUS_ACTIVE;

    @Column(type = "datetime", comment = "过期:此后不再对外可见")
    @Index
    private Date expireTime;

    @Column(type = "datetime", comment = "创建:创建时间")
    private Date createTime;

    @Column(type = "datetime", comment = "更新:更新时间")
    private Date updateTime;

    @Column(type = "datetime", comment = "活跃:最近使用时间")
    private Date lastSeenTime;

    /**
     * 正常且未过期才可继续作为聊天身份。
     */
    public boolean isActive() {
        return status == STATUS_ACTIVE && expireTime != null && expireTime.after(new Date());
    }

    @Override
    public int getVerify() {
        return UserContext.super.getVerify();
    }

    @Override
    public boolean isRobot() {
        return false;
    }

    @Override
    public boolean isGuest() {
        return true;
    }

    @Override
    public String getOwner() {
        return null;
    }

    public User toUser() {
        User user = new User();
        user.setUuid(uuid);
        user.setOwner(uuid);
        user.setNickname(nickname);
        user.setIcon(icon);
        user.setStatus(status);
        user.setRobot(false);
        user.setGuest(true);
        return user;
    }

    public User toSimple() {
        User user = new User();
        user.setUuid(uuid);
        user.setNickname(nickname);
        user.setIcon(icon);
        user.setRobot(false);
        user.setGuest(true);
        return user;
    }
}
