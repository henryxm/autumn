package cn.org.autumn.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

/**
 * 手机号 / 身份证永久禁用探针。业务仓（如 autumn-account）提供实现；
 * 实名模块（alipay）在发起/落库前通过本接口拦截。
 */
@Component
@ConditionalOnMissingBean(IdentityForbidHandler.class)
public interface IdentityForbidHandler {

    default boolean isPhoneForbidden(String phone) {
        return false;
    }

    default boolean isIdCardForbidden(String idCard) {
        return false;
    }
}
