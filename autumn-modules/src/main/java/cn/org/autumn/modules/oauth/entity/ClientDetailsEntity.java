package cn.org.autumn.modules.oauth.entity;

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

    /**
     * id
     */
    @TableId
    @Column(isKey = true, type = "bigint", length = 20, isNull = false, isAutoIncrement = true, comment = "id")
    private Long id;
    /**
     * 资源ID
     */
    @Column(type = DataType.TEXT, comment = "资源ID")
    private String resourceIds;
    /**
     * 范围
     */
    @Column(length = 200, comment = "范围")
    private String scope;
    /**
     * 授权类型
     */
    @Column(length = 200, comment = "授权类型")
    private String grantTypes;
    /**
     * 角色
     */
    @Column(length = 200, comment = "角色")
    private String roles;
    /**
     * 是否可信
     */
    @Column(type = "int", length = 11, comment = "是否可信")
    private Integer trusted;
    /**
     * 是否归档
     */
    @Column(type = "int", length = 11, comment = "是否归档")
    private Integer archived;
    /**
     * 客户端ID
     */
    @Column(length = 200, comment = "客户端ID", isUnique = true)
    private String clientId;
    /**
     * 客户端密匙
     */
    @Column(length = 200, comment = "客户端密匙")
    private String clientSecret;
    /**
     * 客户端名字
     */
    @Column(length = 200, comment = "客户端名字")
    private String clientName;
    /**
     * 客户端URI
     */
    @Column(length = 500, comment = "客户端URI")
    private String clientUri;
    /**
     * 客户端图标URI
     */
    @Column(length = 500, comment = "客户端图标URI")
    private String clientIconUri;
    /**
     * 重定向地址
     */
    @Column(length = 500, comment = "重定向地址")
    private String redirectUri;
    /**
     * 描述信息
     */
    @Column(length = 500, comment = "描述信息")
    private String description;
    /**
     * 创建时间
     */
    @Column(type = "datetime", comment = "创建时间")
    private Date createTime;
    /**
     * 设置：id
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * 获取：id
     */
    public Long getId() {
        return id;
    }

    /**
     * 设置：资源ID
     */
    public void setResourceIds(String resourceIds) {
        this.resourceIds = resourceIds;
    }

    /**
     * 获取：资源ID
     */
    public String getResourceIds() {
        return resourceIds;
    }

    /**
     * 设置：范围
     */
    public void setScope(String scope) {
        this.scope = scope;
    }

    /**
     * 获取：范围
     */
    public String getScope() {
        return scope;
    }

    /**
     * 设置：授权类型
     */
    public void setGrantTypes(String grantTypes) {
        this.grantTypes = grantTypes;
    }

    /**
     * 获取：授权类型
     */
    public String getGrantTypes() {
        return grantTypes;
    }

    /**
     * 设置：角色
     */
    public void setRoles(String roles) {
        this.roles = roles;
    }

    /**
     * 获取：角色
     */
    public String getRoles() {
        return roles;
    }

    /**
     * 设置：是否可信
     */
    public void setTrusted(Integer trusted) {
        this.trusted = trusted;
    }

    /**
     * 获取：是否可信
     */
    public Integer getTrusted() {
        return trusted;
    }

    /**
     * 设置：是否归档
     */
    public void setArchived(Integer archived) {
        this.archived = archived;
    }

    /**
     * 获取：是否归档
     */
    public Integer getArchived() {
        return archived;
    }

    /**
     * 设置：创建时间
     */
    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    /**
     * 获取：创建时间
     */
    public Date getCreateTime() {
        return createTime;
    }

    /**
     * 设置：客户端ID
     */
    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    /**
     * 获取：客户端ID
     */
    public String getClientId() {
        return clientId;
    }

    /**
     * 设置：客户端密匙
     */
    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    /**
     * 获取：客户端密匙
     */
    public String getClientSecret() {
        return clientSecret;
    }

    /**
     * 设置：客户端名字
     */
    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    /**
     * 获取：客户端名字
     */
    public String getClientName() {
        return clientName;
    }

    /**
     * 设置：客户端URI
     */
    public void setClientUri(String clientUri) {
        this.clientUri = clientUri;
    }

    /**
     * 获取：客户端URI
     */
    public String getClientUri() {
        return clientUri;
    }

    /**
     * 设置：客户端图标URI
     */
    public void setClientIconUri(String clientIconUri) {
        this.clientIconUri = clientIconUri;
    }

    /**
     * 获取：客户端图标URI
     */
    public String getClientIconUri() {
        return clientIconUri;
    }

    /**
     * 设置：重定向地址
     */
    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }

    /**
     * 获取：重定向地址
     */
    public String getRedirectUri() {
        return redirectUri;
    }

    /**
     * 设置：描述信息
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * 获取：描述信息
     */
    public String getDescription() {
        return description;
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
