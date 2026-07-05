package cn.org.autumn.modules.opl.entity;

import cn.org.autumn.annotation.Cache;
import cn.org.autumn.entity.UuidBased;
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

/**
 * App 内用户 openId（App 维度唯一）。
 * <p>
 * unionId 见 {@link OpenUnionEntity}（开发者账号维度）。
 */
@Getter
@Setter
@TableName("opl_open_identity")
@Table(value = "opl_open_identity", comment = "开放身份:App内openId")
public class OpenIdentityEntity implements UuidBased, Serializable {
    private static final long serialVersionUID = 1L;

    @TableId
    @Column(isKey = true, type = DataType.BIGINT, length = 20, isNull = false, isAutoIncrement = true, comment = "id")
    private Long id;

    @Cache
    @Column(length = 32, comment = "标识:业务主键", isUnique = true)
    private String uuid;

    @Column(length = 32, comment = "应用:appId")
    @Index
    private String appId;

    @Column(length = 32, comment = "用户:终端用户sys_user.uuid")
    @Index
    private String user;

    @Cache(name = "openId")
    @Column(length = 64, comment = "开放:App内用户openId", isUnique = true)
    private String openId;

    @Column(type = DataType.DATETIME, comment = "创建:创建时间")
    private Date create;

    @Column(type = DataType.DATETIME, comment = "更新:更新时间")
    private Date update;
}
