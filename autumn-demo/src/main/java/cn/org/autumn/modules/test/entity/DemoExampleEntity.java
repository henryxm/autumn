package cn.org.autumn.modules.test.entity;

import com.baomidou.mybatisplus.annotations.*;
import cn.org.autumn.table.annotation.*;
import cn.org.autumn.table.data.DataType;


import java.io.Serializable;
import java.util.Date;

/**
 * 测试例子
 * 
 * @author Shaohua Xu
 * @email henryxm@163.com
 * @date 2020-10
 */

@TableName("test_demo_example")
@Table(value = "test_demo_example", comment = "测试例子")
public class DemoExampleEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * ID
	 */
	@TableId
	@Column(isKey = true, type = "bigint", length = 20, isNull = false, isAutoIncrement = true, comment = "ID")
	private Long id;
	/**
	 * 例子字段
	 */
	@Column(comment = "例子字段")
	private String example;

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
	 * 设置：例子字段
	 */
	public void setExample(String example) {
		this.example = example;
	}
	/**
	 * 获取：例子字段
	 */
	public String getExample() {
		return example;
	}
}
