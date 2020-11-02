package cn.org.autumn.modules.wall.entity;

import com.baomidou.mybatisplus.annotations.*;
import cn.org.autumn.table.annotation.*;
import cn.org.autumn.table.data.DataType;


import java.io.Serializable;
import java.util.Date;

/**
 * 主机统计
 * 
 * @author Shaohua Xu
 * @email henryxm@163.com
 * @date 2020-11
 */
@UniqueKeys({@UniqueKey(name = "host", fields = {@UniqueKeyFields(field = "host")})})
@TableName("wall_host")
@Table(value = "wall_host", comment = "主机统计")
public class HostEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * id
	 */
	@TableId
	@Column(isKey = true, type = "bigint", length = 20, isNull = false, isAutoIncrement = true, comment = "id")
	private Long id;
	/**
	 * 主机地址
	 */
	@Column(isNull = false, comment = "主机地址")
	private String host;
	/**
	 * 访问次数
	 */
	@Column(type = "bigint", length = 20, comment = "访问次数")
	private Long count;
	/**
	 * 禁用
	 */
	@Column(type = "int", length = 10, defaultValue = "0", comment = "禁用")
	private Integer forbidden;
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
	 * 设置：主机地址
	 */
	public void setHost(String host) {
		this.host = host;
	}
	/**
	 * 获取：主机地址
	 */
	public String getHost() {
		return host;
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
	 * 设置：禁用
	 */
	public void setForbidden(Integer forbidden) {
		this.forbidden = forbidden;
	}
	/**
	 * 获取：禁用
	 */
	public Integer getForbidden() {
		return forbidden;
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
}
