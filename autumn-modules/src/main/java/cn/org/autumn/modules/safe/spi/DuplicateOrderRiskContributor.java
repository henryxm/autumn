package cn.org.autumn.modules.safe.spi;

import cn.org.autumn.modules.safe.dto.PayGateAssessRequest;
import cn.org.autumn.modules.safe.service.PayGateAttemptService;
import cn.org.autumn.modules.safe.service.PayUserSecuritySettingService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * 短时同一订单号重复 assess 拒绝。
 */
@Component
@Order(100)
public class DuplicateOrderRiskContributor implements PayGateRiskContributor {

    @Autowired
    private PayGateAttemptService payGateAttemptService;

    @Override
    public List<String> evaluate(String userUuid, PayGateAssessRequest request, PayUserSecuritySettingService.UserSecurityEffective effective, String clientIp) {
        List<String> reasons = new ArrayList<>();
        if (request == null || StringUtils.isBlank(request.getOrderId()))
            return reasons;
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MINUTE, -5);
        int n = payGateAttemptService.countByOrderIdSince(userUuid, request.getOrderId(), cal.getTime());
        if (n > 0)
            reasons.add("订单正在处理中，请勿重复提交");
        return reasons;
    }
}
