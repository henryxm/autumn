package cn.org.autumn.modules.sms.entity;

import com.baomidou.mybatisplus.annotations.TableId;
import com.baomidou.mybatisplus.annotations.TableName;
import cn.org.autumn.table.annotation.Column;
import cn.org.autumn.table.annotation.Table;
import cn.org.autumn.table.data.DataType;

import java.io.Serializable;
import java.util.Date;

/**
 * 阿里云授权码
 * 
 * @author Shaohua
 * @email henryxm@163.com
 * @date 2018-10
 */
@TableName("tb_aliyun_access_keys")
@Table(value = "tb_aliyun_access_keys", comment = "阿里云授权码")
public class AliyunAccessKeysEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 用户ID
	 */
	@TableId
	@Column(isKey = true, type = DataType.BIGINT, length = 20, isNull = false, isAutoIncrement = true, comment = "用户ID")
	private Long id;
	/**
	 * Access Key ID
	 */
	@Column(comment = "Access Key ID")
	private String accessKeyId;
	/**
	 * Access Key Secret
	 */
	@Column(comment = "Access Key Secret")
	private String accessKeySecret;
	/**
	 * 名称
	 */
	@Column(comment = "名称")
	private String name;
	/**
	 * 说明
	 */
	@Column(comment = "说明")
	private String description;

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
	 * 设置：Access Key ID
	 */
	public void setAccessKeyId(String accessKeyId) {
		this.accessKeyId = accessKeyId;
	}
	/**
	 * 获取：Access Key ID
	 */
	public String getAccessKeyId() {
		return accessKeyId;
	}
	/**
	 * 设置：Access Key Secret
	 */
	public void setAccessKeySecret(String accessKeySecret) {
		this.accessKeySecret = accessKeySecret;
	}
	/**
	 * 获取：Access Key Secret
	 */
	public String getAccessKeySecret() {
		return accessKeySecret;
	}
	/**
	 * 设置：名称
	 */
	public void setName(String name) {
		this.name = name;
	}
	/**
	 * 获取：名称
	 */
	public String getName() {
		return name;
	}
	/**
	 * 设置：说明
	 */
	public void setDescription(String description) {
		this.description = description;
	}
	/**
	 * 获取：说明
	 */
	public String getDescription() {
		return description;
	}
}
