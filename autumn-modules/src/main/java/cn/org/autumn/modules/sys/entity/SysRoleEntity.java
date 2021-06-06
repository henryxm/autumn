package cn.org.autumn.modules.sys.entity;

import cn.org.autumn.table.annotation.Column;
import cn.org.autumn.table.annotation.Table;
import cn.org.autumn.table.data.DataType;
import com.baomidou.mybatisplus.annotations.TableField;
import com.baomidou.mybatisplus.annotations.TableId;
import com.baomidou.mybatisplus.annotations.TableName;
import com.baomidou.mybatisplus.enums.IdType;
import com.fasterxml.jackson.annotation.JsonFormat;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 角色
 */
@TableName("sys_role")
@Table(value = "sys_role", comment = "角色")
public class SysRoleEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId
    @Column(isKey = true, type = DataType.BIGINT, length = 20, isNull = false, isAutoIncrement = true, comment = "id")
    private Long roleId;

    @NotBlank(message = "角色标识不能为空")
    @Column(length = 100, comment = "角色标识", isUnique = true)
    private String roleKey;
    /**
     * 角色名称
     */
    @NotBlank(message = "角色名称不能为空")
    @Column(length = 100, comment = "角色名称")
    private String roleName;

    @Column(length = 100, comment = "部门标识")
    private String deptKey;
    /**
     * 备注
     */
    @Column(length = 100, comment = "备注")
    private String remark;

    /**
     * 部门名称
     */
    @TableField(exist = false)
    private String deptName;

    @TableField(exist = false)
    private List<String> menuKeys;

    @TableField(exist = false)
    private List<String> deptKeys;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Column(type = DataType.DATETIME, comment = "创建时间")
    private Date createTime;

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }

    public String getRoleKey() {
        return roleKey;
    }

    public void setRoleKey(String roleKey) {
        this.roleKey = roleKey;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public String getDeptKey() {
        return deptKey;
    }

    public void setDeptKey(String deptKey) {
        this.deptKey = deptKey;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public String getDeptName() {
        return deptName;
    }

    public void setDeptName(String deptName) {
        this.deptName = deptName;
    }

    public List<String> getMenuKeys() {
        return menuKeys;
    }

    public void setMenuKeys(List<String> menuKeys) {
        this.menuKeys = menuKeys;
    }

    public List<String> getDeptKeys() {
        return deptKeys;
    }

    public void setDeptKeys(List<String> deptKeys) {
        this.deptKeys = deptKeys;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }
}
