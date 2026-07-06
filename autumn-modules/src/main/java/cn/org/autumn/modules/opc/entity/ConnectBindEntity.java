package cn.org.autumn.modules.opc.entity;

import cn.org.autumn.annotation.Cache;
import cn.org.autumn.entity.UuidBased;
import cn.org.autumn.table.annotation.Column;
import cn.org.autumn.table.annotation.Index;
import cn.org.autumn.table.annotation.Table;
import cn.org.autumn.table.data.DataType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("opc_connect_bind")
@Table(value = "opc_connect_bind", comment = "接入绑定:本地用户与openId/unionId")
public class ConnectBindEntity implements UuidBased, Serializable {
    private static final long serialVersionUID = 1L;

    @TableId
    @Column(isKey = true, type = DataType.BIGINT, length = 20, isNull = false, isAutoIncrement = true, comment = "id")
    private Long id;

    @Cache
    @Column(length = 32, comment = "标识:业务主键", isUnique = true)
    private String uuid;

    @Column(length = 32, comment = "应用:ConnectApp.uuid")
    @Index
    private String connectApp;

    @Column(length = 32, comment = "用户:本地sys_user.uuid")
    @Index
    private String user;

    @Cache(name = "openId")
    @Column(length = 64, comment = "开放:openId")
    @Index
    private String openId;

    @Column(length = 64, comment = "联合:unionId")
    @Index
    private String unionId;

    @Column(type = DataType.DATETIME, comment = "创建:创建时间")
    private Date create;

    @Column(type = DataType.DATETIME, comment = "更新:更新时间")
    private Date update;
}
