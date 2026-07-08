package cn.org.autumn.modules.opc.entity;

import cn.org.autumn.annotation.Cache;
import cn.org.autumn.annotation.FieldEncrypt;
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
@TableName("opc_connect_app")
@Table(value = "opc_connect_app", comment = "接入应用:第三方开放平台连接配置")
public class ConnectAppEntity implements UuidBased, Serializable {
    private static final long serialVersionUID = 1L;

    public static final int STATUS_ACTIVE = 1;
    public static final int STATUS_DISABLED = 0;

    @TableId
    @Column(isKey = true, type = DataType.BIGINT, length = 20, isNull = false, isAutoIncrement = true, comment = "id")
    private Long id;

    @Cache
    @Column(length = 32, comment = "标识:业务主键", isUnique = true)
    private String uuid;

    @Column(length = 32, comment = "用户:配置所属人sys_user.uuid")
    @Index
    private String user;

    @Cache(name = "appId", unique = true)
    @Column(length = 32, comment = "应用:opl平台appId", isUnique = true)
    private String appId;

    @FieldEncrypt
    @Column(length = 512, comment = "密钥:appSecret加密存储")
    private String appSecret;

    @Column(length = 500, comment = "平台:opl根地址")
    private String platformBaseUrl;

    @Column(length = 500, comment = "回调:本地redirectUri")
    private String redirectUri;

    @Column(length = 500, comment = "授权:authorizeUri")
    private String authorizeUri;

    @Column(length = 500, comment = "令牌:tokenUri")
    private String tokenUri;

    @Column(length = 500, comment = "用户:userInfoUri")
    private String userInfoUri;

    @Column(length = 100, comment = "名称:应用名称")
    private String name;

    @Column(length = 200, comment = "范围:授权范围", defaultValue = "basic")
    private String scope;

    @Column(comment = "状态:1正常;0禁用", defaultValue = "1")
    private int status = STATUS_ACTIVE;

    @Column(length = 500, comment = "图标:登录页图标地址")
    private String icon;

    @Column(comment = "HASH:图标文件HASH")
    @Index
    private String hash;

    @Column(comment = "登录页展示:1展示;0隐式", defaultValue = "0")
    private int pageLogin;

    @Column(type = DataType.DATETIME, comment = "创建:创建时间")
    private Date create;

    @Column(type = DataType.DATETIME, comment = "更新:更新时间")
    private Date update;
}
