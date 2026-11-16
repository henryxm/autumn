package cn.org.autumn.modules.sys.dto;

import java.util.Date;

/**
 * 管理端口令变更日志视图：不含 previousPasswordHash / previousSalt。
 */
public class PasswordChangeLogView {

    private String uuid;
    private String targetUserUuid;
    private String operatorUserUuid;
    private String action;
    private String channel;
    private boolean success;
    private String ip;
    private String userAgent;
    private String remark;
    private Date createTime;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getTargetUserUuid() {
        return targetUserUuid;
    }

    public void setTargetUserUuid(String targetUserUuid) {
        this.targetUserUuid = targetUserUuid;
    }

    public String getOperatorUserUuid() {
        return operatorUserUuid;
    }

    public void setOperatorUserUuid(String operatorUserUuid) {
        this.operatorUserUuid = operatorUserUuid;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }
}
