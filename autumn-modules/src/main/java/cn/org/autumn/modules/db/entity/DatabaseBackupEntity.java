package cn.org.autumn.modules.db.entity;

import cn.org.autumn.table.annotation.*;
import com.baomidou.mybatisplus.annotations.TableId;
import com.baomidou.mybatisplus.annotations.TableName;
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

    @Column(comment = "文件名")
    private String filename;

    @Column(comment = "文件路径")
    private String filepath;

    @Column(comment = "文件大小(字节)")
    private Long filesize;

    @Column(comment = "数据库名称")
    private String database;

    @Column(comment = "备份表数量")
    private Integer tables;

    @Column(comment = "备份记录数")
    private Integer records;

    @Column(comment = "备注说明")
    private String remark;

    @Column(comment = "状态:0-进行中,1-成功,2-失败")
    private Integer status;

    @Column(type = "text", comment = "错误信息")
    private String error;

    @Column(comment = "耗时(毫秒)")
    private Long duration;

    @Column(comment = "创建时间")
    private Date createTime;
}
