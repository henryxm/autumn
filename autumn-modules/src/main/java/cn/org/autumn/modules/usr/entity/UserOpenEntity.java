package cn.org.autumn.modules.usr.entity;

import cn.org.autumn.table.annotation.Column;
import cn.org.autumn.table.annotation.Table;
import cn.org.autumn.table.data.DataType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;

@Getter
@Setter
@TableName("usr_user_open")
@Table(value = "usr_user_open", comment = "认证对接")
public class UserOpenEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId
    @Column(isKey = true, type = DataType.BIGINT, length = 20, isNull = false, isAutoIncrement = true, comment = "id")
    private Long id;

    @Column(length = 32, comment = "用户")
    private String uuid;

    @Column(length = 32, comment = "平台")
    private String platform;

    @Column(comment = "应用ID")
    private String appid;

    @Column(comment = "开放ID")
    private String openid;

    @Column(comment = "联合ID")
    private String unionid;

    @Column(comment = "删除", defaultValue = "0")
    private boolean deleted;

    @Column(comment = "创建")
    @TableField("`create`")
    private Date create;

    @Column(comment = "更新")
    @TableField("`update`")
    private Date update;
}
