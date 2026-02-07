package cn.org.autumn.modules.db.entity;

import cn.org.autumn.table.annotation.Column;
import cn.org.autumn.table.annotation.Table;
import com.baomidou.mybatisplus.annotations.TableId;
import com.baomidou.mybatisplus.annotations.TableName;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;

@Getter
@Setter
@TableName("db_database_backup_strategy")
@Table(value = "db_database_backup_strategy", comment = "备份策略")
public class DatabaseBackupStrategyEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId
    @Column(isKey = true, type = "bigint", length = 20, isNull = false, isAutoIncrement = true, comment = "id")
    private Long id;

    @Column(comment = "策略名称")
    private String name;

    @Column(comment = "是否启用", defaultValue = "1")
    private Boolean enable;

    /**
     * 备份模式: FULL-全量, TABLES-指定表
     */
    @Column(comment = "备份模式:FULL-全量,TABLES-指定表", defaultValue = "FULL")
    private String mode;

    /**
     * 指定备份的表名列表，逗号分隔，mode=TABLES时有效
     */
    @Column(type = "text", comment = "备份表列表(逗号分隔)")
    private String tables;

    /**
     * 调度周期: ONCE-单次, HOURLY-每小时, DAILY-每天, WEEKLY-每周
     */
    @Column(comment = "调度周期:ONCE-单次,HOURLY-每小时,DAILY-每天,WEEKLY-每周", defaultValue = "DAILY")
    private String schedule;

    /**
     * 是否启用滚动备份
     * 启用后，当备份数量超过 maxKeep 时，自动删除最旧的非永久备份
     */
    @Column(comment = "启用滚动备份", defaultValue = "0")
    private Boolean rollingEnabled;

    /**
     * 滚动保留数量，配合 rollingEnabled 使用
     * 0表示不限制。超出后自动删除最旧的非永久备份
     */
    @Column(comment = "滚动保留数量(0不限制)", defaultValue = "0")
    private Integer maxKeep;

    @Column(comment = "备注")
    private String remark;

    @Column(comment = "上次执行时间")
    private Date lastRunTime;

    @Column(comment = "创建时间")
    private Date createTime;
}
