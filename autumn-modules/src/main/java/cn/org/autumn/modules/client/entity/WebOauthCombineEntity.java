package cn.org.autumn.modules.client.entity;

import cn.org.autumn.table.annotation.Column;
import cn.org.autumn.table.annotation.Table;
import com.baomidou.mybatisplus.annotations.TableId;
import com.baomidou.mybatisplus.annotations.TableName;

import java.io.Serializable;
import java.util.Date;

@TableName("client_web_oauth_combine")
@Table(value = "client_web_oauth_combine", comment = "授权登录合并")
public class WebOauthCombineEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId
    @Column(isKey = true, type = "bigint", length = 20, isNull = false, isAutoIncrement = true, comment = "id")
    private Long id;

    @Column(length = 50, comment = "UUID", isUnique = true)
    private String uuid;

    @Column(length = 50, comment = "Client ID", isUnique = true)
    private String clientId;

    @Column(length = 50, comment = "Web Authentication UUID")
    private String webAuthenticationUuid;

    @Column(length = 50, comment = "Client Details UUID")
    private String clientDetailsUuid;

    @Column(type = "datetime", comment = "创建时间")
    private Date createTime;

    @Column(type = "datetime", comment = "更新时间")
    private Date updateTime;

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

    public String getWebAuthenticationUuid() {
        return webAuthenticationUuid;
    }

    public void setWebAuthenticationUuid(String webAuthenticationUuid) {
        this.webAuthenticationUuid = webAuthenticationUuid;
    }

    public String getClientDetailsUuid() {
        return clientDetailsUuid;
    }

    public void setClientDetailsUuid(String clientDetailsUuid) {
        this.clientDetailsUuid = clientDetailsUuid;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }
}