package cn.org.autumn.modules.sys.entity;

import cn.org.autumn.table.annotation.Column;
import cn.org.autumn.table.annotation.Table;
import cn.org.autumn.table.data.DataType;
import com.baomidou.mybatisplus.annotations.TableField;
import com.baomidou.mybatisplus.annotations.TableId;
import com.baomidou.mybatisplus.annotations.TableName;

import java.io.Serializable;
import java.util.List;

@TableName("sys_category")
@Table(value = "sys_category", comment = "系统配置类型表")
public class SysCategoryEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId
    @Column(isKey = true, type = DataType.BIGINT, length = 20, isNull = false, isAutoIncrement = true, comment = "id")
    private Long id;

    @Column(comment = "分类", isUnique = true)
    private String category;

    @Column(comment = "名称")
    private String name;

    @Column(comment = "状态", defaultValue = "0")
    private int status;

    @Column(comment = "冻结:冻结后不更新状态和描述", defaultValue = "0")
    private boolean frozen;

    @Column(comment = "描述")
    private String description;

    @TableField(exist = false)
    List<SysConfigEntity> configs;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public boolean isFrozen() {
        return frozen;
    }

    public void setFrozen(boolean frozen) {
        this.frozen = frozen;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<SysConfigEntity> getConfigs() {
        return configs;
    }

    public void setConfigs(List<SysConfigEntity> configs) {
        this.configs = configs;
    }
}
