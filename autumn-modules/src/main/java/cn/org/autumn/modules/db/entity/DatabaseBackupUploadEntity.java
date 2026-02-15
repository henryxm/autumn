package cn.org.autumn.modules.db.entity;

import cn.org.autumn.table.annotation.Column;
import cn.org.autumn.table.annotation.Table;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;

@Getter
@Setter
@TableName("db_database_backup_upload")
@Table(value = "db_database_backup_upload", comment = "备份上传")
public class DatabaseBackupUploadEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId
    @Column(isKey = true, type = "bigint", length = 20, isNull = false, isAutoIncrement = true, comment = "id")
    private Long id;

    @Column(comment = "原始文件名")
    private String originalFilename;

    @Column(comment = "存储文件名")
    private String filename;

    @Column(comment = "文件路径")
    private String filepath;

    @Column(comment = "文件大小(字节)")
    private Long filesize;

    @Column(comment = "目标数据库名称")
    @TableField("`database`")
    private String database;

    @Column(comment = "备注说明")
    private String remark;

    /**
     * 状态: 0-上传成功, 1-恢复中, 2-恢复成功, 3-恢复失败
     */
    @Column(comment = "状态:0-上传成功,1-恢复中,2-恢复成功,3-恢复失败", defaultValue = "0")
    private Integer status;

    @Column(type = "text", comment = "错误信息")
    private String error;

    @Column(comment = "恢复耗时(毫秒)")
    private Long restoreDuration;

    @Column(comment = "恢复时间")
    private Date restoreTime;

    @Column(comment = "创建时间")
    private Date createTime;
}
