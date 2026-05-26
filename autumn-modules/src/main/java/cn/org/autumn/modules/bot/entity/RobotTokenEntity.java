package cn.org.autumn.modules.bot.entity;

import cn.org.autumn.table.annotation.Column;
import cn.org.autumn.table.annotation.Index;
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
@TableName("bot_robot_token")
@Table(value = "bot_robot_token", comment = "机令牌:机器人API访问令牌")
public class RobotTokenEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final int STATUS_ACTIVE = 1;
    public static final int STATUS_REVOKED = 0;

    @TableId
    @Column(isKey = true, type = DataType.BIGINT, length = 20, isNull = false, isAutoIncrement = true, comment = "id")
    private Long id;

    @Column(length = 32, comment = "标识:令牌行业务主键", isUnique = true)
    private String uuid;

    @Column(length = 32, comment = "机器人:对应bot_robot.uuid")
    @Index
    private String robot;

    @Column(length = 128, comment = "摘要:令牌SHA256哈希", isUnique = true)
    private String token;

    @Column(length = 16, comment = "前缀:明文令牌前缀便于检索")
    @Index
    private String tokenPrefix;

    @Column(comment = "状态:1有效;0已作废", defaultValue = "1")
    private int status = STATUS_ACTIVE;

    @Column(type = "datetime", comment = "过期:为空表示长期有效")
    private Date expireTime;

    @Column(type = "datetime", comment = "更新:更新时间")
    private Date updateTime;

    @Column(type = "datetime", comment = "活跃:最近校验时间")
    private Date lastUsedTime;

    public boolean isActive() {
        return status == STATUS_ACTIVE;
    }
}
