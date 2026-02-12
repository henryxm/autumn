package cn.org.autumn.modules.job.entity;

import cn.org.autumn.table.annotation.Column;
import cn.org.autumn.table.annotation.Table;
import cn.org.autumn.table.data.DataType;
import com.baomidou.mybatisplus.annotations.TableId;
import com.baomidou.mybatisplus.annotations.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;

@Getter
@Setter
@TableName("sys_schedule_job_log")
@Table(prefix = "job", value = "sys_schedule_job_log", comment = "任务日志")
public class ScheduleJobLogEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId
    @Column(isKey = true, type = DataType.BIGINT, length = 20, isNull = false, isAutoIncrement = true, comment = "任务日志id")
    private Long logId;

    @Column(type = DataType.BIGINT, length = 20, isNull = false, defaultValue = "0", comment = "任务id")
    private Long jobId;

    @Column(length = 200, comment = "BeanName")
    private String beanName;

    @Column(length = 100, comment = "方法名")
    private String methodName;

    @Column(length = 2000, comment = "参数")
    private String params;

    @Column(type = DataType.INT, comment = "任务状态,0:成功,1:失败")
    private Integer status;

    @Column(length = 2000, comment = "失败信息")
    private String error;

    @Column(type = DataType.INT, length = 11, isNull = false, defaultValue = "0", comment = "耗时(单位：毫秒)")
    private Integer times;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Column(type = DataType.DATETIME, comment = "创建时间")
    private Date createTime;
}
