package cn.org.autumn.modules.usr.entity;

import com.baomidou.mybatisplus.annotations.*;
import cn.org.autumn.table.annotation.*;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;

@Getter
@Setter
@TableName("usr_user_login_log")
@Table(value = "usr_user_login_log", comment = "登录日志")
public class UserLoginLogEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId
    @Column(isKey = true, type = "bigint", length = 20, isNull = false, isAutoIncrement = true, comment = "ID")
    private Long id;

    @Column(length = 32, comment = "用户ID", defaultValue = "")
    @Index
    private String uuid;

    @Column(length = 32, comment = "账号", defaultValue = "")
    @Index
    private String account;

    @Column(length = 100, comment = "方式:登录方式", defaultValue = "")
    @Index
    private String way;

    @Column(length = 64, comment = "主机", defaultValue = "")
    @Index
    private String host;

    @Column(length = 40, comment = "IP地址", defaultValue = "")
    @Index
    private String ip;

    @Column(length = 64, comment = "会话", defaultValue = "")
    @Index
    private String session;

    @Column(length = 64, comment = "路径", defaultValue = "")
    @Index
    private String path;

    @Column(comment = "代理", defaultValue = "")
    private String agent;

    @Column(comment = "白名单:开启限制后，该记录对应的指定IP可登录，其它IP被限制登录", defaultValue = "0")
    private boolean white;

    @Column(comment = "退出", defaultValue = "0")
    private boolean logout;

    @Column(comment = "允许", defaultValue = "1")
    private boolean allow;

    @Column(comment = "原因", defaultValue = "")
    private String reason;

    @Column(comment = "创建")
    private Date create;
}
