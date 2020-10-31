package cn.org.autumn.modules.spm.entity;

import com.baomidou.mybatisplus.annotations.*;
import cn.org.autumn.table.annotation.*;
import org.apache.commons.lang.StringUtils;


import java.io.Serializable;

/**
 * 超级位置模型
 *
 * @author Shaohua Xu
 * @email henryxm@163.com
 * @date 2020-10
 */

@TableName("spm_super_position_model")
@Table(value = "spm_super_position_model" , comment = "超级位置模型")
public class SuperPositionModelEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    @TableId
    @Column(isKey = true, type = "bigint" , length = 20, isNull = false, isAutoIncrement = true, comment = "id")
    private Long id;
    /**
     * 网站ID
     */
    @Column(length = 50, comment = "网站ID")
    private String siteId;
    /**
     * 网页ID
     */
    @Column(length = 50, comment = "网页ID")
    private String pageId;
    /**
     * 频道ID
     */
    @Column(length = 50, comment = "频道ID")
    private String channelId;
    /**
     * 产品ID
     */
    @Column(length = 50, comment = "产品ID")
    private String productId;
    /**
     * 资源ID
     */
    @Column(length = 250, comment = "资源ID")
    private String resourceId;

    @Column(length = 20, comment = "需要登录" , defaultValue = "0")
    private Integer needLogin;

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
     * 设置：网站ID
     */
    public void setSiteId(String siteId) {
        this.siteId = siteId;
    }

    /**
     * 获取：网站ID
     */
    public String getSiteId() {
        return siteId;
    }

    /**
     * 设置：网页ID
     */
    public void setPageId(String pageId) {
        this.pageId = pageId;
    }

    /**
     * 获取：网页ID
     */
    public String getPageId() {
        return pageId;
    }

    /**
     * 设置：频道ID
     */
    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }

    /**
     * 获取：频道ID
     */
    public String getChannelId() {
        return channelId;
    }

    /**
     * 设置：产品ID
     */
    public void setProductId(String productId) {
        this.productId = productId;
    }

    /**
     * 获取：产品ID
     */
    public String getProductId() {
        return productId;
    }

    /**
     * 设置：资源ID
     */
    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    /**
     * 获取：资源ID
     */
    public String getResourceId() {
        return resourceId;
    }

    public Integer getNeedLogin() {
        return needLogin;
    }

    public void setNeedLogin(Integer needLogin) {
        this.needLogin = needLogin;
    }

    public String toString() {
        String spm = siteId;
        if (StringUtils.isNotEmpty(pageId))
            spm += "." + pageId;
        if (StringUtils.isNotEmpty(channelId))
            spm += "." + channelId;
        if (StringUtils.isNotEmpty(productId))
            spm += "." + productId;
        return spm;
    }
}
