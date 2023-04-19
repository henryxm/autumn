package cn.org.autumn.modules.oauth.entity;

import cn.org.autumn.config.ClientType;
import cn.org.autumn.table.data.DataType;
import com.baomidou.mybatisplus.annotations.*;
import cn.org.autumn.table.annotation.*;
import org.apache.commons.lang.StringUtils;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 客户端详情
 *
 * @author Shaohua Xu
 * @email henryxm@163.com
 * @date 2020-11
 */

@TableName("oauth_client_details")
@Table(value = "oauth_client_details", comment = "客户端详情")
public class ClientDetailsEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId
    @Column(isKey = true, type = "bigint", length = 20, isNull = false, isAutoIncrement = true, comment = "id")
    private Long id;

    @Column(length = 50, comment = "UUID")
    private String uuid;

    @Column(type = DataType.TEXT, comment = "资源ID")
    private String resourceIds;

    @Column(length = 200, comment = "范围")
    private String scope;

    @Column(length = 200, comment = "授权类型")
    private String grantTypes;

    @Column(length = 200, comment = "角色")
    private String roles;

    @Column(type = "int", length = 11, comment = "是否可信")
    private Integer trusted;

    @Column(type = "int", length = 11, comment = "是否归档")
    private Integer archived;

    @Column(length = 200, comment = "客户端ID", isUnique = true)
    private String clientId;

    @Column(length = 200, comment = "客户端密匙")
    private String clientSecret;

    @Column(length = 200, comment = "客户端名字")
    private String clientName;

    @Column(length = 500, comment = "客户端URI")
    private String clientUri;

    @Column(length = 500, comment = "客户端图标URI")
    private String clientIconUri;

    @Column(length = 500, comment = "重定向地址")
    private String redirectUri;

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

    public String getResourceIds() {
        return resourceIds;
    }

    public void setResourceIds(String resourceIds) {
        this.resourceIds = resourceIds;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getGrantTypes() {
        return grantTypes;
    }

    public void setGrantTypes(String grantTypes) {
        this.grantTypes = grantTypes;
    }

    public String getRoles() {
        return roles;
    }

    public void setRoles(String roles) {
        this.roles = roles;
    }

    public Integer getTrusted() {
        return trusted;
    }

    public void setTrusted(Integer trusted) {
        this.trusted = trusted;
    }

    public Integer getArchived() {
        return archived;
    }

    public void setArchived(Integer archived) {
        this.archived = archived;
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

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public String getClientUri() {
        return clientUri;
    }

    public void setClientUri(String clientUri) {
        this.clientUri = clientUri;
    }

    public String getClientIconUri() {
        return clientIconUri;
    }

    public void setClientIconUri(String clientIconUri) {
        this.clientIconUri = clientIconUri;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
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

    public List<String> parse(String source) {
        List<String> list = new ArrayList<>();
        if (null == source)
            return list;
        String[] ll = source.split("[;,；， ]");
        for (String l : ll) {
            if (StringUtils.isNotBlank(l))
                list.add(l.toLowerCase());
        }
        return list;
    }

    public List<String> grants() {
        return parse(grantTypes);
    }

    public List<String> resources() {
        return parse(resourceIds);
    }

    public List<String> roles() {
        return parse(roles);
    }

    public List<String> scopes() {
        return parse(scope);
    }

    public boolean granted(String grant) {
        if (null == grantTypes)
            return false;
        if (grantTypes.toLowerCase().contains("all"))
            return true;
        return grants().contains(grant);
    }

    public boolean resourced(String resource) {
        if (null == resourceIds)
            return false;
        if (resourceIds.toLowerCase().contains("all"))
            return true;
        return resources().contains(resource);
    }

    public boolean roled(String role) {
        if (null == roles)
            return false;
        if (roles.toLowerCase().contains("all"))
            return true;
        return roles().contains(role);
    }

    public boolean scoped(String scope) {
        if (null == scope)
            return false;
        if (scope.toLowerCase().contains("all"))
            return true;
        return scopes().contains(scope);
    }
}
