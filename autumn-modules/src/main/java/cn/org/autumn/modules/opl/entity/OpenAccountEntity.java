package cn.org.autumn.modules.opl.entity;

import cn.org.autumn.annotation.Cache;
import cn.org.autumn.entity.UuidBased;
import cn.org.autumn.table.annotation.Column;
import cn.org.autumn.table.annotation.Table;
import cn.org.autumn.table.data.DataType;
import com.baomidou.mybatisplus.annotations.TableId;
import com.baomidou.mybatisplus.annotations.TableName;
import java.io.Serializable;
import java.util.Date;
import lombok.Getter;
import lombok.Setter;

/**
 * 开放平台开发者主体账号。
 * <p>
 * 账号下终端用户的 unionId 见 {@link OpenUnionEntity} / {@link OpenUnionService}。
 */
@Getter
@Setter
@TableName("opl_open_account")
@Table(value = "opl_open_account", comment = "开发者:开放平台主体账号")
public class OpenAccountEntity implements UuidBased, Serializable {
    private static final long serialVersionUID = 1L;

    public static final int STATUS_ACTIVE = 1;
    public static final int STATUS_DISABLED = 0;

    @TableId
    @Column(isKey = true, type = DataType.BIGINT, length = 20, isNull = false, isAutoIncrement = true, comment = "id")
    private Long id;

    @Cache
    @Column(length = 32, comment = "标识:业务主键", isUnique = true)
    private String uuid;

    @Cache(name = "user")
    @Column(length = 32, comment = "用户:sys_user.uuid", isUnique = true)
    private String user;

    @Column(length = 100, comment = "名称:开发者主体名称")
    private String name;

    @Column(comment = "状态:1正常;0禁用", defaultValue = "1")
    private int status = STATUS_ACTIVE;

    @Column(type = DataType.DATETIME, comment = "创建:创建时间")
    private Date create;

    @Column(type = DataType.DATETIME, comment = "更新:更新时间")
    private Date update;
}
