/**
 * Copyright 2018 Autumn.org.cn http://www.autumn.org.cn
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package cn.org.autumn.modules.job.entity;

import cn.org.autumn.table.annotation.Column;
import cn.org.autumn.table.annotation.Table;
import cn.org.autumn.table.data.DataType;
import com.baomidou.mybatisplus.annotations.TableId;
import com.baomidou.mybatisplus.annotations.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.io.Serializable;
import java.util.Date;

/**
 * 定时任务日志
 */
@TableName("sys_schedule_job_log")
@Table(value = "sys_schedule_job_log", comment = "任务日志")
public class ScheduleJobLogEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 日志id
     */
    @TableId
    @Column(isKey = true, type = DataType.BIGINT, length = 20, isNull = false, isAutoIncrement = true, comment = "任务日志id")
    private Long logId;

    /**
     * 任务id
     */
    @Column(type = DataType.BIGINT, length = 20, isNull = false,defaultValue = "0", comment = "任务id")
    private Long jobId;

    /**
     * spring bean名称
     */
    @Column(length = 200, comment = "BeanName")
    private String beanName;

    /**
     * 方法名
     */
    @Column(length = 100, comment = "方法名")
    private String methodName;

    /**
     * 参数
     */
    @Column(length = 2000, comment = "参数")
    private String params;

    /**
     * 任务状态    0：成功    1：失败
     */
    @Column(type = DataType.INT,comment = "任务状态,0:成功,1:失败")
    private Integer status;

    /**
     * 失败信息
     */
    @Column(length = 2000, comment = "失败信息")
    private String error;

    /**
     * 耗时(单位：毫秒)
     */
    @Column(type = DataType.INT,length = 11, isNull = false, defaultValue = "0", comment = "耗时(单位：毫秒)")
    private Integer times;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Column(type = DataType.DATETIME, comment = "创建时间")
    private Date createTime;

    public Long getLogId() {
        return logId;
    }

    public void setLogId(Long logId) {
        this.logId = logId;
    }

    public Long getJobId() {
        return jobId;
    }

    public void setJobId(Long jobId) {
        this.jobId = jobId;
    }

    public String getBeanName() {
        return beanName;
    }

    public void setBeanName(String beanName) {
        this.beanName = beanName;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public String getParams() {
        return params;
    }

    public void setParams(String params) {
        this.params = params;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public Integer getTimes() {
        return times;
    }

    public void setTimes(Integer times) {
        this.times = times;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

}
