package cn.org.autumn.modules.sys.entity;

import cn.org.autumn.table.annotation.Column;
import cn.org.autumn.table.annotation.Table;
import cn.org.autumn.table.data.DataType;
import com.baomidou.mybatisplus.annotations.TableField;
import com.baomidou.mybatisplus.annotations.TableId;
import com.baomidou.mybatisplus.annotations.TableName;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

/**
 * 菜单管理
 */
@TableName("sys_menu")
@Table(value = "sys_menu", comment = "菜单管理")
public class SysMenuEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId
    @Column(isKey = true, type = DataType.BIGINT, length = 20, isNull = false, isAutoIncrement = true, comment = "id")
    private Long menuId;

    @Column(length = 100, comment = "MenuKey", isUnique = true)
    private String menuKey;

    @Column(length = 100, comment = "ParentKey")
    private String parentKey;

    @Column(length = 50, comment = "语言标识")
    private String languageName;

    /**
     * 菜单名称
     */
    @Column(length = 50, comment = "菜单名称")
    private String name;

    /**
     * 父菜单名称
     */
    @TableField(exist = false)
    private String parentName;

    /**
     * 菜单URL
     */
    @Column(length = 200, comment = "菜单URL")
    private String url;

    /**
     * 授权(多个用逗号分隔，如：user:list,user:create)
     */
    @Column(length = 500, comment = "授权(多个用逗号分隔，如：user:list,user:create)")
    private String perms;

    /**
     * 类型     0：目录   1：菜单   2：按钮
     */
    @Column(length = 11, type = DataType.INT, comment = "类型   0：目录   1：菜单   2：按钮")
    private Integer type;

    /**
     * 菜单图标
     */
    @Column(length = 50, comment = "菜单图标")
    private String icon;

    /**
     * 排序
     */
    @Column(length = 50, type = DataType.INT, comment = "排序")
    private Integer orderNum;

    /**
     * ztree属性
     */
    @TableField(exist = false)
    private Boolean open;

    @TableField(exist = false)
    private List<?> list;

    public Long getMenuId() {
        return menuId;
    }

    public void setMenuId(Long menuId) {
        this.menuId = menuId;
    }

    public String getMenuKey() {
        return menuKey;
    }

    public void setMenuKey(String menuKey) {
        this.menuKey = menuKey;
    }

    public String getParentKey() {
        return parentKey;
    }

    public void setParentKey(String parentKey) {
        this.parentKey = parentKey;
    }

    public String getLanguageName() {
        return languageName;
    }

    public void setLanguageName(String languageName) {
        this.languageName = languageName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getParentName() {
        return parentName;
    }

    public void setParentName(String parentName) {
        this.parentName = parentName;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getPerms() {
        return perms;
    }

    public void setPerms(String perms) {
        this.perms = perms;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public Integer getOrderNum() {
        return orderNum;
    }

    public void setOrderNum(Integer orderNum) {
        this.orderNum = orderNum;
    }

    public Boolean getOpen() {
        return open;
    }

    public void setOpen(Boolean open) {
        this.open = open;
    }

    public List<?> getList() {
        return list;
    }

    public void setList(List<?> list) {
        this.list = list;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getMenuKey(), getLanguageName(), getName(), getUrl(), getPerms(), getType(), getIcon(), getOrderNum());
    }
}
