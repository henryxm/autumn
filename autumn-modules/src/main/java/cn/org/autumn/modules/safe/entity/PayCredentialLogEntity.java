package cn.org.autumn.modules.safe.entity;

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
@TableName("safe_pay_credential_log")
@Table(comment = "凭证审计:支付密码操作日志")
public class PayCredentialLogEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId
    @Column(isKey = true, type = DataType.BIGINT, length = 20, isNull = false, isAutoIncrement = true, comment = "id")
    private Long id;

    @Column(length = 32, comment = "标识:业务主键", isUnique = true)
    private String uuid;

    @Column(length = 32, comment = "用户:sys_user.uuid")
    @Index
    private String userUuid;

    @Column(length = 32, comment = "动作:SET/CHANGE/RESET/VERIFY_FAIL/LOCK")
    private String action;

    @Column(length = 16, comment = "方式:PIN/GESTURE/BIO")
    private String method;

    @Column(comment = "成功:是否成功", defaultValue = "0")
    private boolean success;

    @Column(length = 64, comment = "地址:客户端IP")
    private String ip;

    @Column(length = 256, comment = "代理:User-Agent")
    private String userAgent;

    @Column(length = 256, comment = "备注:脱敏说明")
    private String remark;

    @Column(type = "datetime", comment = "时间:记录时间")
    @Index
    private Date createTime;
}
