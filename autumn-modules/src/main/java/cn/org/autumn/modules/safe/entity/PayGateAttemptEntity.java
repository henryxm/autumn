package cn.org.autumn.modules.safe.entity;

import cn.org.autumn.table.annotation.Column;
import cn.org.autumn.table.annotation.Index;
import cn.org.autumn.table.annotation.Table;
import cn.org.autumn.table.data.DataType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;

@Getter
@Setter
@TableName("safe_pay_gate_attempt")
@Table(comment = "支付闸门:支付安全评估记录")
public class PayGateAttemptEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String AUTH_DENIED = "DENIED";
    public static final String AUTH_PASSWORD_REQUIRED = "PASSWORD_REQUIRED";
    public static final String AUTH_PASSWORDLESS = "PASSWORDLESS";

    @TableId
    @Column(isKey = true, type = DataType.BIGINT, length = 20, isNull = false, isAutoIncrement = true, comment = "id")
    private Long id;

    @Column(length = 32, comment = "标识:业务主键", isUnique = true)
    private String uuid;

    @Column(length = 32, comment = "用户:sys_user.uuid")
    @Index
    private String userUuid;

    @Column(comment = "金额:支付金额分", defaultValue = "0")
    private long amountCent;

    @Column(length = 8, comment = "币种:如CNY")
    private String currency;

    @Column(length = 64, comment = "订单:业务订单号")
    private String orderId;

    @Column(length = 64, comment = "商户:商户标识")
    private String merchantId;

    @Column(length = 256, comment = "理由:支付说明")
    private String reason;

    @Column(length = 64, comment = "设备:设备ID")
    private String deviceId;

    @Column(length = 64, comment = "地址:评估时IP")
    private String ip;

    @Column(length = 128, comment = "地点:客户端上报地点")
    private String location;

    @Column(length = 16, comment = "模式:评估结果授权模式")
    private String authMode;

    @Column(comment = "通过:是否允许进入下一步", defaultValue = "0")
    private boolean authorized;

    @Column(type = DataType.TEXT, comment = "原因:拒绝或提示JSON")
    private String detailJson;

    @Column(type = "datetime", comment = "时间:评估时间")
    @Index
    private Date createTime;
}
