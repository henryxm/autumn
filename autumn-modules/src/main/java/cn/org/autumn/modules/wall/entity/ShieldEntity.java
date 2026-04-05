package cn.org.autumn.modules.wall.entity;

import cn.org.autumn.annotation.Cache;
import cn.org.autumn.table.annotation.Column;
import cn.org.autumn.table.annotation.Table;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;

@TableName("wall_shield")
@Table(value = "wall_shield", comment = "攻击防御")
public class ShieldEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId
    @Column(isKey = true, type = "bigint", length = 20, isNull = false, isAutoIncrement = true, comment = "id")
    private Long id;

    @Cache
    @Column(comment = "资源", isUnique = true, defaultValue = "")
    private String uri;

    /** 0/1，与 PG smallint / MySQL tinyint 一致；勿用 boolean，否则 JDBC 在 PostgreSQL 上会绑成 boolean 类型与列不兼容 */
    @Column(comment = "开启", defaultValue = "0")
    private int enable;

    @Column(comment = "触发:5秒IP请求次数触发自动防御模式，最低值1000", defaultValue = "10000")
    private int auto;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public int getEnable() {
        return enable;
    }

    public void setEnable(int enable) {
        this.enable = enable;
    }

    /** 勿命名为 isEnable，会与 getEnable 在 MyBatis Reflector 中冲突 */
    public boolean isEnabled() {
        return enable != 0;
    }

    public int getAuto() {
        return auto;
    }

    public void setAuto(int auto) {
        this.auto = auto;
    }
}
