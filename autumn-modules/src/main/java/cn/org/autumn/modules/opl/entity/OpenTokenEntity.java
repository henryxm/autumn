package cn.org.autumn.modules.opl.entity;

import cn.org.autumn.table.annotation.Column;
import cn.org.autumn.table.annotation.Index;
import cn.org.autumn.table.annotation.Table;
import cn.org.autumn.table.data.DataType;
import com.baomidou.mybatisplus.annotations.TableId;
import com.baomidou.mybatisplus.annotations.TableName;
import java.io.Serializable;
import java.util.Date;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("opl_open_token")
@Table(value = "opl_open_token", comment = "令牌:access与refresh token")
public class OpenTokenEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId
    @Column(isKey = true, type = DataType.BIGINT, length = 20, isNull = false, isAutoIncrement = true, comment = "id")
    private Long id;

    @Column(length = 64, comment = "访问:accessToken", isUnique = true)
    private String accessToken;

    @Column(length = 64, comment = "刷新:refreshToken", isUnique = true)
    private String refreshToken;

    @Column(length = 64, comment = "授权:关联授权码")
    private String authCode;

    @Column(length = 32, comment = "应用:appId")
    @Index
    private String appId;

    @Column(length = 32, comment = "用户:终端用户uuid")
    private String user;

    @Column(length = 64, comment = "开放:openId")
    private String openId;

    @Column(length = 64, comment = "联合:unionId")
    private String unionId;

    @Column(comment = "过期:accessToken秒数", defaultValue = "86400")
    private long accessExpireIn;

    @Column(comment = "过期:refreshToken秒数", defaultValue = "604800")
    private long refreshExpireIn;

    @Column(length = 200, comment = "范围:granted scope")
    private String scope;

    @Column(type = DataType.DATETIME, comment = "更新:更新时间")
    private Date updateTime;
}
