package cn.org.autumn.modules.usr.entity;

import com.baomidou.mybatisplus.annotations.*;
import cn.org.autumn.table.annotation.*;
import com.baomidou.mybatisplus.enums.IdType;


import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

/**
 * 用户信息
 *
 * @author Shaohua Xu
 * @email henryxm@163.com
 * @date 2020-11
 */
@TableName("usr_user_profile")
@Table(value = "usr_user_profile", comment = "用户信息")
public class UserProfileEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.INPUT)
    @Column(comment = "UUID", length = 50, isKey = true, isUnique = true)
    private String uuid;
    /**
     * OPENID
     */
    @Column(comment = "OPENID")
    private String openId;
    /**
     * UNIONID
     */
    @Column(comment = "UNIONID")
    private String unionId;
    /**
     * 头像
     */
    @Column(comment = "头像")
    private String icon;
    /**
     * 用户名
     */
    @Column(length = 50, comment = "用户名", isUnique = true)
    private String username;
    /**
     * 用户昵称
     */
    @Column(length = 50, comment = "用户昵称")
    private String nickname;
    /**
     * 手机号
     */
    @Column(length = 20, comment = "手机号")
    private String mobile;
    /**
     * 密码
     */
    @Column(length = 128, comment = "密码")
    private String password;
    /**
     * 创建时间
     */
    @Column(type = "datetime", comment = "创建时间")
    private Date createTime;

    /**
     * 设置：UUID
     */
    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    /**
     * 获取：UUID
     */
    public String getUuid() {
        return uuid;
    }

    /**
     * 设置：OPENID
     */
    public void setOpenId(String openId) {
        this.openId = openId;
    }

    /**
     * 获取：OPENID
     */
    public String getOpenId() {
        return openId;
    }

    /**
     * 设置：UNIONID
     */
    public void setUnionId(String unionId) {
        this.unionId = unionId;
    }

    /**
     * 获取：UNIONID
     */
    public String getUnionId() {
        return unionId;
    }

    /**
     * 设置：头像
     */
    public void setIcon(String icon) {
        this.icon = icon;
    }

    /**
     * 获取：头像
     */
    public String getIcon() {
        return icon;
    }

    /**
     * 设置：用户名
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * 获取：用户名
     */
    public String getUsername() {
        return username;
    }

    /**
     * 设置：用户昵称
     */
    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    /**
     * 获取：用户昵称
     */
    public String getNickname() {
        return nickname;
    }

    /**
     * 设置：手机号
     */
    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    /**
     * 获取：手机号
     */
    public String getMobile() {
        return mobile;
    }

    /**
     * 设置：密码
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * 获取：密码
     */
    public String getPassword() {
        return password;
    }

    /**
     * 设置：创建时间
     */
    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    /**
     * 获取：创建时间
     */
    public Date getCreateTime() {
        return createTime;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getUuid(), getOpenId(), getUnionId(), getIcon(), getUsername(), getPassword());
    }
}
