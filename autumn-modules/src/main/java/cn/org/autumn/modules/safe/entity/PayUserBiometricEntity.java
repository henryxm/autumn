package cn.org.autumn.modules.safe.entity;

import static com.fasterxml.jackson.annotation.JsonProperty.Access.WRITE_ONLY;

import cn.org.autumn.table.annotation.Column;
import cn.org.autumn.table.annotation.Index;
import cn.org.autumn.table.annotation.IndexField;
import cn.org.autumn.table.annotation.IndexMethodEnum;
import cn.org.autumn.table.annotation.IndexTypeEnum;
import cn.org.autumn.table.annotation.Indexes;
import cn.org.autumn.table.annotation.Table;
import cn.org.autumn.table.data.DataType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.util.Date;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("safe_pay_user_biometric")
@Table(comment = "生物识别:用户设备公钥绑定")
@Indexes(@Index(name = "user_device", indexType = IndexTypeEnum.UNIQUE, indexMethod = IndexMethodEnum.BTREE, fields = {
        @IndexField(field = "userUuid", length = 32),
        @IndexField(field = "deviceId", length = 64)
}))
public class PayUserBiometricEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final int STATUS_ACTIVE = 1;
    public static final int STATUS_REVOKED = 0;

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

    @Column(length = 16, comment = "平台:ios/android/harmony")
    private String platform;

    @Column(length = 64, comment = "凭证:客户端密钥标识")
    private String credentialId;

    @JsonProperty(access = WRITE_ONLY)
    @Column(type = DataType.TEXT, comment = "公钥:Base64编码X509")
    private String publicKey;

    @Column(comment = "状态:1有效;0吊销", defaultValue = "1")
    private int status = STATUS_ACTIVE;

    @Column(type = "datetime", comment = "活跃:最近验签时间")
    private Date lastUsedTime;

    @Column(type = "datetime", comment = "注册:创建时间")
    private Date createTime;

    @Column(type = "datetime", comment = "更新:更新时间")
    private Date updateTime;
}
