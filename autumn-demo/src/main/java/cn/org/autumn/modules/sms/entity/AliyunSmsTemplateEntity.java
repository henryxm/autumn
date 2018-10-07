package cn.org.autumn.modules.sms.entity;

import com.baomidou.mybatisplus.annotations.TableId;
import com.baomidou.mybatisplus.annotations.TableName;
import cn.org.autumn.table.annotation.Column;
import cn.org.autumn.table.annotation.Table;
import cn.org.autumn.table.data.DataType;

import java.io.Serializable;
import java.util.Date;

/**
 * 阿里云短信模板
 * 
 * @author Shaohua
 * @email henryxm@163.com
 * @date 2018-10
 */
@TableName("tb_aliyun_sms_template")
@Table(value = "tb_aliyun_sms_template", comment = "阿里云短信模板")
public class AliyunSmsTemplateEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 用户ID
	 */
	@TableId
	@Column(isKey = true, type = DataType.BIGINT, length = 20, isNull = false, isAutoIncrement = true, comment = "用户ID")
	private Long id;
	/**
	 * 模版类型
	 */
	@Column(comment = "模版类型")
	private String type;
	/**
	 * 模板代码
	 */
	@Column(comment = "模板代码")
	private String code;
	/**
	 * 模板名称
	 */
	@Column(comment = "模板名称")
	private String name;
	/**
	 * 模版内容
	 */
	@Column(comment = "模版内容")
	private String content;

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
	 * 设置：模版类型
	 */
	public void setType(String type) {
		this.type = type;
	}
	/**
	 * 获取：模版类型
	 */
	public String getType() {
		return type;
	}
	/**
	 * 设置：模板代码
	 */
	public void setCode(String code) {
		this.code = code;
	}
	/**
	 * 获取：模板代码
	 */
	public String getCode() {
		return code;
	}
	/**
	 * 设置：模板名称
	 */
	public void setName(String name) {
		this.name = name;
	}
	/**
	 * 获取：模板名称
	 */
	public String getName() {
		return name;
	}
	/**
	 * 设置：模版内容
	 */
	public void setContent(String content) {
		this.content = content;
	}
	/**
	 * 获取：模版内容
	 */
	public String getContent() {
		return content;
	}
}
