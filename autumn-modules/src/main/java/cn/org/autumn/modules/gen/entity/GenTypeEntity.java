package cn.org.autumn.modules.gen.entity;

import com.baomidou.mybatisplus.annotations.TableId;
import com.baomidou.mybatisplus.annotations.TableName;
import cn.org.autumn.table.annotation.Column;
import cn.org.autumn.table.annotation.Table;
import cn.org.autumn.table.data.DataType;

import java.io.Serializable;
import java.util.Date;

/**
 * 代码生成设置
 * 
 * @author Shaohua Xu
 * @email henryxm@163.com
 * @date 2018-10
 */
@TableName("sys_gen_type")
@Table(value = "sys_gen_type", comment = "生成方案")
public class GenTypeEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 序列号
	 */
	@TableId
	@Column(isKey = true, type = "bigint", length = 20, isNull = false, isAutoIncrement = true, comment = "序列号")
	private Long id;
	/**
	 * 数据库类型
	 */
	@Column(length = 100, comment = "数据库类型")
	private String databaseType;
	/**
	 * 程序根包名
	 */
	@Column(comment = "程序根包名")
	private String rootPackage;
	/**
	 * 模块根包名
	 */
	@Column(comment = "模块根包名")
	private String modulePackage;
	/**
	 * 模块名(用于包名)
	 */
	@Column(comment = "模块名(用于包名)")
	private String moduleName;
	/**
	 * 模块名称(用于目录)
	 */
	@Column(comment = "模块名称(用于目录)")
	private String moduleText;

	/**
	 * 模块名称(用于目录)
	 */
	@Column(comment = "模块ID(用于目录)")
	private String moduleId;

	/**
	 * 作者名字
	 */
	@Column(comment = "作者名字")
	private String authorName;
	/**
	 * 作者邮箱
	 */
	@Column(comment = "作者邮箱")
	private String email;
	/**
	 * 表前缀
	 */
	@Column(comment = "表前缀")
	private String tablePrefix;
	/**
	 * 表字段映射
	 */
	@Column(length = 5000, comment = "表字段映射")
	private String mappingString;

	/**
	 * 设置：序列号
	 */
	public void setId(Long id) {
		this.id = id;
	}
	/**
	 * 获取：序列号
	 */
	public Long getId() {
		return id;
	}
	/**
	 * 设置：数据库类型
	 */
	public void setDatabaseType(String databaseType) {
		this.databaseType = databaseType;
	}
	/**
	 * 获取：数据库类型
	 */
	public String getDatabaseType() {
		return databaseType;
	}
	/**
	 * 设置：程序根包名
	 */
	public void setRootPackage(String rootPackage) {
		this.rootPackage = rootPackage;
	}
	/**
	 * 获取：程序根包名
	 */
	public String getRootPackage() {
		return rootPackage;
	}
	/**
	 * 设置：模块根包名
	 */
	public void setModulePackage(String modulePackage) {
		this.modulePackage = modulePackage;
	}
	/**
	 * 获取：模块根包名
	 */
	public String getModulePackage() {
		return modulePackage;
	}
	/**
	 * 设置：模块名(用于包名)
	 */
	public void setModuleName(String moduleName) {
		this.moduleName = moduleName;
	}
	/**
	 * 获取：模块名(用于包名)
	 */
	public String getModuleName() {
		return moduleName;
	}
	/**
	 * 设置：模块名称(用于目录)
	 */
	public void setModuleText(String moduleText) {
		this.moduleText = moduleText;
	}
	/**
	 * 获取：模块名称(用于目录)
	 */
	public String getModuleText() {
		return moduleText;
	}
	/**
	 * 设置：作者名字
	 */
	public void setAuthorName(String authorName) {
		this.authorName = authorName;
	}
	/**
	 * 获取：作者名字
	 */
	public String getAuthorName() {
		return authorName;
	}
	/**
	 * 设置：作者邮箱
	 */
	public void setEmail(String email) {
		this.email = email;
	}
	/**
	 * 获取：作者邮箱
	 */
	public String getEmail() {
		return email;
	}
	/**
	 * 设置：表前缀
	 */
	public void setTablePrefix(String tablePrefix) {
		this.tablePrefix = tablePrefix;
	}
	/**
	 * 获取：表前缀
	 */
	public String getTablePrefix() {
		return tablePrefix;
	}
	/**
	 * 设置：表字段映射
	 */
	public void setMappingString(String mappingString) {
		this.mappingString = mappingString;
	}
	/**
	 * 获取：表字段映射
	 */
	public String getMappingString() {
		return mappingString;
	}

	public String getModuleId() {
		return moduleId;
	}

	public void setModuleId(String moduleId) {
		this.moduleId = moduleId;
	}
}
