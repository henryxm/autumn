package cn.org.autumn.modules.opl.entity;

import cn.org.autumn.annotation.Cache;
import cn.org.autumn.entity.UuidBased;
import cn.org.autumn.opl.model.OpenAppType;
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
@TableName("opl_open_app")
@Table(value = "opl_open_app", comment = "开放应用:第三方App注册")
public class OpenAppEntity implements UuidBased, Serializable {
    private static final long serialVersionUID = 1L;

    public static final int STATUS_ACTIVE = 1;
    public static final int STATUS_DISABLED = 0;

    @TableId
    @Column(isKey = true, type = DataType.BIGINT, length = 20, isNull = false, isAutoIncrement = true, comment = "id")
    private Long id;

    @Cache
    @Column(length = 32, comment = "标识:业务主键", isUnique = true)
    private String uuid;

    @Column(length = 32, comment = "主体:开发者账号uuid")
    @Index
    private String account;

    @Cache(name = "appId")
    @Column(length = 32, comment = "应用:开放平台appId", isUnique = true)
    private String appId;

    @Column(length = 128, comment = "密钥:appSecret哈希")
    private String appSecretHash;

    @Column(length = 32, comment = "盐值:密钥哈希盐")
    private String appSecretSalt;

    @Column(length = 100, comment = "名称:应用名称")
    private String name;

    @Column(length = 32, comment = "类型:应用类型", defaultValue = "Web")
    @Index
    private OpenAppType appType = OpenAppType.Web;

    @Column(length = 500, comment = "回调:授权回调地址")
    private String redirectUri;

    @Column(length = 200, comment = "范围:授权范围", defaultValue = "basic")
    private String scope;

    @Column(comment = "状态:1正常;0禁用", defaultValue = "1")
    private int status = STATUS_ACTIVE;

    @Column(type = DataType.DATETIME, comment = "创建:创建时间")
    private Date create;

    @Column(type = DataType.DATETIME, comment = "更新:更新时间")
    private Date update;
}
