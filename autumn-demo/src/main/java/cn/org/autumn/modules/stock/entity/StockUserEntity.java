package cn.org.autumn.modules.stock.entity;

import com.baomidou.mybatisplus.annotations.TableId;
import com.baomidou.mybatisplus.annotations.TableName;
import cn.org.autumn.table.annotation.Column;
import cn.org.autumn.table.annotation.Table;
import cn.org.autumn.table.data.DataType;

import java.io.Serializable;
import java.util.Date;

/**
 * 股票用户
 * 
 * @author Shaohua
 * @email henryxm@163.com
 * @date 2018-10
 */
@TableName("tb_stock_user")
@Table(value = "tb_stock_user", comment = "股票用户")
public class StockUserEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * ID
	 */
	@TableId
	@Column(isKey = true, type = DataType.BIGINT, length = 20, isNull = false, isAutoIncrement = true, comment = "ID")
	private Long id;
	/**
	 * UUID
	 */
	@Column(comment = "UUID")
	private String uuid;
	/**
	 * 用户名
	 */
	@Column(comment = "用户名")
	private String username;
	/**
	 * 性别
	 */
	@Column(comment = "性别")
	private String sex;
	/**
	 * 手机号码
	 */
	@Column(comment = "手机号码")
	private String mobile;
	/**
	 * 年龄
	 */
	@Column(comment = "年龄")
	private String age;
	/**
	 * 职业
	 */
	@Column(comment = "职业")
	private String job;

	/**
	 * 设置：ID
	 */
	public void setId(Long id) {
		this.id = id;
	}
	/**
	 * 获取：ID
	 */
	public Long getId() {
		return id;
	}
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
	 * 设置：性别
	 */
	public void setSex(String sex) {
		this.sex = sex;
	}
	/**
	 * 获取：性别
	 */
	public String getSex() {
		return sex;
	}
	/**
	 * 设置：手机号码
	 */
	public void setMobile(String mobile) {
		this.mobile = mobile;
	}
	/**
	 * 获取：手机号码
	 */
	public String getMobile() {
		return mobile;
	}
	/**
	 * 设置：年龄
	 */
	public void setAge(String age) {
		this.age = age;
	}
	/**
	 * 获取：年龄
	 */
	public String getAge() {
		return age;
	}
	/**
	 * 设置：职业
	 */
	public void setJob(String job) {
		this.job = job;
	}
	/**
	 * 获取：职业
	 */
	public String getJob() {
		return job;
	}
}
