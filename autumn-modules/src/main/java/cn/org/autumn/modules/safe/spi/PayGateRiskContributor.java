package cn.org.autumn.modules.safe.spi;

import cn.org.autumn.modules.safe.dto.PayGateAssessRequest;
import cn.org.autumn.modules.safe.service.PayUserSecuritySettingService;

import java.util.List;

/**
 * 支付闸门扩展风险评估（业务仓可实现并注册为 Spring Bean）。
 *
 * @see docs/AI_SAFE_CREDENTIAL_INTEGRATION.md 扩展示例
 */
public interface PayGateRiskContributor {

    /**
     * @return 拒绝原因文案；空或 null 表示本扩展点通过
     */
    List<String> evaluate(String userUuid, PayGateAssessRequest request, PayUserSecuritySettingService.UserSecurityEffective effective, String clientIp);
}
