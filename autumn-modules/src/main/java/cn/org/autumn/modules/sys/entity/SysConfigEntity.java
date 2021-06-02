package cn.org.autumn.modules.sys.entity;

import cn.org.autumn.table.annotation.Column;
import cn.org.autumn.table.annotation.Table;
import cn.org.autumn.table.data.DataType;
import com.baomidou.mybatisplus.annotations.TableId;
import com.baomidou.mybatisplus.annotations.TableName;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * 系统配置信息
 */
@TableName("sys_config")
@Table(value = "sys_config", comment = "系统配置信息表")
public class SysConfigEntity implements Serializable {
    @TableId
    @Column(isKey = true, type = DataType.BIGINT, length = 20, isNull = false, isAutoIncrement = true, comment = "id")
    private Long id;

    @NotBlank(message = "参数名不能为空")
    @Column(comment = "key", isUnique = true)
    private String paramKey;

    @NotBlank(message = "参数值不能为空")
    @Column(type = DataType.TEXT, comment = "value")
    private String paramValue;

    @Column(type = DataType.TEXT, comment = "备注")
    private String remark;

    @Column(type = DataType.INT, length = 4, defaultValue = "1", comment = "状态   0：隐藏   1：显示")
    private int status;

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getParamKey() {
        return paramKey;
    }

    public void setParamKey(String paramKey) {
        this.paramKey = paramKey;
    }

    public String getParamValue() {
        return paramValue;
    }

    public void setParamValue(String paramValue) {
        this.paramValue = paramValue;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
