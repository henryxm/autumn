package cn.org.autumn.modules.client.entity;

import cn.org.autumn.config.ClientType;
import com.baomidou.mybatisplus.annotations.*;
import cn.org.autumn.table.annotation.*;

import java.io.Serializable;
import java.util.Date;

/**
 * 网站客户端
 *
 * @author Shaohua Xu
 * @email henryxm@163.com
 * @date 2020-11
 */

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

    @Column(length = 200, comment = "范围")
    private String scope;

    @Column(length = 200, comment = "状态")
    private String state;

    @Column(length = 100, comment = "类型")
    private ClientType clientType;

    @Column(length = 500, comment = "描述信息")
    private String description;

    @Column(type = "datetime", comment = "创建时间")
    private Date createTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }

    public String getAuthorizeUri() {
        return authorizeUri;
    }

    public void setAuthorizeUri(String authorizeUri) {
        this.authorizeUri = authorizeUri;
    }

    public String getAccessTokenUri() {
        return accessTokenUri;
    }

    public void setAccessTokenUri(String accessTokenUri) {
        this.accessTokenUri = accessTokenUri;
    }

    public String getUserInfoUri() {
        return userInfoUri;
    }

    public void setUserInfoUri(String userInfoUri) {
        this.userInfoUri = userInfoUri;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public ClientType getClientType() {
        return clientType;
    }

    public void setClientType(ClientType clientType) {
        this.clientType = clientType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }
}
