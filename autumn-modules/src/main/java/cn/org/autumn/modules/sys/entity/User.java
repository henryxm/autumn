package cn.org.autumn.modules.sys.entity;

import cn.org.autumn.annotation.SearchType;
import cn.org.autumn.model.UserContext;
import cn.org.autumn.modules.bot.entity.RobotEntity;
import cn.org.autumn.modules.bot.entity.GuestEntity;
import cn.org.autumn.search.IResult;
import cn.org.autumn.search.Result;
import lombok.*;
import org.apache.commons.beanutils.BeanUtils;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SearchType(value = "User", name = "账号", alias = "用户", describe = "基础用户账号信息")
public class User implements IResult, UserContext {

    private Result result = new Result(User.class);

    private String uuid;

    private String owner;

    private String username;

    private String nickname;

    private String description;

    private String email;

    private String mobile;

    private String idCard;

    private String icon;

    /** 头像对象存储 ID（对应 {@link SysUserEntity#getIconId()}）；展示 URL 用 {@link #icon}。 */
    private String iconId;

    private int status;

    private int verify;

    /** 永久封禁：1=banned（与 {@link SysUserEntity#getBanned()} 一致）。 */
    private int banned;

    private boolean robot;

    private boolean guest;

    private String access;

    private boolean black;

    private String scopes;

    @SneakyThrows
    public User(SysUserEntity entity) {
        BeanUtils.copyProperties(this, entity);
        this.robot = false;
        this.guest = false;
        this.owner = entity.getUuid();
    }

    @SneakyThrows
    public User(RobotEntity entity) {
        BeanUtils.copyProperties(this, entity);
        this.robot = true;
        this.guest = false;
        if (this.owner == null)
            this.owner = entity.getOwner();
    }

    @SneakyThrows
    public User(GuestEntity entity) {
        BeanUtils.copyProperties(this, entity);
        this.robot = false;
        this.guest = true;
        this.owner = entity.getUuid();
    }

    @Override
    public boolean isActive() {
        return status >= 1;
    }
}
