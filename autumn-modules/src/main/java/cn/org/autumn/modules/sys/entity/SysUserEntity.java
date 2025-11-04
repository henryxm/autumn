package cn.org.autumn.modules.sys.entity;

import cn.org.autumn.config.AccountHandler;
import cn.org.autumn.exception.CodeException;
import cn.org.autumn.modules.usr.entity.UserProfileEntity;
import cn.org.autumn.table.annotation.Column;
import cn.org.autumn.table.annotation.Index;
import cn.org.autumn.table.annotation.Table;
import cn.org.autumn.table.data.DataType;
import com.baomidou.mybatisplus.annotations.TableField;
import com.baomidou.mybatisplus.annotations.TableId;
import com.baomidou.mybatisplus.annotations.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;
import cn.org.autumn.validator.group.AddGroup;
import cn.org.autumn.validator.group.UpdateGroup;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@Getter
@Setter
@TableName("sys_user")
@Table(value = "sys_user", comment = "系统用户")
public class SysUserEntity implements AccountHandler.User, Serializable {
    private static final long serialVersionUID = 1L;

    @TableId
    @Column(isKey = true, type = DataType.BIGINT, length = 20, isNull = false, isAutoIncrement = true, comment = "id")
    private Long userId;

    @Column(length = 50, comment = "UUID", isUnique = true)
    private String uuid;

    @Column(comment = "父级UUID")
    private String parentUuid;

    @NotBlank(message = "用户名不能为空", groups = {AddGroup.class, UpdateGroup.class})
    @Column(length = 100, comment = "用户名", isUnique = true)
    private String username;

    @Column(length = 100, comment = "昵称", defaultValue = "")
    @Index
    private String nickname;

    @NotBlank(message = "密码不能为空", groups = AddGroup.class)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Column(length = 100, comment = "密码")
    private String password;

    @Column(length = 20, comment = "盐")
    private String salt;

    @Column(length = 100, comment = "邮箱")
    @Index
    private String email;

    @Column(length = 100, comment = "手机号")
    @Index
    private String mobile;

    @Column(length = 100, comment = "qq号")
    @Index
    private String qq;

    @Column(length = 100, comment = "微信号")
    @Index
    private String weixin;

    @Column(length = 100, comment = "支付宝号")
    @Index
    private String alipay;

    @Column(length = 100, comment = "身份证号")
    @Index
    private String idCard;

    @Column(comment = "头像")
    private String icon;

    @Column(comment = "头像MD5", length = 128, defaultValue = "")
    private String iconMd5;

    @Column(length = 100, comment = "OPENID")
    @Index
    private String openId;

    @Column(length = 100, comment = "UNIONID")
    @Index
    private String unionId;

    @Column(comment = "状态:-1,注销;0,禁用;1:正常")
    private int status = 1;

    @Column(length = 100, comment = "部门标识")
    private String deptKey;

    @Column(comment = "原因")
    private String reason;

    @Column(comment = "创建")
    private Date createTime;

    @Column(comment = "更新")
    private Date updateTime;

    @TableField(exist = false)
    private List<String> roleKeys;

    @TableField(exist = false)
    private String deptName;

    @TableField(exist = false)
    private UserProfileEntity profile;

    @TableField(exist = false)
    private SysUserEntity parent;

    public void copy(SysUserEntity entity) {
        this.parentUuid = entity.parentUuid;
        this.username = entity.username;
        this.nickname = entity.nickname;
        this.alipay = entity.alipay;
        this.mobile = entity.mobile;
        this.email = entity.email;
        this.qq = entity.qq;
        this.weixin = entity.weixin;
        this.status = entity.status;
        this.icon = entity.icon;
    }

    public SysUserEntity copy() {
        SysUserEntity clone = new SysUserEntity();
        clone.copy(this);
        return clone;
    }

    public boolean check() {
        return status >= 1;
    }

    public void checkThrow() throws Exception {
        if (status < 1)
            throw new CodeException("服务繁忙", -10000);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                getUuid(),
                getParentUuid(),
                getUsername(),
                getNickname(),
                getPassword(),
                getSalt(),
                getEmail(),
                getMobile(),
                getQq(),
                getWeixin(),
                getAlipay(),
                getIdCard(),
                getStatus(),
                getIcon(),
                getIconMd5()
        );
    }
}