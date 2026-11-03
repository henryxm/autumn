package cn.org.autumn.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

/**
 * 机器人展示名硬拦。多实现经 Factory 按 {@code @Order} 择优；调用方勿直接注入本类型。
 */
@Component
@ConditionalOnMissingBean(BotDisplayNameRiskHandler.class)
public interface BotDisplayNameRiskHandler {

    /**
     * @return 拒文案；null 表示放行或风控不可用（fail-open）
     */
    default String blockMessageOrNull(String ownerUuid, String name) {
        return null;
    }
}
