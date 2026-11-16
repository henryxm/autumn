package cn.org.autumn.modules.sys.entity;

import cn.org.autumn.table.annotation.Column;
import cn.org.autumn.table.annotation.Index;
import cn.org.autumn.table.annotation.Table;
import cn.org.autumn.table.data.DataType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.util.Date;

/**
 * 登录口令变更审计：记录操作者、目标用户、IP/UA，以及变更前哈希（非明文）供历史校验。
 */
@TableName("sys_password_change_log")
@Table(value = "sys_password_change_log", comment = "安全审计:登录口令变更日志")
public class PasswordChangeLogEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId
    @Column(isKey = true, type = DataType.BIGINT, length = 20, isNull = false, isAutoIncrement = true, comment = "id")
    private Long id;

    @Column(length = 32, comment = "标识:业务主键", isUnique = true)
    private String uuid;

    @Column(length = 32, comment = "目标:被改口令用户 sys_user.uuid")
    @Index
    private String targetUserUuid;

    @Column(length = 32, comment = "操作者:sys_user.uuid，匿名/系统可空")
    @Index
    private String operatorUserUuid;

    @Column(length = 32, comment = "动作:CHANGE/RESET/ADMIN_SET/CREATE/CLEAR/UPGRADE")
    private String action;

    @Column(length = 32, comment = "渠道:SELF/ADMIN/FORGOT/REGISTER/BOOTSTRAP/OAUTH_PROVISION/SYSTEM")
    private String channel;

    @Column(length = 512, comment = "旧哈希:变更前 sys_user.password，CREATE 可空")
    private String previousPasswordHash;

    @Column(length = 64, comment = "旧盐:变更前 sys_user.salt，遗留 SHA 校验用")
    private String previousSalt;

    @Column(comment = "成功:是否成功", defaultValue = "1")
    private boolean success;

    @Column(length = 64, comment = "地址:客户端IP")
    private String ip;

    @Column(length = 256, comment = "代理:User-Agent")
    private String userAgent;

    @Column(length = 256, comment = "备注:脱敏说明")
    private String remark;

    @Column(type = "datetime", comment = "时间:记录时间")
    @Index
    private Date createTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public String getPreviousPasswordHash() {
        return previousPasswordHash;
    }

    public void setPreviousPasswordHash(String previousPasswordHash) {
        this.previousPasswordHash = previousPasswordHash;
    }

    public String getPreviousSalt() {
        return previousSalt;
    }

    public void setPreviousSalt(String previousSalt) {
        this.previousSalt = previousSalt;
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
