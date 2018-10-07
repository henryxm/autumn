package cn.org.autumn.modules.sms.entity;

import com.baomidou.mybatisplus.annotations.TableId;
import com.baomidou.mybatisplus.annotations.TableName;
import cn.org.autumn.table.annotation.Column;
import cn.org.autumn.table.annotation.Table;
import cn.org.autumn.table.data.DataType;

import java.io.Serializable;
import java.util.Date;

/**
 * 发送验证码
 * 
 * @author Shaohua
 * @email henryxm@163.com
 * @date 2018-10
 */
@TableName("tb_aliyun_sms_send")
@Table(value = "tb_aliyun_sms_send", comment = "发送验证码")
public class AliyunSmsSendEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 用户ID
	 */
	@TableId
	@Column(isKey = true, type = DataType.BIGINT, length = 20, isNull = false, isAutoIncrement = true, comment = "用户ID")
	private Long id;
	/**
	 * 验证码
	 */
	@Column(comment = "验证码")
	private String code;
	/**
	 * 结果码
	 */
	@Column(comment = "结果码")
	private String resultCode;
	/**
	 * 消息
	 */
	@Column(comment = "消息")
	private String message;
	/**
	 * 请求ID
	 */
	@Column(comment = "请求ID")
	private String requestId;

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
	 * 设置：验证码
	 */
	public void setCode(String code) {
		this.code = code;
	}
	/**
	 * 获取：验证码
	 */
	public String getCode() {
		return code;
	}
	/**
	 * 设置：结果码
	 */
	public void setResultCode(String resultCode) {
		this.resultCode = resultCode;
	}
	/**
	 * 获取：结果码
	 */
	public String getResultCode() {
		return resultCode;
	}
	/**
	 * 设置：消息
	 */
	public void setMessage(String message) {
		this.message = message;
	}
	/**
	 * 获取：消息
	 */
	public String getMessage() {
		return message;
	}
	/**
	 * 设置：请求ID
	 */
	public void setRequestId(String requestId) {
		this.requestId = requestId;
	}
	/**
	 * 获取：请求ID
	 */
	public String getRequestId() {
		return requestId;
	}
}
