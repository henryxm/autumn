package cn.org.autumn.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

/**
 * 机器人资料头像入审。多实现经 Factory 按 {@code @Order} 择优；调用方勿直接注入本类型。
 */
@Component
@ConditionalOnMissingBean(BotIconAuditHandler.class)
public interface BotIconAuditHandler {

    /**
     * 提交待审；失败抛异常。
     *
     * @return 审核单 uuid；默认 null 表示未入审
     */
    default String submitPending(String user, String robotUuid, String iconId, String icon) throws Exception {
        return null;
    }
}
