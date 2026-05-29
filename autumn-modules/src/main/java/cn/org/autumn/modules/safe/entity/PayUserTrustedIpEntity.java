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
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;

@Getter
@Setter
@TableName("safe_pay_user_trusted_ip")
@Table(comment = "支付安全:用户常用支付IP")
@Indexes(@Index(name = "user_ip", indexType = IndexTypeEnum.UNIQUE, indexMethod = IndexMethodEnum.BTREE, fields = {
        @IndexField(field = "userUuid", length = 32),
        @IndexField(field = "ip", length = 64)
}))
public class PayUserTrustedIpEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId
    @Column(isKey = true, type = DataType.BIGINT, length = 20, isNull = false, isAutoIncrement = true, comment = "id")
    private Long id;

    @Column(length = 32, comment = "标识:业务主键", isUnique = true)
    private String uuid;

    @Column(length = 32, comment = "用户:sys_user.uuid")
    @Index
    private String userUuid;

    @Column(length = 64, comment = "地址:客户端IP")
    private String ip;

    @Column(length = 128, comment = "地点:IP归属或客户端上报地点")
    private String locationLabel;

    @Column(type = "datetime", comment = "信任:加入常用时间")
    private Date trustTime;

    @Column(type = "datetime", comment = "活跃:最近使用时间")
    private Date lastUsedTime;
}
