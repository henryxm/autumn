package cn.org.autumn.modules.usr.entity;

import cn.org.autumn.table.data.DataType;
import com.baomidou.mybatisplus.annotations.*;
import cn.org.autumn.table.annotation.*;
import com.baomidou.mybatisplus.enums.IdType;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;

@Getter
@Setter
@TableName("usr_user_profile")
@Table(value = "usr_user_profile", comment = "用户信息")
public class UserProfileEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.INPUT)
    @Column(isKey = true, comment = "UUID", length = 50, isUnique = true)
    private String uuid;

    @Column(comment = "头像")
    private String icon;

    @Column(length = 50, comment = "用户名", isUnique = true)
    private String username;

    @Column(length = 50, comment = "用户昵称")
    private String nickname;

    @Column(length = 20, comment = "手机号")
    private String mobile;

    @Column(comment = "登录IP")
    private String loginIp;

    @Column(comment = "访问IP")
    private String visitIp;

    @Column(comment = "用户代理", type = DataType.TEXT)
    private String userAgent;

    @Column(type = "datetime", comment = "登录时间")
    private Date loginTime;

    @Column(type = "datetime", comment = "访问时间")
    private Date visitTime;

    @Column(type = "datetime", comment = "创建时间")
    private Date createTime;
}
