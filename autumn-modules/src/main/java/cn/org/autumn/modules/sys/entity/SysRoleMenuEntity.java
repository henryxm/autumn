package cn.org.autumn.modules.sys.entity;

import cn.org.autumn.table.annotation.*;
import cn.org.autumn.table.data.DataType;
import com.baomidou.mybatisplus.annotations.TableId;
import com.baomidou.mybatisplus.annotations.TableName;

import java.io.Serializable;

/**
 * 角色与菜单对应关系
 */
@TableName("sys_role_menu")
@Table(value = "sys_role_menu", comment = "角色与菜单对应关系")
@Indexes(@Index(name = "role_keymenu_key", indexType = IndexTypeEnum.UNIQUE, indexMethod = IndexMethodEnum.BTREE, fields = {@IndexField(field = "role_key", length = 100), @IndexField(field = "menu_key", length = 100)}))
public class SysRoleMenuEntity implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId
    @Column(isKey = true, type = DataType.BIGINT, length = 20, isNull = false, isAutoIncrement = true, comment = "id")
    private Long id;

    @Column(length = 100, comment = "角色标识")
    private String roleKey;

    @Column(length = 100, comment = "MenuKey")
    private String menuKey;

    /**
     * 设置：
     *
     * @param id
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * 获取：
     *
     * @return Long
     */
    public Long getId() {
        return id;
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
