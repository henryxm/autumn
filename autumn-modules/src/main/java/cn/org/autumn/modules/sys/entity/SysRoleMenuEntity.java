package cn.org.autumn.modules.sys.entity;

import cn.org.autumn.table.annotation.Column;
import cn.org.autumn.table.annotation.Table;
import cn.org.autumn.table.data.DataType;
import com.baomidou.mybatisplus.annotations.TableId;
import com.baomidou.mybatisplus.annotations.TableName;

import java.io.Serializable;

/**
 * 角色与菜单对应关系
 */
@TableName("sys_role_menu")
@Table(value = "sys_role_menu", comment = "角色与菜单对应关系")
public class SysRoleMenuEntity implements Serializable {
	private static final long serialVersionUID = 1L;
	@TableId
	@Column(isKey = true, type = DataType.BIGINT, length = 20, isNull = false, isAutoIncrement = true, comment = "id")
	private Long id;

	/**
	 * 角色ID
	 */
	@Column(length = 11, type = DataType.INT, comment = "角色ID")
	private Long roleId;


	@Column(length = 100, comment = "角色标识")
	private String roleKey;

	/**
	 * 菜单ID
	 */
	@Column(length = 11, type = DataType.INT, comment = "菜单ID")
	private Long menuId;

	@Column(length = 50, comment = "MenuKey")
	private String menuKey;

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
	 * 设置：菜单ID
	 * @param menuId 菜单ID
	 */
	public void setMenuId(Long menuId) {
		this.menuId = menuId;
	}

	/**
	 * 获取：菜单ID
	 * @return Long
	 */
	public Long getMenuId() {
		return menuId;
	}

	public String getRoleKey() {
		return roleKey;
	}

	public void setRoleKey(String roleKey) {
		this.roleKey = roleKey;
	}

	public String getMenuKey() {
		return menuKey;
	}

	public void setMenuKey(String menuKey) {
		this.menuKey = menuKey;
	}
}
