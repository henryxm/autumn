package cn.org.autumn.modules.sys.entity;

import cn.org.autumn.table.annotation.Column;
import cn.org.autumn.table.annotation.Table;
import cn.org.autumn.table.data.DataType;
import com.baomidou.mybatisplus.annotations.TableId;
import com.baomidou.mybatisplus.annotations.TableLogic;
import com.baomidou.mybatisplus.annotations.TableName;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * 数据字典
 */
@TableName("sys_dict")
@Table(value = "sys_dict", comment = "数据字典表")
public class SysDictEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	@TableId
	@Column(isKey = true, type = DataType.BIGINT, length = 20, isNull = false, isAutoIncrement = true, comment = "id")
	private Long id;
	/**
	 * 字典名称
	 */
	@Column(length = 100, isNull = false, comment = "字典名称")
	@NotBlank(message="字典名称不能为空")
	private String name;
	/**
	 * 字典类型
	 */
	@NotBlank(message="字典类型不能为空")
	@Column(length = 100, isNull = false, comment = "字典类型")
	private String type;
	/**
	 * 字典码
	 */
	@NotBlank(message="字典码不能为空")
	@Column(length = 100, isNull = false, comment = "字典码")
	private String code;
	/**
	 * 字典值
	 */
	@NotBlank(message="字典值不能为空")
	@Column(length = 100, isNull = false, comment = "字典值")
	private String value;
	/**
	 * 排序
	 */
	@Column(type = DataType.INT, length = 11, defaultValue = "0", comment = "排序")
	private Integer orderNum;
	/**
	 * 备注
	 */
	@Column(comment = "备注")
	private String remark;
	/**
	 * 删除标记  -1：已删除  0：正常
	 */
	@TableLogic
	@Column(type = DataType.INT, length = 4, defaultValue = "0", comment = "删除标记  -1：已删除  0：正常")
	private Integer delFlag;

	/**
	 * 设置：
	 */
	public void setId(Long id) {
		this.id = id;
	}
	/**
	 * 获取：
	 */
	public Long getId() {
		return id;
	}
	/**
	 * 设置：字典名称
	 */
	public void setName(String name) {
		this.name = name;
	}
	/**
	 * 获取：字典名称
	 */
	public String getName() {
		return name;
	}
	/**
	 * 设置：字典类型
	 */
	public void setType(String type) {
		this.type = type;
	}
	/**
	 * 获取：字典类型
	 */
	public String getType() {
		return type;
	}
	/**
	 * 设置：字典码
	 */
	public void setCode(String code) {
		this.code = code;
	}
	/**
	 * 获取：字典码
	 */
	public String getCode() {
		return code;
	}
	/**
	 * 设置：字典值
	 */
	public void setValue(String value) {
		this.value = value;
	}
	/**
	 * 获取：字典值
	 */
	public String getValue() {
		return value;
	}
	/**
	 * 设置：排序
	 */
	public void setOrderNum(Integer orderNum) {
		this.orderNum = orderNum;
	}
	/**
	 * 获取：排序
	 */
	public Integer getOrderNum() {
		return orderNum;
	}
	/**
	 * 设置：备注
	 */
	public void setRemark(String remark) {
		this.remark = remark;
	}
	/**
	 * 获取：备注
	 */
	public String getRemark() {
		return remark;
	}
	/**
	 * 设置：删除标记  -1：已删除  0：正常
	 */
	public void setDelFlag(Integer delFlag) {
		this.delFlag = delFlag;
	}
	/**
	 * 获取：删除标记  -1：已删除  0：正常
	 */
	public Integer getDelFlag() {
		return delFlag;
	}
}
