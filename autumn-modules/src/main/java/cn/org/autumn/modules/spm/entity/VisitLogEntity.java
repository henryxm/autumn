package cn.org.autumn.modules.spm.entity;

import com.baomidou.mybatisplus.annotations.*;
import cn.org.autumn.table.annotation.*;
import cn.org.autumn.table.data.DataType;


import java.io.Serializable;
import java.util.Date;

/**
 * 访问统计
 * 
 * @author Shaohua Xu
 * @email henryxm@163.com
 * @date 2020-11
 */

@TableName("spm_visit_log")
@Table(value = "spm_visit_log", comment = "访问统计")
public class VisitLogEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * id
	 */
	@TableId
	@Column(isKey = true, type = "bigint", length = 20, isNull = false, isAutoIncrement = true, comment = "id")
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
	 * 独立访客(UV)
	 */
	@Column(length = 20, type = DataType.BIGINT,comment = "独立访客(UV)")
	private Integer uniqueVisitor;
	/**
	 * 访问量(PV)
	 */
	@Column(length = 20, type = DataType.BIGINT,comment = "访问量(PV)")
	private Integer pageView;
	/**
	 * 当天
	 */
	@Column(length = 50, comment = "当天")
	private String dayString;
	/**
	 * 创建时间
	 */
	@Column(type = "datetime", comment = "创建时间")
	private Date createTime;

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
	 * 设置：独立访客(UV)
	 */
	public void setUniqueVisitor(Integer uniqueVisitor) {
		this.uniqueVisitor = uniqueVisitor;
	}
	/**
	 * 获取：独立访客(UV)
	 */
	public Integer getUniqueVisitor() {
		return uniqueVisitor;
	}
	/**
	 * 设置：访问量(PV)
	 */
	public void setPageView(Integer pageView) {
		this.pageView = pageView;
	}
	/**
	 * 获取：访问量(PV)
	 */
	public Integer getPageView() {
		return pageView;
	}
	/**
	 * 设置：当天
	 */
	public void setDayString(String dayString) {
		this.dayString = dayString;
	}
	/**
	 * 获取：当天
	 */
	public String getDayString() {
		return dayString;
	}
	/**
	 * 设置：创建时间
	 */
	public void setCreateTime(Date createTime) {
		this.createTime = createTime;
	}
	/**
	 * 获取：创建时间
	 */
	public Date getCreateTime() {
		return createTime;
	}
}
