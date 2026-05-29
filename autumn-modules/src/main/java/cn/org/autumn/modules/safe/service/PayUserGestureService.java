package cn.org.autumn.modules.safe.service;

import cn.org.autumn.base.ModuleService;
import cn.org.autumn.exception.CodeException;
import cn.org.autumn.model.Error;
import cn.org.autumn.model.PayCredentialConfig;
import cn.org.autumn.modules.safe.dao.PayUserGestureDao;
import cn.org.autumn.modules.safe.dto.PayGestureStatusResult;
import cn.org.autumn.modules.safe.dto.PayPinVerifyResult;
import cn.org.autumn.modules.safe.entity.PayGateAttemptEntity;
import cn.org.autumn.modules.safe.entity.PayUserGestureEntity;
import cn.org.autumn.modules.safe.site.SafeConfig;
import cn.org.autumn.modules.safe.spi.PayCredentialResetOrchestrator;
import cn.org.autumn.modules.safe.spi.PayResetContext;
import cn.org.autumn.modules.safe.support.PayCredentialSupport;
import cn.org.autumn.modules.safe.support.PayCredentialVerifyMethods;
import cn.org.autumn.utils.Uuid;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Calendar;
import java.util.Date;

@Service
public class PayUserGestureService extends ModuleService<PayUserGestureDao, PayUserGestureEntity> {

    private static final String LOCK_KEY_PREFIX = "safe:credential:gesture:";

    @Autowired
    private SafeConfig safeConfig;

    @Autowired
    private PayCredentialTokenStore payCredentialTokenStore;

    @Autowired
    @Lazy
    private PayCredentialLogService payCredentialLogService;

    @Autowired
    @Lazy
    private PayCredentialResetOrchestrator payCredentialResetOrchestrator;

    @Autowired
    @Lazy
    private PayGateTokenStore payGateTokenStore;

    @Autowired
    @Lazy
    private PayUserSecuritySettingService payUserSecuritySettingService;

    @Override
    public String ico() {
        return "fa-hand-pointer-o";
    }

    public PayUserGestureEntity getByUserUuid(String userUuid) {
        if (StringUtils.isBlank(userUuid))
            return null;
        PayUserGestureEntity cached = getCache(PayUserGestureEntity.class, "userUuid", userUuid);
        if (cached != null)
            return cached;
        return baseMapper.getByUserUuid(userUuid);
    }

