package cn.org.autumn.model;

import cn.org.autumn.annotation.ConfigField;
import cn.org.autumn.annotation.ConfigParam;
import cn.org.autumn.config.InputType;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigParam(paramKey = ScanLoginConfig.CONFIG_KEY, category = ScanLoginConfig.config, name = "扫码登录配置", description = "QRC 票据 TTL、轮询与 Webhook 等全局策略")
public class ScanLoginConfig implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String CONFIG_KEY = "QRC_CONFIG";
    public static final String config = "qrc_config";

    @ConfigField(category = InputType.NumberType, name = "票据TTL秒", description = "扫码票据默认有效秒数")
    private int ticketTtlSeconds = 300;

    @ConfigField(category = InputType.NumberType, name = "交换TTL秒", description = "PC 一次性 exchangeToken 有效秒数")
    private int exchangeTokenTtlSeconds = 60;

    @ConfigField(category = InputType.NumberType, name = "轮询间隔毫秒", description = "文档建议的前端轮询间隔")
    private int pollIntervalMs = 2000;

    @ConfigField(category = InputType.NumberType, name = "Webhook超时毫秒", description = "Webhook 投递 HTTP 超时")
    private int webhookTimeoutMs = 5000;

    @ConfigField(category = InputType.NumberType, name = "每分钟轮询上限", description = "Open API 按 client 轮询频率上限，0 不限")
    private int maxPollPerMinute = 120;

    @ConfigField(category = InputType.BooleanType, name = "OAuth扫码优先", description = "authorize 未登录时优先展示扫码页")
    private boolean oauthQrFirst = true;

    public List<String> validateAndFix() {
        List<String> fixes = new ArrayList<>();
        if (ticketTtlSeconds < 60) {
            int old = ticketTtlSeconds;
            ticketTtlSeconds = 300;
            fixes.add(String.format("票据TTL不合理:%d，已修正为:%d", old, ticketTtlSeconds));
        }
        if (exchangeTokenTtlSeconds < 10) {
            int old = exchangeTokenTtlSeconds;
            exchangeTokenTtlSeconds = 60;
            fixes.add(String.format("交换TTL不合理:%d，已修正为:%d", old, exchangeTokenTtlSeconds));
        }
        if (pollIntervalMs < 500) {
            pollIntervalMs = 2000;
        }
        if (webhookTimeoutMs < 1000) {
            webhookTimeoutMs = 5000;
        }
        if (maxPollPerMinute < 0) {
            maxPollPerMinute = 120;
        }
        return fixes;
    }
}
