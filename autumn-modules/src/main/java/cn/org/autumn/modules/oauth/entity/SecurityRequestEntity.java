package cn.org.autumn.modules.oauth.entity;

import cn.org.autumn.annotation.Cache;
import cn.org.autumn.table.annotation.Column;
import cn.org.autumn.table.annotation.Table;
import cn.org.autumn.table.data.DataType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;

@Getter
@Setter
@TableName("oauth_security_request")
@Table(value = "oauth_security_request", comment = "安全验证")
public class SecurityRequestEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId
    @Column(isKey = true, type = "bigint", length = 20, isNull = false, isAutoIncrement = true, comment = "id")
    private Long id;

    @Column(length = 100, comment = "特征值:浏览器代理特征值，使用AES key加密返回", isUnique = true)
    @Cache(name = "agent")
    private String agent;

    @Column(length = 100, comment = "认证值:通用认证header请求:X-Encrypt-Auth，用于鉴别并防止非法请求，特别是当用户未登录前的API请求，使用AES key加密返回", isUnique = true)
    @Cache(name = "auth")
    private String auth;

    /** 0/1 存库；Java boolean，见 {@link cn.org.autumn.config.BooleanNumericTypeHandler} */
    @Column(type = DataType.TINYINT, length = 1, comment = "开启")
    private boolean enabled;

    @Column(comment = "过期")
    private Date expire;

    @Column(comment = "创建")
    private Date create;
}
