package cn.org.autumn.modules.auth.entity;

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

@Getter
@Setter
@TableName("auth_scope_definition")
@Table(comment = "授权范围:OAuth/OPL scope目录")
public class ScopeDefinitionEntity implements UuidBased, Serializable {
    private static final long serialVersionUID = 1L;

    @TableId
    @Column(isKey = true, type = DataType.BIGINT, length = 20, isNull = false, isAutoIncrement = true, comment = "id")
    private Long id;

    @Column(length = 32, comment = "标识:行全局唯一业务主键", isUnique = true)
    private String uuid;

    @Column(length = 32, comment = "代码:scope代码", isUnique = true)
    private String code;

    @Column(length = 100, comment = "名称:展示名称")
    private String label;

    @Column(length = 32, comment = "轨道:oauth,opl CSV")
    private String tracks;

    @Column(length = 200, comment = "字段:输出字段CSV")
    private String fields;

    @Column(length = 16, comment = "敏感:low/medium/high", defaultValue = "low")
    private String sensitivity;

    @Column(length = 100, comment = "依赖:前置scope CSV")
    private String requires;

    @Column(type = DataType.TINYINT, length = 1, comment = "启用:是否可用", defaultValue = "1")
    private boolean enabled = true;

    @Column(type = DataType.TINYINT, length = 1, comment = "内置:是否系统内置", defaultValue = "0")
    private boolean builtin;

    @Column(type = "datetime", comment = "更新")
    private Date updated;
}
