package cn.org.autumn.modules.client.entity;

import cn.org.autumn.annotation.Cache;
import cn.org.autumn.config.ClientType;
import cn.org.autumn.table.annotation.*;
import com.baomidou.mybatisplus.annotations.*;
import java.io.Serializable;
import java.util.Date;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("client_web_authentication")
@Table(value = "client_web_authentication", comment = "网站客户端")
public class WebAuthenticationEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId
    @Column(isKey = true, type = "bigint", length = 20, isNull = false, isAutoIncrement = true, comment = "id")
    private Long id;

    @Column(length = 50, comment = "UUID")
    private String uuid;

    @Column(length = 200, comment = "客户端名字")
    private String name;

    @Cache
    @Column(length = 200, comment = "客户端ID", isUnique = true)
    private String clientId;

    @Column(length = 200, comment = "客户端密匙")
    private String clientSecret;

    @Column(length = 500, comment = "重定向地址")
    private String redirectUri;

    @Column(length = 200, comment = "授权码地址")
    private String authorizeUri;

    @Column(length = 200, comment = "Token地址")
    private String accessTokenUri;

    @Column(length = 200, comment = "用户信息地址")
    private String userInfoUri;

    @Column(length = 16, comment = "userInfo传参:legacy/bearer,空则自动")
    private String userInfoDelivery;

    @Column(length = 200, comment = "范围")
    private String scope;

    @Column(length = 200, comment = "状态")
    private String state;

    @Column(length = 100, comment = "类型")
    private ClientType clientType;

    @Column(length = 500, comment = "描述信息")
    private String description;

    @Column(length = 500, comment = "远程身份源根地址（如 https://a.com）；同实例 AS+RP 留空，由 authorizeUri 等本地 URI 驱动")
    private String originUri;

    @Column(length = 500, comment = "图标:登录页图标地址")
    private String icon;

    @Column(comment = "HASH:图标文件HASH")
    @Index
    private String hash;

    @Column(comment = "登录页展示:1展示;0隐式", defaultValue = "0")
    private int pageLogin;

    @Column(type = "datetime", comment = "创建时间")
    private Date createTime;
}
