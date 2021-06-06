package cn.org.autumn.modules.sys.entity;

import cn.org.autumn.table.annotation.*;
import cn.org.autumn.table.data.DataType;
import com.baomidou.mybatisplus.annotations.TableId;
import com.baomidou.mybatisplus.annotations.TableName;

import java.io.Serializable;

/**
 * 角色与部门对应关系
 */
@TableName("sys_role_dept")
@Table(value = "sys_role_dept", comment = "角色与部门对应关系")
@Indexes(@Index(name = "role_keydept_key", indexType = IndexTypeEnum.UNIQUE, indexMethod = IndexMethodEnum.BTREE, fields = {@IndexField(field = "role_key", length = 100), @IndexField(field = "dept_key", length = 100)}))
public class SysRoleDeptEntity implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId
    @Column(isKey = true, type = DataType.BIGINT, length = 20, isNull = false, isAutoIncrement = true, comment = "id")
    private Long id;

    @Column(length = 100, comment = "角色标识")
    private String roleKey;

    @Column(length = 100, comment = "部门标识")
    private String deptKey;

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

    public String getDeptKey() {
        return deptKey;
    }

    public void setDeptKey(String deptKey) {
        this.deptKey = deptKey;
    }
}
