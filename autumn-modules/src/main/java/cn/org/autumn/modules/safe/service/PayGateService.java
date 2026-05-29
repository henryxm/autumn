package cn.org.autumn.modules.safe.service;

import cn.org.autumn.config.GsonConfig;
import cn.org.autumn.exception.CodeException;
import cn.org.autumn.model.Error;
import cn.org.autumn.model.PayCredentialConfig;
import cn.org.autumn.modules.safe.dto.PayGateAssessRequest;
import cn.org.autumn.modules.safe.dto.PayGateAssessResult;
import cn.org.autumn.modules.safe.dto.PaySecurityStatusResult;
import cn.org.autumn.modules.safe.entity.PayGateAttemptEntity;
import cn.org.autumn.modules.safe.entity.PayUserPinEntity;
import cn.org.autumn.modules.safe.site.SafeConfig;
import cn.org.autumn.modules.safe.spi.PayGateRiskContributor;
import cn.org.autumn.modules.safe.support.PayCredentialVerifyMethods;
import cn.org.autumn.service.DistributedLockService;
import cn.org.autumn.utils.IPUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PayGateService {

    private static final String LOCK_KEY_PREFIX = "safe:gate:";

    @Autowired
    private SafeConfig safeConfig;

    @Autowired
    private PayUserSecuritySettingService payUserSecuritySettingService;

    @Autowired
    private PayUserPinService payUserPinService;

    @Autowired
    private PayUserTrustedDeviceService payUserTrustedDeviceService;

    @Autowired
    private PayUserTrustedIpService payUserTrustedIpService;

    @Autowired
    private PayGateAttemptService payGateAttemptService;

    @Autowired
    private PayGateTokenStore payGateTokenStore;

    @Autowired
    private DistributedLockService distributedLockService;

    @Autowired
    @Lazy
    private PayCredentialLogService payCredentialLogService;

    @Autowired(required = false)
    private List<PayGateRiskContributor> riskContributors;

    public PaySecurityStatusResult securityStatus(String userUuid) throws Exception {
        PayUserSecuritySettingService.UserSecurityEffective effective = payUserSecuritySettingService.resolveEffective(userUuid);
        PaySecurityStatusResult result = new PaySecurityStatusResult();
        result.setGateEnabled(effective.isGateEnabled());
        result.setPasswordlessEnabled(effective.isPasswordlessEnabled());
        result.setPasswordlessMaxAmountCent(effective.getPasswordlessMaxAmountCent());
        result.setPasswordlessWindowMinutes(effective.getPasswordlessWindowMinutes());
        result.setHighAmountThresholdCent(effective.getHighAmountThresholdCent());
        result.setGesturePaymentEnabled(effective.isGesturePaymentEnabled());
        result.setPasswordlessWindowActive(payGateTokenStore.isPasswordlessWindowActive(userUuid));
        result.setPasswordlessRemainingSeconds(payGateTokenStore.getPasswordlessRemainingSeconds(userUuid));
        result.setTrustedDevices(payUserTrustedDeviceService.listByUser(userUuid));
        result.setTrustedIps(payUserTrustedIpService.listByUser(userUuid));
        return result;
    }

    public PayGateAssessResult assess(String userUuid, PayGateAssessRequest request, HttpServletRequest servlet) throws Exception {
        return distributedLockService.withLock(LOCK_KEY_PREFIX + userUuid, () -> assessInternal(userUuid, request, servlet));
    }

    private PayGateAssessResult assessInternal(String userUuid, PayGateAssessRequest request, HttpServletRequest servlet) throws Exception {
        if (request == null || request.getAmountCent() <= 0)
            throw new CodeException(Error.DATA_FORMAT_ERROR);
        PayCredentialConfig global = safeConfig.get();
        PayUserSecuritySettingService.UserSecurityEffective effective = payUserSecuritySettingService.resolveEffective(userUuid);
        String clientIp = resolveClientIp(servlet);
        List<String> reasons = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        if (!effective.isGateEnabled()) {
            PayGateAssessResult bypass = finishAssess(userUuid, request, clientIp, true, PayGateAttemptEntity.AUTH_PASSWORD_REQUIRED, true, false, reasons, warnings, effective, servlet);
            bypass.setAllowedVerifyMethods(buildAllowedMethods(effective));
            return bypass;
        }

        PayUserPinEntity pin = payUserPinService.getByUserUuid(userUuid);
        if (pin != null && pin.getStatus() == PayUserPinEntity.STATUS_LOCKED) {
            reasons.add(Error.PAY_PIN_LOCKED.getMsg());
            return finishAssess(userUuid, request, clientIp, false, PayGateAttemptEntity.AUTH_DENIED, true, false, reasons, warnings, effective, servlet);
        }

        boolean trustedDevice = payUserTrustedDeviceService.isTrustedDevice(userUuid, request.getDeviceId());
        boolean trustedIp = payUserTrustedIpService.isTrustedIp(userUuid, clientIp);
        if (StringUtils.isNotBlank(request.getDeviceId()) && !trustedDevice)
            warnings.add("当前设备非常用支付设备");
        if (StringUtils.isNotBlank(clientIp) && !trustedIp)
            warnings.add("当前IP非常用支付网络");

        if (global.isNewDeviceRequirePassword() && StringUtils.isNotBlank(request.getDeviceId()) && !trustedDevice) {
            return finishAssess(userUuid, request, clientIp, true, PayGateAttemptEntity.AUTH_PASSWORD_REQUIRED, true, false, reasons, warnings, effective, servlet);
        }

        if (request.getAmountCent() > effective.getHighAmountThresholdCent()) {
            return finishAssess(userUuid, request, clientIp, true, PayGateAttemptEntity.AUTH_PASSWORD_REQUIRED, true, false, reasons, warnings, effective, servlet);
        }

        int dupWindow = global.getDuplicateAmountWindowMinutes() > 0 ? global.getDuplicateAmountWindowMinutes() : 10;
        int dupCount = global.getDuplicateAmountAlertCount() > 0 ? global.getDuplicateAmountAlertCount() : 2;
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MINUTE, -dupWindow);
        int sameAmountCount = payGateAttemptService.countAuthorizedSameAmountSince(userUuid, request.getAmountCent(), cal.getTime());
        if (sameAmountCount >= dupCount - 1) {
            warnings.add("短时间内存在相同金额的支付，请确认是否本人操作");
            return finishAssess(userUuid, request, clientIp, true, PayGateAttemptEntity.AUTH_PASSWORD_REQUIRED, true, false, reasons, warnings, effective, servlet);
        }

        if (riskContributors != null) {
            for (PayGateRiskContributor contributor : riskContributors) {
                if (contributor == null)
                    continue;
                List<String> extra = contributor.evaluate(userUuid, request, effective, clientIp);
                if (extra != null) {
                    for (String r : extra) {
                        if (StringUtils.isNotBlank(r))
                            reasons.add(r);
                    }
                }
            }
        }
        if (!reasons.isEmpty()) {
            return finishAssess(userUuid, request, clientIp, false, PayGateAttemptEntity.AUTH_DENIED, true, false, reasons, warnings, effective, servlet);
        }

        boolean passwordlessEligible = evaluatePasswordlessEligible(userUuid, request, global, effective, trustedDevice, trustedIp, warnings);
        if (passwordlessEligible) {
            return finishAssess(userUuid, request, clientIp, true, PayGateAttemptEntity.AUTH_PASSWORDLESS, false, true, reasons, warnings, effective, servlet);
        }
        return finishAssess(userUuid, request, clientIp, true, PayGateAttemptEntity.AUTH_PASSWORD_REQUIRED, true, false, reasons, warnings, effective, servlet);
    }

    private boolean evaluatePasswordlessEligible(String userUuid, PayGateAssessRequest request, PayCredentialConfig global, PayUserSecuritySettingService.UserSecurityEffective effective, boolean trustedDevice, boolean trustedIp, List<String> warnings) {
        if (!effective.isPasswordlessEnabled()) {
            warnings.add("未开启小额免密支付");
            return false;
        }
        if (request.getAmountCent() > effective.getPasswordlessMaxAmountCent()) {
            warnings.add("支付金额超过免密上限");
            return false;
        }
        if (!payGateTokenStore.isPasswordlessWindowActive(userUuid)) {
            warnings.add("免密支付窗口已过期，请校验支付密码");
            return false;
        }
        if (effective.isPasswordlessRequireTrustedDevice() && !trustedDevice) {
            warnings.add("免密支付需要常用设备");
            return false;
        }
        if (effective.isPasswordlessRequireTrustedIp() && !trustedIp) {
            warnings.add("免密支付需要常用IP");
            return false;
        }
        PayUserPinEntity pin = payUserPinService.getByUserUuid(userUuid);
        if (pin == null || pin.getStatus() == PayUserPinEntity.STATUS_UNSET) {
            warnings.add("请先设置支付密码");
            return false;
        }
        if (global.getPasswordlessDailyMaxCount() > 0) {
            int todayCount = payGateAttemptService.countPasswordlessSince(userUuid, payGateAttemptService.startOfToday());
            if (todayCount >= global.getPasswordlessDailyMaxCount()) {
                warnings.add("今日免密支付次数已达上限");
                return false;
            }
        }
        if (global.getPasswordlessDailyMaxAmountCent() > 0) {
            long todaySum = payGateAttemptService.sumPasswordlessAmountSince(userUuid, payGateAttemptService.startOfToday());
            if (todaySum + request.getAmountCent() > global.getPasswordlessDailyMaxAmountCent()) {
                warnings.add("今日免密支付金额已达上限");
                return false;
            }
        }
        return true;
    }

    private List<String> buildAllowedMethods(PayUserSecuritySettingService.UserSecurityEffective effective) {
        List<String> methods = new ArrayList<>();
        methods.add(PayCredentialVerifyMethods.PIN);
        methods.add(PayCredentialVerifyMethods.BIO);
        if (effective.isGesturePaymentEnabled())
            methods.add(PayCredentialVerifyMethods.GESTURE);
        return methods;
    }

    private PayGateAssessResult finishAssess(String userUuid, PayGateAssessRequest request, String clientIp, boolean authorized, String authMode, boolean needPassword, boolean passwordlessEligible, List<String> reasons, List<String> warnings, PayUserSecuritySettingService.UserSecurityEffective effective, HttpServletRequest servlet) {
        PayGateAssessResult result = new PayGateAssessResult();
        result.setAuthorized(authorized);
        result.setAuthMode(authMode);
        result.setNeedPassword(needPassword);
        result.setPasswordlessEligible(passwordlessEligible);
        result.setTrustedDevice(payUserTrustedDeviceService.isTrustedDevice(userUuid, request.getDeviceId()));
        result.setTrustedIp(payUserTrustedIpService.isTrustedIp(userUuid, clientIp));
        result.setEffectivePasswordlessMaxCent(effective.getPasswordlessMaxAmountCent());
        result.setEffectivePasswordlessWindowMinutes(effective.getPasswordlessWindowMinutes());
        result.setReasons(reasons);
        result.setWarnings(warnings);
        result.setAllowedVerifyMethods(buildAllowedMethods(effective));
        if (authorized) {
            String orderId = StringUtils.isBlank(request.getOrderId()) ? "" : request.getOrderId();
            result.setGateToken(payGateTokenStore.issueGateToken(userUuid, request.getAmountCent(), orderId, authMode));
            if (StringUtils.isNotBlank(request.getDeviceId()))
                payUserTrustedDeviceService.touchSuccess(userUuid, request.getDeviceId(), request.getPlatform());
            if (StringUtils.isNotBlank(clientIp))
                payUserTrustedIpService.touchSuccess(userUuid, clientIp, request.getLocation());
        }
        persistAttempt(userUuid, request, clientIp, authorized, authMode, reasons, warnings);
        payCredentialLogService.appendGateAssess(userUuid, authorized, authMode, request.getAmountCent(), request.getOrderId(), reasons, servlet);
        return result;
    }

    private void persistAttempt(String userUuid, PayGateAssessRequest request, String clientIp, boolean authorized, String authMode, List<String> reasons, List<String> warnings) {
        PayGateAttemptEntity entity = new PayGateAttemptEntity();
        entity.setUserUuid(userUuid);
        entity.setAmountCent(request.getAmountCent());
        entity.setCurrency(StringUtils.isBlank(request.getCurrency()) ? "CNY" : request.getCurrency());
        entity.setOrderId(request.getOrderId());
        entity.setMerchantId(request.getMerchantId());
        entity.setReason(request.getReason());
        entity.setDeviceId(request.getDeviceId());
        entity.setIp(clientIp);
        entity.setLocation(request.getLocation());
        entity.setAuthMode(authMode);
        entity.setAuthorized(authorized);
        Map<String, Object> detail = new HashMap<>();
        detail.put("reasons", reasons);
        detail.put("warnings", warnings);
        entity.setDetailJson(GsonConfig.getGson().toJson(detail));
        payGateAttemptService.record(entity);
    }

    private String resolveClientIp(HttpServletRequest servlet) {
        if (servlet == null)
            return null;
        try {
            return IPUtils.getIp(servlet);
        } catch (Exception ignored) {
            return null;
        }
    }
}
