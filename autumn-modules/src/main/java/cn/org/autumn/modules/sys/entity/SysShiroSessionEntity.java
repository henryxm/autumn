package cn.org.autumn.modules.sys.entity;

import cn.org.autumn.table.annotation.Column;
import cn.org.autumn.table.annotation.Index;
import cn.org.autumn.table.annotation.Table;
import cn.org.autumn.table.data.DataType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;
import lombok.Getter;
import lombok.Setter;

/**
 * Shiro Web Session 数据库持久化（DB 权威，Redis/内存为一级缓存）。
 */
@Getter
@Setter
@TableName("sys_shiro_session")
@Table(comment = "Shiro会话")
public class SysShiroSessionEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId
    @Column(isKey = true, type = DataType.BIGINT, length = 20, isNull = false, isAutoIncrement = true, comment = "id")
    private Long id;

    @Column(length = 128, isNull = false, isUnique = true, comment = "会话:Shiro sessionId")
    private String sessionId;

    @Column(length = 32, comment = "用户:sys_user.uuid")
    @Index
    private String user;

    /** Base64 编码的 JDK 序列化 Session。 */
    @Column(type = DataType.MEDIUMTEXT, comment = "载荷:序列化Session")
    private String payload;

    @Column(type = DataType.DATETIME, comment = "过期时间")
    @Index
    private Date expireTime;

    @Column(type = DataType.DATETIME, comment = "最后访问")
    private Date lastAccessTime;

    @Column(type = DataType.DATETIME, comment = "更新时间")
    private Date updateTime;
}
