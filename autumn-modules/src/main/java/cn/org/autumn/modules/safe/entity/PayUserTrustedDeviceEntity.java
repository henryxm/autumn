package cn.org.autumn.modules.safe.entity;

import cn.org.autumn.table.annotation.Column;
import cn.org.autumn.table.annotation.Index;
import cn.org.autumn.table.annotation.IndexField;
import cn.org.autumn.table.annotation.IndexMethodEnum;
import cn.org.autumn.table.annotation.IndexTypeEnum;
import cn.org.autumn.table.annotation.Indexes;
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
@TableName("safe_pay_user_trusted_device")
@Table(comment = "支付安全:用户常用支付设备")
@Indexes(@Index(name = "user_device", indexType = IndexTypeEnum.UNIQUE, indexMethod = IndexMethodEnum.BTREE, fields = {
        @IndexField(field = "userUuid", length = 32),
        @IndexField(field = "deviceId", length = 64)
}))
public class PayUserTrustedDeviceEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId
    @Column(isKey = true, type = DataType.BIGINT, length = 20, isNull = false, isAutoIncrement = true, comment = "id")
    private Long id;

    @Column(length = 32, comment = "标识:业务主键", isUnique = true)
    private String uuid;

    @Column(length = 32, comment = "用户:sys_user.uuid")
    @Index
    private String userUuid;

    @Column(length = 64, comment = "设备:客户端稳定设备ID")
    private String deviceId;

    @Column(length = 64, comment = "名称:设备展示名")
    private String deviceName;

    @Column(length = 16, comment = "平台:ios/android等")
    private String platform;

    @Column(comment = "成功:累计成功支付次数", defaultValue = "0")
    private int successCount;

    @Column(type = "datetime", comment = "信任:加入常用时间")
    private Date trustTime;

    @Column(type = "datetime", comment = "活跃:最近使用时间")
    private Date lastUsedTime;
}
