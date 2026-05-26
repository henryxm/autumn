package cn.org.autumn.model;

import cn.org.autumn.annotation.ConfigField;
import cn.org.autumn.annotation.ConfigParam;
import cn.org.autumn.config.InputType;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 机器人全局配额（用户可创建机器人数、每机器人令牌数、Hook 数）。
 */
@ConfigParam(paramKey = RobotQuotaConfig.CONFIG_KEY, category = RobotQuotaConfig.config, name = "机器人配额", description = "配置用户与机器人资源默认上限")
public class RobotQuotaConfig implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String CONFIG_KEY = "ROBOT_QUOTA_CONFIG";
    public static final String config = "robot_quota_config";

    @ConfigField(category = InputType.NumberType, name = "用户机器人数", description = "每个用户默认可创建的机器人数量上限")
    private int maxRobotsPerUser = 5;

    @ConfigField(category = InputType.NumberType, name = "机器人令牌数", description = "每个机器人默认可同时有效的 API 令牌数量")
    private int maxTokensPerRobot = 2;

    @ConfigField(category = InputType.NumberType, name = "机器人Hook数", description = "每个机器人默认可创建的 Hook 数量")
    private int maxHooksPerRobot = 2;

    @ConfigField(category = InputType.NumberType, name = "入站推送限流", description = "每个机器人每分钟允许的消息推送次数，0 表示不限制")
    private int maxMessagePushPerMinute = 60;

    @ConfigField(category = InputType.NumberType, name = "Hook投递重试", description = "Hook HTTP 回调队列最大重试次数")
    private int hookDispatchRetries = 5;

    @ConfigField(category = InputType.NumberType, name = "入站消息重试", description = "入站消息分发队列最大重试次数")
    private int messageDispatchRetries = 3;

    @ConfigField(category = InputType.NumberType, name = "幂等缓存小时", description = "入站消息幂等键在缓存中保留的小时数")
    private int messageIdempotencyHours = 24;

    public List<String> validateAndFix() {
        List<String> fixes = new ArrayList<>();
        if (maxRobotsPerUser <= 0) {
            int old = maxRobotsPerUser;
            maxRobotsPerUser = 5;
            fixes.add(String.format("用户机器人数不合理:%d，已修正为:%d", old, maxRobotsPerUser));
        }
        if (maxTokensPerRobot <= 0) {
            int old = maxTokensPerRobot;
            maxTokensPerRobot = 2;
            fixes.add(String.format("机器人令牌数不合理:%d，已修正为:%d", old, maxTokensPerRobot));
        }
        if (maxHooksPerRobot <= 0) {
            int old = maxHooksPerRobot;
            maxHooksPerRobot = 2;
            fixes.add(String.format("机器人Hook数不合理:%d，已修正为:%d", old, maxHooksPerRobot));
        }
        if (maxMessagePushPerMinute < 0) {
            int old = maxMessagePushPerMinute;
            maxMessagePushPerMinute = 60;
            fixes.add(String.format("入站推送限流不合理:%d，已修正为:%d", old, maxMessagePushPerMinute));
        }
        if (hookDispatchRetries < 0) {
            int old = hookDispatchRetries;
            hookDispatchRetries = 5;
            fixes.add(String.format("Hook投递重试不合理:%d，已修正为:%d", old, hookDispatchRetries));
        }
        if (messageDispatchRetries < 0) {
            int old = messageDispatchRetries;
            messageDispatchRetries = 3;
            fixes.add(String.format("入站消息重试不合理:%d，已修正为:%d", old, messageDispatchRetries));
        }
        if (messageIdempotencyHours <= 0) {
            int old = messageIdempotencyHours;
            messageIdempotencyHours = 24;
            fixes.add(String.format("幂等缓存小时不合理:%d，已修正为:%d", old, messageIdempotencyHours));
        }
        return fixes;
    }

    public int getMaxRobotsPerUser() {
        return maxRobotsPerUser;
    }

    public void setMaxRobotsPerUser(int maxRobotsPerUser) {
        this.maxRobotsPerUser = maxRobotsPerUser;
    }

    public int getMaxTokensPerRobot() {
        return maxTokensPerRobot;
    }

    public void setMaxTokensPerRobot(int maxTokensPerRobot) {
        this.maxTokensPerRobot = maxTokensPerRobot;
    }

    public int getMaxHooksPerRobot() {
        return maxHooksPerRobot;
    }

    public void setMaxHooksPerRobot(int maxHooksPerRobot) {
        this.maxHooksPerRobot = maxHooksPerRobot;
    }

    public int getMaxMessagePushPerMinute() {
        return maxMessagePushPerMinute;
    }

    public void setMaxMessagePushPerMinute(int maxMessagePushPerMinute) {
        this.maxMessagePushPerMinute = maxMessagePushPerMinute;
    }

    public int getHookDispatchRetries() {
        return hookDispatchRetries;
    }

    public void setHookDispatchRetries(int hookDispatchRetries) {
        this.hookDispatchRetries = hookDispatchRetries;
    }

    public int getMessageDispatchRetries() {
        return messageDispatchRetries;
    }

    public void setMessageDispatchRetries(int messageDispatchRetries) {
        this.messageDispatchRetries = messageDispatchRetries;
    }

    public int getMessageIdempotencyHours() {
        return messageIdempotencyHours;
    }

    public void setMessageIdempotencyHours(int messageIdempotencyHours) {
        this.messageIdempotencyHours = messageIdempotencyHours;
    }
}
