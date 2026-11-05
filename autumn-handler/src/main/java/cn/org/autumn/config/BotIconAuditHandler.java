package cn.org.autumn.config;

/**
 * 机器人资料头像入审。多实现经 Factory 按 {@code @Order} 择优；调用方勿直接注入本类型。
 * <p>禁止在本接口上挂 {@code @Component} 空桩，否则 {@code hasIconAuditHandler()} 恒真却不入审。
 */
public interface BotIconAuditHandler {

    /**
     * 入审结果（含机审自动通过时的 pending=false）。
     */
    class SubmitOutcome {
        public String uuid;
        public int audit;
        public boolean pending = true;
        public String iconId;
        public String icon;
    }

    /**
     * 提交入审；失败抛异常。自动通过时业务表已写新头像。
     */
    default SubmitOutcome submit(String user, String robotUuid, String iconId, String icon) throws Exception {
        return submit(user, robotUuid, iconId, icon, false);
    }

    /**
     * @param forceSyncDetect true 时忽略配置中的异步 Detect（create 建号门禁须同步 BLOCK）
     */
    default SubmitOutcome submit(String user, String robotUuid, String iconId, String icon, boolean forceSyncDetect)
            throws Exception {
        String uuid = submitPending(user, robotUuid, iconId, icon);
        SubmitOutcome o = new SubmitOutcome();
        o.uuid = uuid;
        o.iconId = iconId;
        o.icon = icon;
        o.pending = true;
        o.audit = 0;
        return o;
    }

    /**
     * 提交待审；失败抛异常。
     *
     * @return 审核单 uuid；默认 null 表示未入审
     */
    default String submitPending(String user, String robotUuid, String iconId, String icon) throws Exception {
        return null;
    }
}
