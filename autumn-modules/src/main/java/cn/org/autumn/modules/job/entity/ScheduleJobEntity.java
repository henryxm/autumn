package cn.org.autumn.modules.job.entity;

import cn.org.autumn.table.annotation.Column;
import cn.org.autumn.table.annotation.Table;
import cn.org.autumn.table.data.DataType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;

import jakarta.validation.constraints.NotBlank;
import java.io.Serializable;
import java.util.Date;

@Getter
@Setter
@TableName("sys_schedule_job")
@Table(module = "job", value = "sys_schedule_job", comment = "定时任务")
public class ScheduleJobEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String JOB_PARAM_KEY = "JOB_PARAM_KEY";

    @TableId
    @Column(isKey = true, type = DataType.BIGINT, length = 20, isNull = false, isAutoIncrement = true, comment = "任务id")
    private Long jobId;

    @NotBlank(message = "Bean名称不能为空")
    @Column(length = 200, comment = "BeanName")
    private String beanName;

    @NotBlank(message = "方法名称不能为空")
    @Column(length = 100, comment = "方法名")
    private String methodName;

    @Column(length = 2000, comment = "参数")
    private String params;

    @NotBlank(message = "cron表达式不能为空")
    @Column(length = 100, comment = "Cron表达式")
    private String cronExpression;

    @Column(type = DataType.INT, comment = "任务状态,0:正常,1:暂停")
    private Integer status;

    @Column(comment = "任务执行模式")
    @TableField("`mode`")
    private String mode;

    @Column(comment = "备注")
    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Column(type = DataType.DATETIME, comment = "创建时间")
    private Date createTime;
}
