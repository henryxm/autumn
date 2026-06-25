package cn.org.autumn.model;

import cn.org.autumn.annotation.ConfigField;
import cn.org.autumn.annotation.ConfigParam;
import cn.org.autumn.config.InputType;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 * 支付密码、手势与生物识别全局策略。
 */
@Getter
@Setter
@ConfigParam(paramKey = PayCredentialConfig.CONFIG_KEY, category = PayCredentialConfig.config, name = "支付凭证", description = "支付密码锁定、校验令牌有效期等")
public class PayCredentialConfig implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String CONFIG_KEY = "PAY_CREDENTIAL_CONFIG";
    public static final String config = "pay_credential_config";

    @ConfigField(category = InputType.NumberType, name = "支付密码位数", description = "默认6位数字")
    private int pinLength = 6;

    @ConfigField(category = InputType.NumberType, name = "最大失败次数", description = "连续校验失败后锁定")
    private int maxFailAttempts = 5;

    @ConfigField(category = InputType.NumberType, name = "锁定时长分钟", description = "锁定持续时间")
    private int lockMinutes = 30;

    @ConfigField(category = InputType.NumberType, name = "校验令牌分钟", description = "verify 成功后令牌有效时间")
    private int verifyTokenMinutes = 5;

    @ConfigField(category = InputType.NumberType, name = "挑战有效分钟", description = "生物识别 challenge 有效时间")
    private int challengeMinutes = 5;

    @ConfigField(category = InputType.NumberType, name = "手势最少点数", description = "手势轨迹最少连接点数")
    private int gestureMinPoints = 4;

    @ConfigField(category = InputType.NumberType, name = "单用户设备数", description = "每用户最多绑定的生物识别设备数，0不限")
    private int maxBiometricDevices = 0;

    @ConfigField(category = InputType.BooleanType, name = "记录操作日志", description = "是否写入 safe_pay_credential_log 审计表")
    private boolean auditLogEnabled = true;

    @ConfigField(category = InputType.NumberType, name = "日志保留天数", description = "超过该天数的操作日志每日物理删除，0表示不自动清理")
    private int logRetentionDays = 180;

    @ConfigField(category = InputType.BooleanType, name = "启用支付闸门", description = "支付前须先调用 gate/assess 评估")
    private boolean gateEnabled = true;

    @ConfigField(category = InputType.BooleanType, name = "启用小额免密", description = "窗口期内低于阈值的支付可免输支付密码")
    private boolean passwordlessEnabled = true;

    @ConfigField(category = InputType.NumberType, name = "免密金额分", description = "低于等于该金额(分)可免密，默认1000即10元")
    private long passwordlessMaxAmountCent = 1000L;

    @ConfigField(category = InputType.NumberType, name = "免密窗口分钟", description = "最近一次校验成功后免密有效分钟，默认15")
    private int passwordlessWindowMinutes = 15;

    @ConfigField(category = InputType.NumberType, name = "闸门令牌分钟", description = "gateToken 有效分钟")
    private int gateTokenMinutes = 5;

    @ConfigField(category = InputType.NumberType, name = "高额支付分", description = "超过该金额(分)强制输支付密码，默认50000即500元")
    private long highAmountThresholdCent = 50000L;

    @ConfigField(category = InputType.NumberType, name = "同额提醒分钟", description = "短时相同金额支付检测窗口")
    private int duplicateAmountWindowMinutes = 10;

    @ConfigField(category = InputType.NumberType, name = "同额提醒次数", description = "窗口内相同金额达到此次数则强制输密码")
    private int duplicateAmountAlertCount = 2;

    @ConfigField(category = InputType.BooleanType, name = "免密须常用设备", description = "免密时设备须在信任列表或已注册生物设备")
    private boolean passwordlessRequireTrustedDevice = false;

    @ConfigField(category = InputType.BooleanType, name = "免密须常用IP", description = "免密时IP须在信任列表")
    private boolean passwordlessRequireTrustedIp = false;

    @ConfigField(category = InputType.BooleanType, name = "记录闸门评估", description = "assess 摘要写入 safe_pay_credential_log")
    private boolean auditGateEnabled = true;

    @ConfigField(category = InputType.BooleanType, name = "校验令牌绑定金额", description = "requireVerifyToken 校验金额与订单")
    private boolean verifyTokenBindAmount = true;

    @ConfigField(category = InputType.BooleanType, name = "新设备强制输密", description = "非常用设备支付一律须校验支付密码")
    private boolean newDeviceRequirePassword = true;

    @ConfigField(category = InputType.NumberType, name = "免密日累计次数", description = "每用户每日免密支付上限，0不限")
    private int passwordlessDailyMaxCount = 0;

    @ConfigField(category = InputType.NumberType, name = "免密日累计分", description = "每用户每日免密金额上限(分)，0不限")
    private long passwordlessDailyMaxAmountCent = 0L;

    @ConfigField(category = InputType.NumberType, name = "客户端时差秒", description = "assess 客户端时间与服务器偏差超过则警告")
    private int clientTimeSkewSeconds = 300;

    public List<String> validateAndFix() {
        List<String> fixes = new ArrayList<>();
        if (pinLength < 4 || pinLength > 12) {
            int old = pinLength;
            pinLength = 6;
            fixes.add(String.format("支付密码位数不合理:%d，已修正为:%d", old, pinLength));
        }
        if (maxFailAttempts <= 0) {
            int old = maxFailAttempts;
            maxFailAttempts = 5;
            fixes.add(String.format("最大失败次数不合理:%d，已修正为:%d", old, maxFailAttempts));
        }
        if (lockMinutes <= 0) {
            int old = lockMinutes;
            lockMinutes = 30;
            fixes.add(String.format("锁定时长不合理:%d，已修正为:%d", old, lockMinutes));
        }
        if (verifyTokenMinutes <= 0) {
            int old = verifyTokenMinutes;
            verifyTokenMinutes = 5;
            fixes.add(String.format("校验令牌分钟不合理:%d，已修正为:%d", old, verifyTokenMinutes));
        }
        if (challengeMinutes <= 0) {
            int old = challengeMinutes;
            challengeMinutes = 5;
            fixes.add(String.format("挑战有效分钟不合理:%d，已修正为:%d", old, challengeMinutes));
        }
        if (gestureMinPoints < 4) {
            int old = gestureMinPoints;
            gestureMinPoints = 4;
            fixes.add(String.format("手势最少点数不合理:%d，已修正为:%d", old, gestureMinPoints));
        }
        if (maxBiometricDevices < 0) {
            int old = maxBiometricDevices;
            maxBiometricDevices = 10;
            fixes.add(String.format("单用户设备数不合理:%d，已修正为:%d", old, maxBiometricDevices));
        }
        if (logRetentionDays < 0) {
            int old = logRetentionDays;
            logRetentionDays = 180;
            fixes.add(String.format("日志保留天数不合理:%d，已修正为:%d", old, logRetentionDays));
        }
        if (passwordlessMaxAmountCent < 0) {
            long old = passwordlessMaxAmountCent;
            passwordlessMaxAmountCent = 1000L;
            fixes.add(String.format("免密金额分不合理:%d，已修正为:%d", old, passwordlessMaxAmountCent));
        }
        if (passwordlessWindowMinutes <= 0) {
            int old = passwordlessWindowMinutes;
            passwordlessWindowMinutes = 15;
            fixes.add(String.format("免密窗口分钟不合理:%d，已修正为:%d", old, passwordlessWindowMinutes));
        }
        if (gateTokenMinutes <= 0) {
            int old = gateTokenMinutes;
            gateTokenMinutes = 5;
            fixes.add(String.format("闸门令牌分钟不合理:%d，已修正为:%d", old, gateTokenMinutes));
        }
        if (highAmountThresholdCent <= 0) {
            long old = highAmountThresholdCent;
            highAmountThresholdCent = 50000L;
            fixes.add(String.format("高额支付分不合理:%d，已修正为:%d", old, highAmountThresholdCent));
        }
        if (duplicateAmountWindowMinutes <= 0) {
            int old = duplicateAmountWindowMinutes;
            duplicateAmountWindowMinutes = 10;
            fixes.add(String.format("同额提醒分钟不合理:%d，已修正为:%d", old, duplicateAmountWindowMinutes));
        }
        if (duplicateAmountAlertCount <= 0) {
            int old = duplicateAmountAlertCount;
            duplicateAmountAlertCount = 2;
            fixes.add(String.format("同额提醒次数不合理:%d，已修正为:%d", old, duplicateAmountAlertCount));
        }
        return fixes;
    }
}