    public PayGestureStatusResult status(String userUuid) {
        PayCredentialConfig config = safeConfig.get();
        PayUserGestureEntity entity = getByUserUuid(userUuid);
        PayGestureStatusResult result = new PayGestureStatusResult();
        if (entity == null || entity.getStatus() == PayUserGestureEntity.STATUS_UNSET) {
            result.setSet(false);
            result.setLocked(false);
            result.setRemainingAttempts(config.getMaxFailAttempts());
            return result;
        }
        refreshLockState(entity);
        result.setSet(true);
        result.setLocked(entity.getStatus() == PayUserGestureEntity.STATUS_LOCKED);
        result.setRemainingAttempts(Math.max(0, config.getMaxFailAttempts() - entity.getFailCount()));
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public void setGesture(String userUuid, int[] points, int[] confirm, HttpServletRequest request) throws Exception {
        withLock(LOCK_KEY_PREFIX + userUuid, () -> {
            PayCredentialConfig config = safeConfig.get();
            PayCredentialSupport.assertGestureConfirm(points, confirm, config);
            PayUserGestureEntity entity = getByUserUuid(userUuid);
            if (entity != null && entity.getStatus() != PayUserGestureEntity.STATUS_UNSET)
                throw new CodeException(Error.PAY_PIN_ALREADY_SET);
            saveGesture(userUuid, entity, points, config, request, "SET");
            return null;
        });
    }

    @Transactional(rollbackFor = Exception.class)
    public void changeGesture(String userUuid, int[] oldPoints, int[] newPoints, int[] confirm, HttpServletRequest request) throws Exception {
        withLock(LOCK_KEY_PREFIX + userUuid, () -> {
            PayCredentialConfig config = safeConfig.get();
            PayUserGestureEntity entity = requireNormal(userUuid);
            verifyGestureInternal(entity, oldPoints, config, request, "CHANGE");
            PayCredentialSupport.assertGestureConfirm(newPoints, confirm, config);
            saveGesture(userUuid, entity, newPoints, config, request, "CHANGE");
            return null;
        });
    }

    @Transactional(rollbackFor = Exception.class)
    public void resetGesture(String userUuid, int[] points, int[] confirm, String loginPassword, HttpServletRequest request) throws Exception {
        withLock(LOCK_KEY_PREFIX + userUuid, () -> {
            PayResetContext ctx = new PayResetContext();
            ctx.setUserUuid(userUuid);
            ctx.setLoginPassword(loginPassword);
            payCredentialResetOrchestrator.verify(ctx);
            PayCredentialConfig config = safeConfig.get();
            PayCredentialSupport.assertGestureConfirm(points, confirm, config);
            PayUserGestureEntity entity = getByUserUuid(userUuid);
            saveGesture(userUuid, entity, points, config, request, "RESET");
            return null;
        });
    }

    public PayPinVerifyResult verifyGesture(String userUuid, int[] points, String gateToken, long amountCent, HttpServletRequest request) throws Exception {
        PayCredentialConfig config = safeConfig.get();
        PayUserSecuritySettingService.UserSecurityEffective effective = payUserSecuritySettingService.resolveEffective(userUuid);
        boolean gateFlow = config.isGateEnabled() || StringUtils.isNotBlank(gateToken);
        if (gateFlow) {
            if (!effective.isGesturePaymentEnabled())
                throw new CodeException(Error.PAY_GESTURE_PAYMENT_DISABLED);
            if (StringUtils.isBlank(gateToken))
                throw new CodeException(Error.PAY_GATE_REQUIRED);
            PayGateTokenStore.GateEntry entry = payGateTokenStore.consumeGateToken(userUuid, gateToken, amountCent);
            if (PayGateAttemptEntity.AUTH_PASSWORDLESS.equals(entry.authMode))
                throw new CodeException(Error.OPERATION_NOT_ALLOWED);
            return doVerifyGestureSuccess(userUuid, points, config, request, amountCent, entry.orderId);
        }
        return doVerifyGestureSuccess(userUuid, points, config, request, amountCent, "");
    }

    private PayPinVerifyResult doVerifyGestureSuccess(String userUuid, int[] points, PayCredentialConfig config, HttpServletRequest request, long amountCent, String orderId) throws Exception {
        PayUserGestureEntity entity = requireNormal(userUuid);
        verifyGestureInternal(entity, points, config, request, "VERIFY");
        entity.setFailCount(0);
        entity.setLockedUntil(null);
        entity.setUpdateTime(new Date());
        updateById(entity);
        removeCache(PayUserGestureEntity.class, "userUuid", userUuid);
        PayUserSecuritySettingService.UserSecurityEffective effective = payUserSecuritySettingService.resolveEffective(userUuid);
        payGateTokenStore.markPasswordlessWindow(userUuid, effective.getPasswordlessWindowMinutes());
        PayPinVerifyResult result = new PayPinVerifyResult();
        result.setVerifyToken(payCredentialTokenStore.issueVerifyToken(userUuid, PayCredentialVerifyMethods.GESTURE, amountCent, orderId));
        payCredentialLogService.append(userUuid, "VERIFY", PayCredentialLogService.METHOD_GESTURE, true, null, request);
        return result;
    }

    private void saveGesture(String userUuid, PayUserGestureEntity entity, int[] points, PayCredentialConfig config, HttpServletRequest request, String action) throws CodeException {
        String canonical = PayCredentialSupport.canonicalizeGesture(points, config);
        Date now = new Date();
        if (entity == null) {
            entity = new PayUserGestureEntity();
            entity.setUuid(Uuid.uuid());
            entity.setUserUuid(userUuid);
            entity.setSetTime(now);
        }
        String salt = PayCredentialSupport.newSalt();
        entity.setSalt(salt);
        entity.setGestureHash(PayCredentialSupport.hashGesture(canonical, salt));
        entity.setStatus(PayUserGestureEntity.STATUS_NORMAL);
        entity.setFailCount(0);
        entity.setLockedUntil(null);
        entity.setUpdateTime(now);
        insertOrUpdate(entity);
        removeCache(PayUserGestureEntity.class, "userUuid", userUuid);
        payCredentialLogService.append(userUuid, action, PayCredentialLogService.METHOD_GESTURE, true, null, request);
    }

    private PayUserGestureEntity requireNormal(String userUuid) throws CodeException {
        PayUserGestureEntity entity = getByUserUuid(userUuid);
        if (entity == null || entity.getStatus() == PayUserGestureEntity.STATUS_UNSET)
            throw new CodeException(Error.PAY_PIN_NOT_SET);
        refreshLockState(entity);
        if (entity.getStatus() == PayUserGestureEntity.STATUS_LOCKED)
            throw new CodeException(Error.PAY_PIN_LOCKED);
        return entity;
    }

    private void verifyGestureInternal(PayUserGestureEntity entity, int[] points, PayCredentialConfig config, HttpServletRequest request, String action) throws CodeException {
        String canonical = PayCredentialSupport.canonicalizeGesture(points, config);
        String hashed = PayCredentialSupport.hashGesture(canonical, entity.getSalt());
        if (StringUtils.equals(hashed, entity.getGestureHash()))
            return;
        onVerifyFail(entity, config, request, action);
        throw new CodeException(Error.PAY_GESTURE_INVALID);
    }

    private void onVerifyFail(PayUserGestureEntity entity, PayCredentialConfig config, HttpServletRequest request, String action) {
        int max = config.getMaxFailAttempts() > 0 ? config.getMaxFailAttempts() : 5;
        entity.setFailCount(entity.getFailCount() + 1);
        if (entity.getFailCount() >= max) {
            entity.setStatus(PayUserGestureEntity.STATUS_LOCKED);
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.MINUTE, config.getLockMinutes() > 0 ? config.getLockMinutes() : 30);
            entity.setLockedUntil(cal.getTime());
            payCredentialLogService.append(entity.getUserUuid(), "LOCK", PayCredentialLogService.METHOD_GESTURE, false, null, request);
        }
        entity.setUpdateTime(new Date());
        updateById(entity);
        removeCache(PayUserGestureEntity.class, "userUuid", entity.getUserUuid());
        payCredentialLogService.append(entity.getUserUuid(), "VERIFY_FAIL", PayCredentialLogService.METHOD_GESTURE, false, action, request);
    }

    private void refreshLockState(PayUserGestureEntity entity) {
        if (entity.getStatus() != PayUserGestureEntity.STATUS_LOCKED)
            return;
        if (entity.getLockedUntil() != null && entity.getLockedUntil().before(new Date())) {
            entity.setStatus(PayUserGestureEntity.STATUS_NORMAL);
            entity.setFailCount(0);
            entity.setLockedUntil(null);
            entity.setUpdateTime(new Date());
            updateById(entity);
            removeCache(PayUserGestureEntity.class, "userUuid", entity.getUserUuid());
        }
    }
}
