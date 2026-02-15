package cn.org.autumn.modules.job.entity;

import cn.org.autumn.table.annotation.Column;
import cn.org.autumn.table.annotation.Table;
import cn.org.autumn.table.data.DataType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;

/**
 * 定时任务服务器分配实体
 * <p>
 * 存储所有定时任务的服务器分配信息，支持通过管理界面动态修改任务的执行服务器。
 */
@Getter
@Setter
@TableName("sys_schedule_assign")
@Table(module = "job", value = "sys_schedule_assign", comment = "定时任务分配")
public class ScheduleAssignEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId
    @Column(isKey = true, type = DataType.BIGINT, length = 20, isNull = false, isAutoIncrement = true, comment = "id")
    private Long id;

    /**
     * 任务ID，格式为 "category|className"，与 LoopJob.JobInfo.id 一致
     */
    @Column(length = 500, isNull = false, comment = "任务ID")
    private String jobId;

    /**
     * 任务显示名称
     */
    @Column(length = 200, comment = "任务名称")
    private String jobName;

    /**
     * 任务完整类名
     */
    @Column(length = 500, comment = "类名")
    private String className;

    /**
     * 任务分类（如 OneSecond、FiveMinute 等）
     */
    @Column(length = 50, comment = "分类")
    private String category;

    /**
     * 分类显示名称
     */
    @Column(length = 200, comment = "分类显示名")
    private String categoryDisplayName;

    /**
     * 服务器分配标签（逗号分隔），为空表示在所有服务器上运行
     * <p>
     * 此字段可通过管理界面修改，优先于注解默认值
     */
    @Column(length = 500, comment = "分配标签")
    private String assignTag;

    /**
     * 注解中定义的默认分配标签，仅供参考，不可通过界面修改
     */
    @Column(length = 500, comment = "注解默认标签")
    private String defaultAssignTag;

    /**
     * 任务分组
     */
    @Column(length = 200, comment = "分组")
    private String groupName;

    /**
     * 任务描述
     */
    @Column(length = 1000, comment = "描述")
    private String description;

    /**
     * 是否启用：0-禁用，1-启用
     */
    @Column(type = DataType.TINYINT, length = 1, comment = "是否启用,0:禁用,1:启用")
    private Integer enabled;

    /**
     * 最后更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Column(type = DataType.DATETIME, comment = "更新时间")
    private Date updateTime;
}
