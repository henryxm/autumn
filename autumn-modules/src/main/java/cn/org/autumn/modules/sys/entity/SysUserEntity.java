package cn.org.autumn.modules.sys.entity;

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

import javax.validation.constraints.NotBlank;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@TableName("sys_user")
@Table(value = "sys_user", comment = "系统用户")
public class SysUserEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId
    @Column(isKey = true, type = DataType.BIGINT, length = 20, isNull = false, isAutoIncrement = true, comment = "id")
    private Long userId;

    @Column(length = 50, comment = "UUID", isUnique = true)
    private String uuid;

    @Column(comment = "父级UUID")
    private String parentUuid;

    @NotBlank(message = "用户名不能为空", groups = {AddGroup.class, UpdateGroup.class})
    @Column(length = 50, comment = "用户名", isUnique = true)
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
    private String weixing;

    @Column(length = 100, comment = "支付宝号")
    @Index
    private String alipay;

    @Column(comment = "身份证号", length = 50)
    @Index
    private String idCard;

    @Column(comment = "头像")
    private String icon;

    @Column(comment = "OPENID")
    @Index
    private String openId;

    @Column(comment = "UNIONID")
    @Index
    private String unionId;

    @Column(comment = "状态:0,禁用;1:正常")
    private int status = 1;

    @TableField(exist = false)
    private List<String> roleKeys;

    @Column(length = 100, comment = "部门标识")
    private String deptKey;

    @Column(length = 20, type = DataType.DATETIME, comment = "创建")
    private Date createTime;

    @TableField(exist = false)
    private String deptName;

    @TableField(exist = false)
    private UserProfileEntity profile;

    @TableField(exist = false)
    private SysUserEntity parent;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getParentUuid() {
        return parentUuid;
    }

    public void setParentUuid(String parentUuid) {
        this.parentUuid = parentUuid;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getSalt() {
        return salt;
    }

    public void setSalt(String salt) {
        this.salt = salt;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getQq() {
        return qq;
    }

    public void setQq(String qq) {
        this.qq = qq;
    }

    public String getWeixing() {
        return weixing;
    }

    public void setWeixing(String weixing) {
        this.weixing = weixing;
    }

    public String getAlipay() {
        return alipay;
    }

    public void setAlipay(String alipay) {
        this.alipay = alipay;
    }

    public String getIdCard() {
        return idCard;
    }

    public void setIdCard(String idCard) {
        this.idCard = idCard;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getOpenId() {
        return openId;
    }

    public void setOpenId(String openId) {
        this.openId = openId;
    }

    public String getUnionId() {
        return unionId;
    }

    public void setUnionId(String unionId) {
        this.unionId = unionId;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public List<String> getRoleKeys() {
        return roleKeys;
    }

    public void setRoleKeys(List<String> roleKeys) {
        this.roleKeys = roleKeys;
    }

    public String getDeptKey() {
        return deptKey;
    }

    public void setDeptKey(String deptKey) {
        this.deptKey = deptKey;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public String getDeptName() {
        return deptName;
    }

    public void setDeptName(String deptName) {
        this.deptName = deptName;
    }

    public UserProfileEntity getProfile() {
        return profile;
    }

    public void setProfile(UserProfileEntity profile) {
        this.profile = profile;
    }

    public SysUserEntity getParent() {
        return parent;
    }

    public void setParent(SysUserEntity parent) {
        this.parent = parent;
    }

    public void copy(SysUserEntity entity) {
        this.parentUuid = entity.parentUuid;
        this.username = entity.username;
        this.nickname = entity.getNickname();
        this.password = entity.password;
        this.salt = entity.salt;
        this.alipay = entity.alipay;
        this.mobile = entity.mobile;
        this.email = entity.email;
        this.qq = entity.qq;
        this.weixing = entity.weixing;
        this.status = entity.status;
        this.icon = entity.getIcon();
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
                getWeixing(),
                getAlipay(),
                getIdCard(),
                getStatus(),
                getIcon()
        );
    }
}