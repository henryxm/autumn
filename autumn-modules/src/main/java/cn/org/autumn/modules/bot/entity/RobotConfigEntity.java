package cn.org.autumn.modules.bot.entity;

import cn.org.autumn.annotation.Cache;
import cn.org.autumn.table.annotation.Column;
import cn.org.autumn.table.annotation.Table;
import cn.org.autumn.table.data.DataType;
import com.baomidou.mybatisplus.annotations.TableId;
import com.baomidou.mybatisplus.annotations.TableName;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;

@Getter
@Setter
@TableName("bot_robot_config")
@Table(value = "bot_robot_config", comment = "用户配额:按用户覆盖机器人全局默认")
public class RobotConfigEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final int INHERIT = -1;

    @TableId
    @Column(isKey = true, type = DataType.BIGINT, length = 20, isNull = false, isAutoIncrement = true, comment = "id")
    private Long id;

    @Cache
    @Column(length = 32, comment = "用户:sys_user.uuid，唯一", isUnique = true)
    private String uuid;

    @Column(comment = "机器人数:-1继承全局默认", defaultValue = "-1")
    private int maxRobots = INHERIT;

    @Column(comment = "令牌数:-1继承全局默认", defaultValue = "-1")
    private int maxTokens = INHERIT;

    @Column(comment = "Hook数:-1继承全局默认", defaultValue = "-1")
    private int maxHooks = INHERIT;

    @Column(type = "datetime", comment = "创建:创建时间")
    private Date createTime;

    @Column(type = "datetime", comment = "更新:更新时间")
    private Date updateTime;
}
