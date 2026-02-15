package cn.org.autumn.modules.usr.entity;

import com.baomidou.mybatisplus.annotation.*;
import cn.org.autumn.table.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户Token
 *
 * @author Shaohua Xu
 * @email henryxm@163.com
 * @date 2020-11
 */

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@TableName("usr_user_token")
@Table(value = "usr_user_token", comment = "用户Token")
public class UserTokenEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId
    @Column(isKey = true, type = "bigint", length = 20, isNull = false, isAutoIncrement = true, comment = "ID")
    private Long id;

    @Column(length = 32, comment = "登录ID")
    @Index
    private String uuid;

    @Column(length = 32, comment = "用户ID")
    @Index
    private String userUuid;

    @Column(length = 100, comment = "Token", isUnique = true)
    private String token;

    @Column(length = 100, comment = "刷新Token")
    @Index
    private String refreshToken;

    @Column(type = "datetime", comment = "过期时间")
    private Date expireTime;

    @Column(type = "datetime", comment = "更新时间")
    private Date updateTime;

    public UserTokenEntity(String userUuid) {
        this.userUuid = userUuid;
    }

    public UserTokenEntity(String userUuid, String token) {
        this.userUuid = userUuid;
        this.token = token;
    }
}
