package cn.org.autumn.modules.usr.entity;

import com.baomidou.mybatisplus.annotations.*;
import cn.org.autumn.table.annotation.*;
import cn.org.autumn.table.data.DataType;


import java.io.Serializable;
import java.util.Date;

/**
 * 用户Token
 *
 * @author Shaohua Xu
 * @email henryxm@163.com
 * @date 2020-11
 */

@TableName("usr_user_token")
@Table(value = "usr_user_token", comment = "用户Token")
public class UserTokenEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    @TableId
    @Column(isKey = true, type = "bigint", length = 20, isNull = false, isAutoIncrement = true, comment = "ID")
    private Long id;

    @Column(type = "bigint", comment = "用户ID")
    private Long userId;
    /**
     * Token
     */
    @Column(length = 100, comment = "Token")
    private String token;
    /**
     * Refresh Token
     */
    @Column(length = 100, comment = "Refresh Token")
    private String refreshToken;
    /**
     * 过期时间
     */
    @Column(type = "datetime", comment = "过期时间")
    private Date expireTime;
    /**
     * 更新时间
     */
    @Column(type = "datetime", comment = "更新时间")
    private Date updateTime;

    /**
     * 设置：用户ID
     */
    public void setUserId(Long userId) {
        this.userId = userId;
    }

    /**
     * 获取：用户ID
     */
    public Long getUserId() {
        return userId;
    }

    /**
     * 设置：Token
     */
    public void setToken(String token) {
        this.token = token;
    }

    /**
     * 获取：Token
     */
    public String getToken() {
        return token;
    }

    /**
     * 设置：Refresh Token
     */
    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    /**
     * 获取：Refresh Token
     */
    public String getRefreshToken() {
        return refreshToken;
    }

    /**
     * 设置：过期时间
     */
    public void setExpireTime(Date expireTime) {
        this.expireTime = expireTime;
    }

    /**
     * 获取：过期时间
     */
    public Date getExpireTime() {
        return expireTime;
    }

    /**
     * 设置：更新时间
     */
    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    /**
     * 获取：更新时间
     */
    public Date getUpdateTime() {
        return updateTime;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
