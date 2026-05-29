package cn.org.autumn.modules.safe.entity;

import cn.org.autumn.annotation.Cache;
import cn.org.autumn.table.annotation.Column;
import cn.org.autumn.table.annotation.Table;
import cn.org.autumn.table.data.DataType;
import com.baomidou.mybatisplus.annotations.TableId;
import com.baomidou.mybatisplus.annotations.TableName;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;

@Getter
@Setter
@TableName("safe_pay_user_security_setting")
@Table(comment = "支付安全:用户支付安全偏好")
public class PayUserSecuritySettingEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId
    @Column(isKey = true, type = DataType.BIGINT, length = 20, isNull = false, isAutoIncrement = true, comment = "id")
    private Long id;

    @Column(length = 32, comment = "标识:业务主键", isUnique = true)
    private String uuid;

    @Cache(name = "userUuid", unique = true)
    @Column(length = 32, comment = "用户:sys_user.uuid", isUnique = true)
    private String userUuid;

    @Column(comment = "免密:用户是否开启小额免密", defaultValue = "1")
    private boolean passwordlessEnabled = true;

    @Column(comment = "免密额:用户免密上限分,0用全局", defaultValue = "0")
    private long passwordlessMaxAmountCent;

    @Column(comment = "免密窗:用户免密窗口分钟,0用全局", defaultValue = "0")
    private int passwordlessWindowMinutes;

    @Column(comment = "手势付:是否允许手势作为支付校验", defaultValue = "0")
    private boolean gesturePaymentEnabled;

    @Column(type = "datetime", comment = "更新:最近修改时间")
    private Date updateTime;
}
