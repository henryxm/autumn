package cn.org.autumn.modules.sms.entity;

import com.baomidou.mybatisplus.annotations.TableId;
import com.baomidou.mybatisplus.annotations.TableName;
import cn.org.autumn.table.annotation.Column;
import cn.org.autumn.table.annotation.Table;
import cn.org.autumn.table.data.DataType;

import java.io.Serializable;
import java.util.Date;

/**
 * 短信签名
 * 
 * @author Shaohua
 * @email henryxm@163.com
 * @date 2018-10
 */
@TableName("tb_aliyun_sms_sign")
@Table(value = "tb_aliyun_sms_sign", comment = "短信签名")
public class AliyunSmsSignEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 用户ID
	 */
	@TableId
	@Column(isKey = true, type = DataType.BIGINT, length = 20, isNull = false, isAutoIncrement = true, comment = "用户ID")
	private Long id;
	/**
	 * 签名名称
	 */
	@Column(comment = "签名名称")
	private String name;
	/**
	 * 签名类型
	 */
	@Column(comment = "签名类型")
	private String type;

	/**
	 * 设置：用户ID
	 */
	public void setId(Long id) {
		this.id = id;
	}
	/**
	 * 获取：用户ID
	 */
	public Long getId() {
		return id;
	}
	/**
	 * 设置：签名名称
	 */
	public void setName(String name) {
		this.name = name;
	}
	/**
	 * 获取：签名名称
	 */
	public String getName() {
		return name;
	}
	/**
	 * 设置：签名类型
	 */
	public void setType(String type) {
		this.type = type;
	}
	/**
	 * 获取：签名类型
	 */
	public String getType() {
		return type;
	}
}
