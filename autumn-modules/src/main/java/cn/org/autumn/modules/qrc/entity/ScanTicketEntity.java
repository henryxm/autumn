package cn.org.autumn.modules.qrc.entity;

import cn.org.autumn.entity.UuidBased;
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

@Getter
@Setter
@TableName("qrc_scan_ticket")
@Table(comment = "扫码票据:审计与风控")
public class ScanTicketEntity implements UuidBased, Serializable {
    private static final long serialVersionUID = 1L;

    @TableId
    @Column(isKey = true, type = DataType.BIGINT, length = 20, isNull = false, isAutoIncrement = true, comment = "id")
    private Long id;

    @Column(length = 32, comment = "票据:全局唯一业务主键", isUnique = true)
    private String uuid;

    @Column(length = 32, comment = "意图:SELF_WEB_LOGIN等")
    @Index
    private String intent;

    @Column(length = 16, comment = "状态:PENDING等")
    @Index
    private String status;

    @Column(length = 50, comment = "客户端:oauth client_id")
    @Index
    private String clientId;

    @Column(length = 32, comment = "扫码用户:sys_user.uuid")
    @Index
    private String scanner;

    @Column(length = 32, comment = "主体用户:授权目标用户uuid")
    @Index
    private String subject;

    @Column(length = 40, comment = "IP地址")
    private String ip;

    @Column(length = 500, comment = "代理")
    private String agent;

    @Column(type = DataType.TEXT, comment = "载荷:OAuth上下文等JSON")
    private String payload;

    @Column(type = DataType.TEXT, comment = "结果:code/token等JSON")
    private String result;

    @Column(type = "datetime", comment = "创建")
    private Date created;

    @Column(type = "datetime", comment = "过期")
    private Date expired;

    @Column(type = "datetime", comment = "完成")
    private Date completed;
}
