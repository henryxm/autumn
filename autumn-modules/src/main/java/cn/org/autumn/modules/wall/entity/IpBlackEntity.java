package cn.org.autumn.modules.wall.entity;

import com.baomidou.mybatisplus.annotations.*;
import cn.org.autumn.table.annotation.*;
import cn.org.autumn.table.data.DataType;


import java.io.Serializable;
import java.util.Date;

/**
 * IP黑名单
 * 
 * @author Shaohua Xu
 * @email henryxm@163.com
 * @date 2020-11
 */
@UniqueKeys({@UniqueKey(name = "ip", fields = {@UniqueKeyFields(field = "ip")})})
@TableName("wall_ip_black")
@Table(value = "wall_ip_black", comment = "IP黑名单")
public class IpBlackEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * id
	 */
	@TableId
	@Column(isKey = true, type = "bigint", length = 20, isNull = false, isAutoIncrement = true, comment = "id")
	private Long id;
	/**
	 * IP地址
	 */
	@Column(isNull = false, comment = "IP地址")
	private String ip;
	/**
	 * 访问次数
	 */
	@Column(type = "bigint", length = 20, comment = "访问次数")
	private Long count;
	/**
	 * 可用
	 */
	@Column(type = "int", length = 10, defaultValue = "0", comment = "可用")
	private Integer available;
	/**
	 * 标签说明
	 */
	@Column(comment = "标签说明")
	private String tag;
	/**
	 * 描述信息
	 */
	@Column(comment = "描述信息")
	private String description;
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
	 * 设置：IP地址
	 */
	public void setIp(String ip) {
		this.ip = ip;
	}
	/**
	 * 获取：IP地址
	 */
	public String getIp() {
		return ip;
	}
	/**
	 * 设置：访问次数
	 */
	public void setCount(Long count) {
		this.count = count;
	}
	/**
	 * 获取：访问次数
	 */
	public Long getCount() {
		return count;
	}
	/**
	 * 设置：可用
	 */
	public void setAvailable(Integer available) {
		this.available = available;
	}
	/**
	 * 获取：可用
	 */
	public Integer getAvailable() {
		return available;
	}
	/**
	 * 设置：标签说明
	 */
	public void setTag(String tag) {
		this.tag = tag;
	}
	/**
	 * 获取：标签说明
	 */
	public String getTag() {
		return tag;
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
}
