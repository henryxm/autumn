package cn.org.autumn.modules.wall.entity;

import com.baomidou.mybatisplus.annotations.*;
import cn.org.autumn.table.annotation.*;
import cn.org.autumn.table.data.DataType;


import java.io.Serializable;
import java.util.Date;

/**
 * 主机统计
 *
 * @author Shaohua Xu
 * @email henryxm@163.com
 * @date 2020-11
 */
@TableName("wall_host")
@Table(value = "wall_host", comment = "主机统计")
public class HostEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    @TableId
    @Column(isKey = true, type = "bigint", length = 20, isNull = false, isAutoIncrement = true, comment = "id")
    private Long id;
    /**
     * 主机地址
     */
    @Column(isNull = false, comment = "主机地址", isUnique = true)
    private String host;
    /**
     * 访问次数
     */
    @Column(type = "bigint", length = 20, comment = "访问次数")
    private Long count;

    @Column(type = "bigint", length = 20, comment = "今日次数")
    private Long today;
    /**
     * 禁用
     */
    @Column(type = "int", length = 10, defaultValue = "0", comment = "禁用")
    private Integer forbidden;
    /**
     * 标签说明
     */
    @Column(comment = "标签说明")
    private String tag;
    /**
     * 描述信息
     */
    @Column(comment = "描述信息")
    private String description;
    /**
     * 创建时间
     */
    @Column(type = "datetime", comment = "创建时间")
    private Date createTime;
    /**
     * 更新时间
     */
    @Column(type = "datetime", comment = "更新时间")
    private Date updateTime;

    /**
     * 设置：id
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * 获取：id
     */
    public Long getId() {
        return id;
    }

    /**
     * 设置：主机地址
     */
    public void setHost(String host) {
        this.host = host;
    }

    /**
     * 获取：主机地址
     */
    public String getHost() {
        return host;
    }

    /**
     * 设置：访问次数
     */
    public void setCount(Long count) {
        this.count = count;
    }

    /**
     * 获取：访问次数
     */
    public Long getCount() {
        return count;
    }

    public Long getToday() {
        return today;
    }

    public void setToday(Long today) {
        this.today = today;
    }

    /**
     * 设置：禁用
     */
    public void setForbidden(Integer forbidden) {
        this.forbidden = forbidden;
    }

    /**
     * 获取：禁用
     */
    public Integer getForbidden() {
        return forbidden;
    }

    /**
     * 设置：标签说明
     */
    public void setTag(String tag) {
        this.tag = tag;
    }

    /**
     * 获取：标签说明
     */
    public String getTag() {
        return tag;
    }

    /**
     * 设置：描述信息
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * 获取：描述信息
     */
    public String getDescription() {
        return description;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }
}
