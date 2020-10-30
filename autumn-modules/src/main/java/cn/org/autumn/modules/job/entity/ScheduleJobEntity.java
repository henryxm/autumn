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

import javax.validation.constraints.NotBlank;
import java.io.Serializable;
import java.util.Date;

/**
 * 定时任务
 */
@TableName("sys_schedule_job")
@Table(value = "sys_schedule_job", comment = "定时任务")
public class ScheduleJobEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 任务调度参数key
     */
    public static final String JOB_PARAM_KEY = "JOB_PARAM_KEY";

    /**
     * 任务id
     */
    @TableId
    @Column(isKey = true, type = DataType.BIGINT, length = 20, isNull = false, isAutoIncrement = true, comment = "任务id")
    private Long jobId;

    /**
     * spring bean名称
     */
    @NotBlank(message = "bean名称不能为空")
    @Column(length = 200, comment = "spring bean名称")
    private String beanName;

    /**
     * 方法名
     */
    @NotBlank(message = "方法名称不能为空")
    @Column(length = 100, comment = "方法名")
    private String methodName;

    /**
     * 参数
     */
    @Column(length = 2000, comment = "参数")
    private String params;

    /**
     * cron表达式
     */
    @NotBlank(message = "cron表达式不能为空")
    @Column(length = 100, comment = "cron表达式")
    private String cronExpression;

    /**
     * 任务状态
     */
    @Column(type = DataType.INT, comment = "任务状态  0：正常  1：暂停")
    private Integer status;

    @Column(comment = "任务执行模式")
    private String mode;

    /**
     * 备注
     */
    @Column(comment = "备注")
    private String remark;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Column(type = DataType.DATETIME, comment = "创建时间")
    private Date createTime;

    /**
     * 设置：任务id
     *
     * @param jobId 任务id
     */
    public void setJobId(Long jobId) {
        this.jobId = jobId;
    }

    /**
     * 获取：任务id
     *
     * @return Long
     */
    public Long getJobId() {
        return jobId;
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

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    /**
     * 设置：任务状态
     *
     * @param status 任务状态
     */
    public void setStatus(Integer status) {
        this.status = status;
    }

    /**
     * 获取：任务状态
     *
     * @return String
     */
    public Integer getStatus() {
        return status;
    }

    /**
     * 设置：cron表达式
     *
     * @param cronExpression cron表达式
     */
    public void setCronExpression(String cronExpression) {
        this.cronExpression = cronExpression;
    }

    /**
     * 获取：cron表达式
     *
     * @return String
     */
    public String getCronExpression() {
        return cronExpression;
    }

    /**
     * 设置：创建时间
     *
     * @param createTime 创建时间
     */
    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    /**
     * 获取：创建时间
     *
     * @return Date
     */
    public Date getCreateTime() {
        return createTime;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }
}
