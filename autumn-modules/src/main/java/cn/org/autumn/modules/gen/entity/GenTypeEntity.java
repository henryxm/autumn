package cn.org.autumn.modules.gen.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import cn.org.autumn.table.annotation.Column;
import cn.org.autumn.table.annotation.Table;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@TableName("sys_gen_type")
@Table(value = "sys_gen_type", comment = "生成方案")
public class GenTypeEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId
    @Column(isKey = true, type = "bigint", length = 20, isNull = false, isAutoIncrement = true, comment = "序列号")
    private Long id;

    @Column(length = 100, comment = "数据库类型")
    private String databaseType;

    @Column(comment = "程序根包名")
    private String rootPackage;

    @Column(comment = "模块根包名")
    private String modulePackage;

    @Column(comment = "模块名(用于包名)")
    private String moduleName;

    @Column(comment = "模块名称(用于目录)")
    private String moduleText;

    @Column(comment = "模块ID(用于目录)")
    private String moduleId;

    @Column(comment = "作者名字")
    private String authorName;

    @Column(comment = "作者邮箱")
    private String email;

    @Column(comment = "表前缀")
    private String tablePrefix;

    @Column(length = 5000, comment = "表字段映射")
    private String mappingString;
}
