package cn.org.autumn.modules.sys.entity;

import cn.org.autumn.table.annotation.Column;
import cn.org.autumn.table.annotation.Table;
import cn.org.autumn.table.data.DataType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.io.Serializable;
import java.util.Date;

/**
 * 系统日志
 */
@TableName("sys_log")
@Table(value = "sys_log", comment = "系统日志")
public class SysLogEntity implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId
    @Column(isKey = true, type = DataType.BIGINT, length = 20, isNull = false, isAutoIncrement = true, comment = "id")
    private Long id;
    //用户名
    @Column(length = 50, comment = "用户名")
    private String username;
    //用户操作
    @Column(length = 50, comment = "用户操作")
    private String operation;
    //请求方法
    @Column(length = 200, comment = "请求方法")
    private String method;
    //请求参数
    @Column(length = 5000, comment = "请求参数")
    private String params;
    //执行时长(毫秒)
    @Column(type = DataType.BIGINT, length = 20, comment = "执行时长(毫秒)")
    private Long time;
    //IP地址
    @Column(length = 64, comment = "IP地址")
    private String ip;
    //创建时间
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Column(type = DataType.DATETIME, comment = "创建时间")
    private Date createDate;

    /**
     * 设置：
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * 获取：
     */
    public Long getId() {
        return id;
    }

    /**
     * 设置：用户名
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * 获取：用户名
     */
    public String getUsername() {
        return username;
    }

    /**
     * 设置：用户操作
     */
    public void setOperation(String operation) {
        this.operation = operation;
    }

    /**
     * 获取：用户操作
     */
    public String getOperation() {
        return operation;
    }

    /**
     * 设置：请求方法
     */
    public void setMethod(String method) {
        this.method = method;
    }

    /**
     * 获取：请求方法
     */
    public String getMethod() {
        return method;
    }

    /**
     * 设置：请求参数
     */
    public void setParams(String params) {
        this.params = params;
    }

    /**
     * 获取：请求参数
     */
    public String getParams() {
        return params;
    }

    /**
     * 设置：IP地址
     */
    public void setIp(String ip) {
        this.ip = ip;
    }

    /**
     * 获取：IP地址
     */
    public String getIp() {
        return ip;
    }

    /**
     * 设置：创建时间
     */
    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

    /**
     * 获取：创建时间
     */
    public Date getCreateDate() {
        return createDate;
    }

    public Long getTime() {
        return time;
    }

    public void setTime(Long time) {
        this.time = time;
    }
}
