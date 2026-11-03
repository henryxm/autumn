package cn.org.autumn.modules.bot.service;

/**
 * 机器人展示名硬拦 SPI（im / account 实现；未注入则放行）。
 * 词库 SSOT：account {@code risk_filter_word}。
 */
public interface BotDisplayNameRiskGuard {

    /**
     * @return 拒文案；null 表示放行或风控不可用（fail-open）
     */
    String blockMessageOrNull(String ownerUuid, String name);
}
