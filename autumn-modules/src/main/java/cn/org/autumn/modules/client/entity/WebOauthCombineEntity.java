package cn.org.autumn.modules.client.entity;

import cn.org.autumn.annotation.Cache;
import cn.org.autumn.table.annotation.Column;
import cn.org.autumn.table.annotation.Table;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;

@Getter
@Setter
@TableName("client_web_oauth_combine")
@Table(value = "client_web_oauth_combine", comment = "授权登录合并")
public class WebOauthCombineEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId
    @Column(isKey = true, type = "bigint", length = 20, isNull = false, isAutoIncrement = true, comment = "id")
    private Long id;

    @Cache
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
}