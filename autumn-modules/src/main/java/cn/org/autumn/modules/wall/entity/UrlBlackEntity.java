package cn.org.autumn.modules.wall.entity;

import com.baomidou.mybatisplus.annotations.*;
import cn.org.autumn.table.annotation.*;

import java.io.Serializable;

/**
 * 链接黑名单
 *
 * @author Shaohua Xu
 * @email henryxm@163.com
 * @date 2020-11
 */

@TableName("wall_url_black")
@Table(value = "wall_url_black", comment = "链接黑名单")
public class UrlBlackEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    @TableId
    @Column(isKey = true, type = "bigint", length = 20, isNull = false, isAutoIncrement = true, comment = "id")
    private Long id;
    /**
     * URL地址
     */
    @Column(comment = "URL地址", isUnique = true)
    private String url;
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
    @Column(type = "int", length = 10, comment = "禁用")
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
     * 设置：URL地址
     */
    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * 获取：URL地址
     */
    public String getUrl() {
        return url;
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
}
