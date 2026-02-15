package cn.org.autumn.modules.db.entity;

import cn.org.autumn.table.annotation.*;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;

@Getter
@Setter
@TableName("db_database_backup")
@Table(value = "db_database_backup", comment = "数据备份")
public class DatabaseBackupEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId
    @Column(isKey = true, type = "bigint", length = 20, isNull = false, isAutoIncrement = true, comment = "id")
    private Long id;

    @Column(comment = "关联策略ID")
    private Long strategyId;

    @Column(comment = "文件名")
    private String filename;

    @Column(comment = "文件路径")
    private String filepath;

    @Column(comment = "文件大小(字节)")
    private Long filesize;

    @Column(comment = "数据库名称")
    @TableField("`database`")
    private String database;

    /**
     * 备份模式: FULL-全量, TABLES-指定表
     */
    @Column(comment = "备份模式:FULL-全量,TABLES-指定表", defaultValue = "FULL")
    @TableField("`mode`")
    private String mode;

    /**
     * 实际备份的表列表(逗号分隔)
     */
    @Column(type = "text", comment = "备份表列表(逗号分隔)")
    private String backupTables;

    @Column(comment = "备份表数量")
    private Integer tables;

    @Column(comment = "备份记录数")
    private Integer records;

    @Column(comment = "备注说明")
    private String remark;

    /**
     * 是否永久存储
     * 标记为永久存储的备份不会被滚动备份策略自动删除
     */
    @Column(comment = "永久存储:0-否,1-是", defaultValue = "0")
    private Boolean permanent;

    /**
     * 状态: 0-等待中, 1-成功, 2-失败, 3-进行中, 4-已暂停, 5-已停止
     */
    @Column(comment = "状态:0-等待中,1-成功,2-失败,3-进行中,4-已暂停,5-已停止")
    private Integer status;

    @Column(type = "text", comment = "错误信息")
    private String error;

    @Column(comment = "耗时(毫秒)")
    private Long duration;

    /**
     * 总表数(用于进度计算)
     */
    @Column(comment = "总表数")
    private Integer totalTables;

    /**
     * 已完成表数(用于进度计算)
     */
    @Column(comment = "已完成表数")
    private Integer completedTables;

    /**
     * 当前正在备份的表名
     */
    @Column(comment = "当前备份表名")
    private String currentTable;

    /**
     * 进度百分比 0-100
     */
    @Column(comment = "进度百分比")
    private Integer progress;

    @Column(comment = "创建时间")
    private Date createTime;
}
