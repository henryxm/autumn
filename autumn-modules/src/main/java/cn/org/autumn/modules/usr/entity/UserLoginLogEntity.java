package cn.org.autumn.modules.usr.entity;

import com.baomidou.mybatisplus.annotations.*;
import cn.org.autumn.table.annotation.*;
import cn.org.autumn.table.data.DataType;


import java.io.Serializable;
import java.util.Date;

/**
 * 登录日志
 * 
 * @author Shaohua Xu
 * @email henryxm@163.com
 * @date 2020-11
 */

@TableName("usr_user_login_log")
@Table(value = "usr_user_login_log", comment = "登录日志")
public class UserLoginLogEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 日志ID
	 */
	@TableId
	@Column(isKey = true, type = "bigint", length = 20, isNull = false, isAutoIncrement = true, comment = "日志ID")
	private Long id;
	/**
	 * 用户ID
	 */
	@Column(type = "bigint", length = 20, comment = "用户ID")
	private Long userId;
	/**
	 * 用户名
	 */
	@Column(length = 50, comment = "用户名")
	private String username;
	/**
	 * 登录时间
	 */
	@Column(type = "datetime", comment = "登录时间")
	private Date loginTime;
	/**
	 * 登出时间
	 */
	@Column(type = "datetime", comment = "登出时间")
	private Date logoutTime;

	/**
	 * 设置：日志ID
	 */
	public void setId(Long id) {
		this.id = id;
	}
	/**
	 * 获取：日志ID
	 */
	public Long getId() {
		return id;
	}
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
	 * 设置：登录时间
	 */
	public void setLoginTime(Date loginTime) {
		this.loginTime = loginTime;
	}
	/**
	 * 获取：登录时间
	 */
	public Date getLoginTime() {
		return loginTime;
	}
	/**
	 * 设置：登出时间
	 */
	public void setLogoutTime(Date logoutTime) {
		this.logoutTime = logoutTime;
	}
	/**
	 * 获取：登出时间
	 */
	public Date getLogoutTime() {
		return logoutTime;
	}
}
