package cn.org.autumn.modules.bot.service;

/**
 * 机器人资料头像入审 SPI（im / account 实现；未注入则跳过入审）。
 */
public interface BotIconAuditGuard {

    /**
     * 提交待审；失败抛异常（create 调用方 catch 后不回滚建号）。
     *
     * @return 审核单 uuid
     */
    String submitPending(String user, String robotUuid, String iconId, String icon) throws Exception;
}
