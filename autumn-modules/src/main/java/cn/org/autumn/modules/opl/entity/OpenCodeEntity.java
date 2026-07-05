package cn.org.autumn.modules.opl.entity;

import cn.org.autumn.table.annotation.Column;
import cn.org.autumn.table.annotation.Index;
import cn.org.autumn.table.annotation.Table;
import cn.org.autumn.table.data.DataType;
import com.baomidou.mybatisplus.annotations.TableId;
import com.baomidou.mybatisplus.annotations.TableName;
import java.io.Serializable;
import java.util.Date;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("opl_open_code")
@Table(value = "opl_open_code", comment = "授权码:OAuth授权码")
public class OpenCodeEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId
    @Column(isKey = true, type = DataType.BIGINT, length = 20, isNull = false, isAutoIncrement = true, comment = "id")
    private Long id;

    @Column(length = 64, comment = "授权:授权码", isUnique = true)
    private String code;

    @Column(length = 32, comment = "应用:appId")
    @Index
    private String appId;

    @Column(length = 32, comment = "用户:终端用户uuid")
    private String user;

    @Column(length = 500, comment = "回调:redirectUri")
    private String redirectUri;

    @Column(type = DataType.DATETIME, comment = "过期:过期时间")
    private Date expire;

    @Column(type = DataType.DATETIME, comment = "创建:创建时间")
    private Date create;
}
