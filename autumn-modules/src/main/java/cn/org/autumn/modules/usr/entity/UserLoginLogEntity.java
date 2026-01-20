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

    @Column(length = 32, comment = "用户ID")
    @Index
    private String uuid;

    @Column(length = 32, comment = "登录账号")
    @Index
    private String account;

    @Column(length = 100, comment = "方式:登录方式")
    @Index
    private String way;

    @Column(length = 40, comment = "IP地址")
    @Index
    private String ip;

    @Column(comment = "退出")
    private boolean logout;

    @Column(comment = "允许")
    private boolean allow;

    @Column(comment = "代理")
    private String agent;

    @Column(comment = "原因")
    private String reason;

    @Column(comment = "创建")
    private Date create;
}
