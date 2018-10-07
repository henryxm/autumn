package cn.org.autumn.modules.stock.entity;

import com.baomidou.mybatisplus.annotations.TableId;
import com.baomidou.mybatisplus.annotations.TableName;
import cn.org.autumn.table.annotation.Column;
import cn.org.autumn.table.annotation.Table;
import cn.org.autumn.table.data.DataType;

import java.io.Serializable;
import java.util.Date;

/**
 * 股票用户访问记录
 * 
 * @author Shaohua
 * @email henryxm@163.com
 * @date 2018-10
 */
@TableName("tb_stock_visit_recode")
@Table(value = "tb_stock_visit_recode", comment = "股票用户访问记录")
public class StockVisitRecodeEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * ID
	 */
	@TableId
	@Column(isKey = true, type = DataType.BIGINT, length = 20, isNull = false, isAutoIncrement = true, comment = "ID")
	private Long id;
	/**
	 * 渠道
	 */
	@Column(comment = "渠道")
	private String channel;

	/**
	 * 设置：ID
	 */
	public void setId(Long id) {
		this.id = id;
	}
	/**
	 * 获取：ID
	 */
	public Long getId() {
		return id;
	}
	/**
	 * 设置：渠道
	 */
	public void setChannel(String channel) {
		this.channel = channel;
	}
	/**
	 * 获取：渠道
	 */
	public String getChannel() {
		return channel;
	}
}
