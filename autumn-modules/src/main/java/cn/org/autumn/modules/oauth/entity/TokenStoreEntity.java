package cn.org.autumn.modules.oauth.entity;

import com.baomidou.mybatisplus.annotations.*;
import cn.org.autumn.table.annotation.*;
import cn.org.autumn.table.data.DataType;


import java.io.Serializable;
import java.util.Date;

/**
 * 授权令牌
 * 
 * @author Shaohua Xu
 * @email henryxm@163.com
 * @date 2020-11
 */

@TableName("oauth_token_store")
@Table(value = "oauth_token_store", comment = "授权令牌")
public class TokenStoreEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * id
	 */
	@TableId
	@Column(isKey = true, type = "bigint", length = 20, isNull = false, isAutoIncrement = true, comment = "id")
	private Long id;
	/**
	 * 用户Uuid
	 */
	@Column(length = 200, comment = "用户Uuid")
	private String userUuid;
	/**
	 * 授权码
	 */
	@Column(length = 200, comment = "授权码")
	private String authCode;
	/**
	 * 访问令牌
	 */
	@Column(length = 200, comment = "访问令牌")
	private String accessToken;
	/**
	 * 访问令牌有效时长(秒)
	 */
	@Column(type = "bigint", length = 20, comment = "访问令牌有效时长(秒)")
	private Long accessTokenExpiredIn;
	/**
	 * 刷新令牌
	 */
	@Column(length = 200, comment = "刷新令牌")
	private String refreshToken;
	/**
	 * 刷新令牌有效时长(秒)
	 */
	@Column(type = "bigint", length = 20, comment = "刷新令牌有效时长(秒)")
	private Long refreshTokenExpiredIn;
	/**
	 * 创建时间
	 */
	@Column(type = "datetime", comment = "创建时间")
	private Date createTime;
	/**
	 * 更新时间
	 */
	@Column(type = "datetime", comment = "更新时间")
	private Date updateTime;

	/**
	 * 设置：id
	 */
	public void setId(Long id) {
		this.id = id;
	}
	/**
	 * 获取：id
	 */
	public Long getId() {
		return id;
	}
	/**
	 * 设置：用户Uuid
	 */
	public void setUserUuid(String userUuid) {
		this.userUuid = userUuid;
	}
	/**
	 * 获取：用户Uuid
	 */
	public String getUserUuid() {
		return userUuid;
	}
	/**
	 * 设置：授权码
	 */
	public void setAuthCode(String authCode) {
		this.authCode = authCode;
	}
	/**
	 * 获取：授权码
	 */
	public String getAuthCode() {
		return authCode;
	}
	/**
	 * 设置：访问令牌
	 */
	public void setAccessToken(String accessToken) {
		this.accessToken = accessToken;
	}
	/**
	 * 获取：访问令牌
	 */
	public String getAccessToken() {
		return accessToken;
	}
	/**
	 * 设置：访问令牌有效时长(秒)
	 */
	public void setAccessTokenExpiredIn(Long accessTokenExpiredIn) {
		this.accessTokenExpiredIn = accessTokenExpiredIn;
	}
	/**
	 * 获取：访问令牌有效时长(秒)
	 */
	public Long getAccessTokenExpiredIn() {
		return accessTokenExpiredIn;
	}
	/**
	 * 设置：刷新令牌
	 */
	public void setRefreshToken(String refreshToken) {
		this.refreshToken = refreshToken;
	}
	/**
	 * 获取：刷新令牌
	 */
	public String getRefreshToken() {
		return refreshToken;
	}
	/**
	 * 设置：刷新令牌有效时长(秒)
	 */
	public void setRefreshTokenExpiredIn(Long refreshTokenExpiredIn) {
		this.refreshTokenExpiredIn = refreshTokenExpiredIn;
	}
	/**
	 * 获取：刷新令牌有效时长(秒)
	 */
	public Long getRefreshTokenExpiredIn() {
		return refreshTokenExpiredIn;
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

	public Date getUpdateTime() {
		return updateTime;
	}

	public void setUpdateTime(Date updateTime) {
		this.updateTime = updateTime;
	}
}
