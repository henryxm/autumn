package cn.org.autumn.modules.safe.entity;

import cn.org.autumn.annotation.Cache;
import cn.org.autumn.table.annotation.Column;
import cn.org.autumn.table.annotation.Table;
import cn.org.autumn.table.data.DataType;
import com.baomidou.mybatisplus.annotations.TableId;
import com.baomidou.mybatisplus.annotations.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;

import static com.fasterxml.jackson.annotation.JsonProperty.Access.WRITE_ONLY;

@Getter
@Setter
@TableName("safe_pay_user_gesture")
@Table(comment = "手势密码:用户九宫格手势")
public class PayUserGestureEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final int STATUS_UNSET = 0;
    public static final int STATUS_NORMAL = 1;
    public static final int STATUS_LOCKED = 2;

    @TableId
    @Column(isKey = true, type = DataType.BIGINT, length = 20, isNull = false, isAutoIncrement = true, comment = "id")
    private Long id;

    @Column(length = 32, comment = "标识:业务主键", isUnique = true)
    private String uuid;

    @Cache(name = "userUuid", unique = true)
    @Column(length = 32, comment = "用户:sys_user.uuid", isUnique = true)
    private String userUuid;

    @JsonProperty(access = WRITE_ONLY)
    @Column(length = 128, comment = "摘要:手势哈希")
    private String gestureHash;

    @JsonProperty(access = WRITE_ONLY)
    @Column(length = 32, comment = "盐值:手势盐")
    private String salt;

    @Column(comment = "状态:0未设置;1正常;2锁定", defaultValue = "0")
    private int status = STATUS_UNSET;

    @Column(comment = "失败:连续错误次数", defaultValue = "0")
    private int failCount;

    @Column(type = "datetime", comment = "锁定:解锁时间")
    private Date lockedUntil;

    @Column(type = "datetime", comment = "设置:首次设置时间")
    private Date setTime;

    @Column(type = "datetime", comment = "更新:最近变更时间")
    private Date updateTime;
}
