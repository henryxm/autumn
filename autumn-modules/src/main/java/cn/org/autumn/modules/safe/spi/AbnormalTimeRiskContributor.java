package cn.org.autumn.modules.safe.spi;

import cn.org.autumn.model.PayCredentialConfig;
import cn.org.autumn.modules.safe.dto.PayGateAssessRequest;
import cn.org.autumn.modules.safe.service.PayUserSecuritySettingService;
import cn.org.autumn.modules.safe.site.SafeConfig;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 客户端时间与服务器偏差过大时拒绝 assess。
 */
@Component
@Order(200)
public class AbnormalTimeRiskContributor implements PayGateRiskContributor {

    @Autowired
    private SafeConfig safeConfig;

    @Override
    public List<String> evaluate(String userUuid, PayGateAssessRequest request, PayUserSecuritySettingService.UserSecurityEffective effective, String clientIp) {
        List<String> reasons = new ArrayList<>();
        if (request == null || request.getClientTime() == null || request.getClientTime() <= 0)
            return reasons;
        PayCredentialConfig config = safeConfig.get();
        int skew = config.getClientTimeSkewSeconds() > 0 ? config.getClientTimeSkewSeconds() : 300;
        long diff = Math.abs(System.currentTimeMillis() - request.getClientTime());
        if (diff > skew * 1000L)
            reasons.add("设备时间异常，请校准系统时间后重试");
        return reasons;
    }
}
