package cn.org.autumn.modules.qrc.entity;

import cn.org.autumn.entity.UuidBased;
import cn.org.autumn.table.annotation.Column;
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
@TableName("qrc_client_grant")
@Table(comment = "扫码授权:OAuth客户端QRC策略")
public class ClientGrantEntity implements UuidBased, Serializable {
    private static final long serialVersionUID = 1L;

    @TableId
    @Column(isKey = true, type = DataType.BIGINT, length = 20, isNull = false, isAutoIncrement = true, comment = "id")
    private Long id;

    @Column(length = 32, comment = "标识:行全局唯一业务主键", isUnique = true)
    private String uuid;

    @Column(length = 50, comment = "客户端:oauth client_id", isUnique = true)
    private String clientId;

    @Column(type = DataType.TINYINT, length = 1, comment = "启用:是否允许扫码授权", defaultValue = "1")
    private boolean enabled = true;

    @Column(length = 20, comment = "交付:POLL_CODE等", defaultValue = "POLL_CODE")
    private String delivery = "POLL_CODE";

    @Column(length = 500, comment = "Webhook:回调地址")
    private String webhook;

    @Column(length = 128, comment = "Webhook:签名密钥")
    private String secret;

    @Column(length = 500, comment = "DeepLink:scheme白名单CSV")
    private String schemes;

    @Column(length = 200, comment = "范围:允许的scope CSV")
    private String scopes;

    @Column(type = DataType.TINYINT, length = 1, comment = "二次确认:浏览器已登录时仍需APP确认", defaultValue = "0")
    private boolean consent;

    /**
     * 桌面快捷登录（AS Bridge + 本机 presence）。
     * <p>
     * 为 true 时，该 OAuth 客户端的扫码登录页在建票后可通过 AS 域 Bridge iframe
     * 探测本机超然信是否已登录，并展示快捷账号入口；为 false（默认）时仅展示二维码。
     * 与证书管理页的「启用生产证书下发」无关：后者只控制 APP 是否拉取生产 TLS 材料。
     */
    @Column(type = DataType.TINYINT, length = 1, comment = "快捷登录:是否允许该客户端网页经AS Bridge探测本机超然信并展示快捷账号(默认关)", defaultValue = "0")
    private boolean quick;

    @Column(type = "datetime", comment = "更新")
    private Date updated;
}
