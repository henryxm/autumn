package cn.org.autumn.modules.bot.entity;

import cn.org.autumn.entity.UuidBased;
import cn.org.autumn.table.annotation.Column;
import cn.org.autumn.table.annotation.Index;
import cn.org.autumn.table.annotation.Table;
import cn.org.autumn.table.data.DataType;
import com.baomidou.mybatisplus.annotations.TableId;
import com.baomidou.mybatisplus.annotations.TableName;
import java.util.Date;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("bot_robot_hook")
@Table(value = "bot_robot_hook", comment = "机回调:机器人业务推送Hook")
public class RobotHookEntity implements UuidBased {
    private static final long serialVersionUID = 1L;

    public static final int STATUS_ACTIVE = 1;
    public static final int STATUS_DISABLED = 0;

    @TableId
    @Column(isKey = true, type = DataType.BIGINT, length = 20, isNull = false, isAutoIncrement = true, comment = "id")
    private Long id;

    @Column(length = 32, comment = "标识:Hook全局唯一业务主键", isUnique = true)
    private String uuid;

    @Column(length = 32, comment = "机器人:对应bot_robot.uuid")
    @Index
    private String robot;

    @Column(length = 32, comment = "用户:所属用户uuid")
    @Index
    private String owner;

    @Column(length = 100, comment = "名称:Hook名称")
    @Index
    private String name;

    @Column(length = 500, comment = "地址:回调URL")
    private String callback;

    @Column(length = 128, comment = "密钥:回调签名校验")
    private String secret;

    @Column(length = 500, comment = "事件:订阅事件CSV")
    private String events;

    @Column(type = DataType.TEXT, comment = "描述:用途说明")
    private String description;

    @Column(comment = "状态:1启用;0停用", defaultValue = "1")
    private int status = STATUS_ACTIVE;

    @Column(type = "datetime", comment = "创建:创建时间")
    private Date createTime;

    @Column(type = "datetime", comment = "更新:更新时间")
    private Date updateTime;

    @Column(type = "datetime", comment = "回调:最近推送时间")
    private Date lastInvokeTime;

    public boolean isActive() {
        return status == STATUS_ACTIVE;
    }
}
