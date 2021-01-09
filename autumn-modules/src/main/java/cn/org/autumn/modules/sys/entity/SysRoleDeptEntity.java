package cn.org.autumn.modules.sys.entity;

import cn.org.autumn.table.annotation.Column;
import cn.org.autumn.table.annotation.Table;
import cn.org.autumn.table.data.DataType;
import com.baomidou.mybatisplus.annotations.TableId;
import com.baomidou.mybatisplus.annotations.TableName;

import java.io.Serializable;

/**
 * 角色与部门对应关系
 */
@TableName("sys_role_dept")
@Table(value = "sys_role_dept", comment = "角色与部门对应关系")
public class SysRoleDeptEntity implements Serializable {
	private static final long serialVersionUID = 1L;
	@TableId
	@Column(isKey = true, type = DataType.BIGINT, length = 20, isNull = false, isAutoIncrement = true, comment = "id")
	private Long id;

	/**
	 * 角色ID
	 */
	@Column(length = 11,  type = DataType.BIGINT,comment = "角色ID")
	private Long roleId;

	@Column(length = 100, comment = "角色标识")
	private String roleKey;

	/**
	 * 部门ID
	 */
	@Column(length = 11,  type = DataType.BIGINT,comment = "菜单ID")
	private Long deptId;

	@Column(length = 100, comment = "部门标识")
	private String deptKey;

	/**
	 * 设置：
	 * @param id 
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * 获取：
	 * @return Long
	 */
	public Long getId() {
		return id;
	}
	
	/**
	 * 设置：角色ID
	 * @param roleId 角色ID
	 */
	public void setRoleId(Long roleId) {
		this.roleId = roleId;
	}

	/**
	 * 获取：角色ID
	 * @return Long
	 */
	public Long getRoleId() {
		return roleId;
	}
	
	/**
	 * 设置：部门ID
	 * @param deptId 部门ID
	 */
	public void setDeptId(Long deptId) {
		this.deptId = deptId;
	}

	/**
	 * 获取：部门ID
	 * @return Long
	 */
	public Long getDeptId() {
		return deptId;
	}

	public String getRoleKey() {
		return roleKey;
	}

	public void setRoleKey(String roleKey) {
		this.roleKey = roleKey;
	}

	public String getDeptKey() {
		return deptKey;
	}

	public void setDeptKey(String deptKey) {
		this.deptKey = deptKey;
	}
}
