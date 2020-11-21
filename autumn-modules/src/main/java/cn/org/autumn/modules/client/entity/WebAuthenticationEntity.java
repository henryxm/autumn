package cn.org.autumn.modules.client.entity;

import com.baomidou.mybatisplus.annotations.*;
import cn.org.autumn.table.annotation.*;
import cn.org.autumn.table.data.DataType;


import java.io.Serializable;
import java.util.Date;

/**
 * 网站客户端
 * 
 * @author Shaohua Xu
 * @email henryxm@163.com
 * @date 2020-11
 */

@TableName("client_web_authentication")
@Table(value = "client_web_authentication", comment = "网站客户端")
public class WebAuthenticationEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * id
	 */
	@TableId
	@Column(isKey = true, type = "bigint", length = 20, isNull = false, isAutoIncrement = true, comment = "id")
	private Long id;
	/**
	 * 客户端名字
	 */
	@Column(length = 200, comment = "客户端名字")
	private String name;
	/**
	 * 客户端ID
	 */
	@Column(length = 200, comment = "客户端ID")
	private String clientId;
	/**
	 * 客户端密匙
	 */
	@Column(length = 200, comment = "客户端密匙")
	private String clientSecret;
	/**
	 * 重定向地址
	 */
	@Column(length = 500, comment = "重定向地址")
	private String redirectUri;
	/**
	 * 授权码地址
	 */
	@Column(length = 200, comment = "授权码地址")
	private String authorizeUri;
	/**
	 * Token地址
	 */
	@Column(length = 200, comment = "Token地址")
	private String accessTokenUri;
	/**
	 * 用户信息地址
	 */
	@Column(length = 200, comment = "用户信息地址")
	private String userInfoUri;
	/**
	 * 范围
	 */
	@Column(length = 200, comment = "范围")
	private String scope;
	/**
	 * 状态
	 */
	@Column(length = 200, comment = "状态")
	private String state;
	/**
	 * 描述信息
	 */
	@Column(length = 500, comment = "描述信息")
	private String description;
	/**
	 * 创建时间
	 */
	@Column(type = "datetime", comment = "创建时间")
	private Date createTime;

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
	 * 设置：客户端名字
	 */
	public void setName(String name) {
		this.name = name;
	}
	/**
	 * 获取：客户端名字
	 */
	public String getName() {
		return name;
	}
	/**
	 * 设置：客户端ID
	 */
	public void setClientId(String clientId) {
		this.clientId = clientId;
	}
	/**
	 * 获取：客户端ID
	 */
	public String getClientId() {
		return clientId;
	}
	/**
	 * 设置：客户端密匙
	 */
	public void setClientSecret(String clientSecret) {
		this.clientSecret = clientSecret;
	}
	/**
	 * 获取：客户端密匙
	 */
	public String getClientSecret() {
		return clientSecret;
	}
	/**
	 * 设置：重定向地址
	 */
	public void setRedirectUri(String redirectUri) {
		this.redirectUri = redirectUri;
	}
	/**
	 * 获取：重定向地址
	 */
	public String getRedirectUri() {
		return redirectUri;
	}
	/**
	 * 设置：授权码地址
	 */
	public void setAuthorizeUri(String authorizeUri) {
		this.authorizeUri = authorizeUri;
	}
	/**
	 * 获取：授权码地址
	 */
	public String getAuthorizeUri() {
		return authorizeUri;
	}
	/**
	 * 设置：Token地址
	 */
	public void setAccessTokenUri(String accessTokenUri) {
		this.accessTokenUri = accessTokenUri;
	}
	/**
	 * 获取：Token地址
	 */
	public String getAccessTokenUri() {
		return accessTokenUri;
	}
	/**
	 * 设置：用户信息地址
	 */
	public void setUserInfoUri(String userInfoUri) {
		this.userInfoUri = userInfoUri;
	}
	/**
	 * 获取：用户信息地址
	 */
	public String getUserInfoUri() {
		return userInfoUri;
	}
	/**
	 * 设置：范围
	 */
	public void setScope(String scope) {
		this.scope = scope;
	}
	/**
	 * 获取：范围
	 */
	public String getScope() {
		return scope;
	}
	/**
	 * 设置：状态
	 */
	public void setState(String state) {
		this.state = state;
	}
	/**
	 * 获取：状态
	 */
	public String getState() {
		return state;
	}
	/**
	 * 设置：描述信息
	 */
	public void setDescription(String description) {
		this.description = description;
	}
	/**
	 * 获取：描述信息
	 */
	public String getDescription() {
		return description;
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
}
