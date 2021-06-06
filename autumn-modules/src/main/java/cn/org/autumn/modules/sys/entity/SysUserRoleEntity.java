package cn.org.autumn.modules.sys.entity;

import cn.org.autumn.table.annotation.*;
import cn.org.autumn.table.data.DataType;
import com.baomidou.mybatisplus.annotations.TableId;
import com.baomidou.mybatisplus.annotations.TableName;

import java.io.Serializable;

/**
 * 用户与角色对应关系
 */
@TableName("sys_user_role")
@Table(value = "sys_user_role", comment = "用户与角色对应关系")
@Indexes(@Index(name = "user_uuidrole_key", indexType = IndexTypeEnum.UNIQUE, indexMethod = IndexMethodEnum.BTREE,
        fields = {@IndexField(field = "user_uuid", length = 50), @IndexField(field = "role_key", length = 100)}))
public class SysUserRoleEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId
    @Column(isKey = true, type = DataType.BIGINT, length = 20, isNull = false, isAutoIncrement = true, comment = "id")
    private Long id;

    @Column(length = 100, comment = "用户UUID")
    private String userUuid;

    @Column(length = 100, comment = "用户名字")
    private String username;

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
