package cn.org.autumn.modules.wall.entity;

import cn.org.autumn.table.annotation.Column;
import cn.org.autumn.table.annotation.Index;
import cn.org.autumn.table.annotation.Table;
import cn.org.autumn.table.data.DataType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;

/**
 * 管理级 IP 白名单（运维页 / 超管 API / gen 全表门禁；与用户 {@code wall_ip_white} 分离）。
 */
@Getter
@Setter
@TableName("wall_ip_admin_white")
@Table(value = "wall_ip_admin_white", comment = "管理级IP白名单")
public class IpAdminWhiteEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId
    @Column(isKey = true, type = "bigint", length = 20, isNull = false, isAutoIncrement = true, comment = "id")
    private Long id;

    @Column(isNull = false, comment = "IP地址", isUnique = true)
    private String ip;

    @Column(type = "bigint", length = 20, comment = "访问次数")
    private Long count;

    @Column(type = "bigint", length = 20, comment = "今日次数")
    private Long today;

    @Column(type = "int", length = 10, defaultValue = "0", comment = "禁用")
    private Integer forbidden;

    @Column(comment = "标签说明")
    @Index
    private String tag;

    @Column(comment = "用户代理", type = DataType.TEXT)
    private String userAgent;

    @Column(comment = "描述信息")
    private String description;

    @Column(type = "datetime", comment = "创建时间")
    private Date createTime;

    @Column(type = "datetime", comment = "更新时间")
    private Date updateTime;
}
