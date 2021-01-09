package cn.org.autumn.modules.sys.entity;

import cn.org.autumn.table.annotation.Column;
import cn.org.autumn.table.annotation.Table;
import cn.org.autumn.table.data.DataType;
import com.baomidou.mybatisplus.annotations.TableId;
import com.baomidou.mybatisplus.annotations.TableName;

import java.io.Serializable;

/**
 * 用户与角色对应关系
 */
@TableName("sys_user_role")
@Table(value = "sys_user_role", comment = "用户与角色对应关系")
public class SysUserRoleEntity implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId
    @Column(isKey = true, type = DataType.BIGINT, length = 20, isNull = false, isAutoIncrement = true, comment = "id")
    private Long id;

    /**
     * 用户ID
     */
    @Column(type = DataType.BIGINT, length = 20, comment = "用户ID")
    private Long userId;

    @Column(length = 100, comment = "用户UUID")
    private String userUuid;

    @Column(length = 100, comment = "用户名字")
    private String username;

    /**
     * 角色ID
     */
    @Column(type = DataType.BIGINT, length = 20, comment = "角色ID")
    private Long roleId;

    @Column(length = 100, comment = "角色标识")
    private String roleKey;

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

    /**
     * 设置：用户ID
     *
     * @param userId 用户ID
     */
    public void setUserId(Long userId) {
        this.userId = userId;
    }

    /**
     * 获取：用户ID
     *
     * @return Long
     */
    public Long getUserId() {
        return userId;
    }

    /**
     * 设置：角色ID
     *
     * @param roleId 角色ID
     */
    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }

    /**
     * 获取：角色ID
     *
     * @return Long
     */
    public Long getRoleId() {
        return roleId;
    }

    public String getUserUuid() {
        return userUuid;
    }

    public void setUserUuid(String userUuid) {
        this.userUuid = userUuid;
    }

    public String getRoleKey() {
        return roleKey;
    }

    public void setRoleKey(String roleKey) {
        this.roleKey = roleKey;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
