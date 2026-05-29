package cn.org.autumn.modules.safe.service;

import cn.org.autumn.base.ModuleService;
import cn.org.autumn.exception.CodeException;
import cn.org.autumn.model.Error;
import cn.org.autumn.model.PayCredentialConfig;
import cn.org.autumn.modules.safe.dao.PayUserPinDao;
import cn.org.autumn.modules.safe.dto.PayPinStatusResult;
import cn.org.autumn.modules.safe.dto.PayPinVerifyResult;
import cn.org.autumn.modules.safe.entity.PayGateAttemptEntity;
import cn.org.autumn.modules.safe.entity.PayUserPinEntity;
import cn.org.autumn.modules.safe.site.SafeConfig;
import cn.org.autumn.modules.safe.spi.PayCredentialResetOrchestrator;
import cn.org.autumn.modules.safe.spi.PayResetContext;
import cn.org.autumn.modules.safe.support.PayCredentialSupport;
import cn.org.autumn.modules.safe.support.PayCredentialVerifyMethods;
import cn.org.autumn.pay.PayPinVerifier;
import cn.org.autumn.utils.Uuid;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import java.util.Calendar;
import java.util.Date;

@Service
public class PayUserPinService extends ModuleService<PayUserPinDao, PayUserPinEntity> implements PayPinVerifier {

    private static final String LOCK_KEY_PREFIX = "safe:credential:pin:";

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
        return "fa-lock";
    }

    public PayUserPinEntity getByUserUuid(String userUuid) {
        if (StringUtils.isBlank(userUuid))
            return null;
        PayUserPinEntity cached = getCache(PayUserPinEntity.class, "userUuid", userUuid);
        if (cached != null)
            return cached;
        return baseMapper.getByUserUuid(userUuid);
    }

    public PayPinStatusResult status(String userUuid) throws Exception {
        PayCredentialConfig config = safeConfig.get();
        PayUserPinEntity entity = getByUserUuid(userUuid);
        PayPinStatusResult result = new PayPinStatusResult();
        if (entity == null || entity.getStatus() == PayUserPinEntity.STATUS_UNSET) {
            result.setSet(false);
            result.setLocked(false);
            result.setRemainingAttempts(config.getMaxFailAttempts());
            return result;
        }
        refreshLockState(entity, config);
        result.setSet(entity.getStatus() == PayUserPinEntity.STATUS_NORMAL || entity.getStatus() == PayUserPinEntity.STATUS_LOCKED);
        result.setLocked(entity.getStatus() == PayUserPinEntity.STATUS_LOCKED);
        result.setRemainingAttempts(Math.max(0, config.getMaxFailAttempts() - entity.getFailCount()));
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public void setPin(String userUuid, String pin, String confirm, HttpServletRequest request) throws Exception {
        withLock(LOCK_KEY_PREFIX + userUuid, () -> {
            PayCredentialConfig config = safeConfig.get();
            PayCredentialSupport.validatePinFormat(pin, config);
            PayCredentialSupport.assertPinConfirm(pin, confirm);
            PayUserPinEntity entity = getByUserUuid(userUuid);
            if (entity != null && entity.getStatus() != PayUserPinEntity.STATUS_UNSET)
                throw new CodeException(Error.PAY_PIN_ALREADY_SET);
            Date now = new Date();
            if (entity == null) {
                entity = new PayUserPinEntity();
                entity.setUuid(Uuid.uuid());
                entity.setUserUuid(userUuid);
            }
            String salt = PayCredentialSupport.newSalt();
            entity.setSalt(salt);
            entity.setPinHash(PayCredentialSupport.hashPin(pin, salt));
            entity.setStatus(PayUserPinEntity.STATUS_NORMAL);
            entity.setFailCount(0);
            entity.setLockedUntil(null);
            entity.setSetTime(now);
            entity.setUpdateTime(now);
            insertOrUpdate(entity);
            removeCache(PayUserPinEntity.class, "userUuid", userUuid);
            payCredentialLogService.append(userUuid, "SET", PayCredentialLogService.METHOD_PIN, true, null, request);
            return null;
        });
    }

    @Transactional(rollbackFor = Exception.class)
    public void changePin(String userUuid, String oldPin, String newPin, String confirm, HttpServletRequest request) throws Exception {
        withLock(LOCK_KEY_PREFIX + userUuid, () -> {
            PayCredentialConfig config = safeConfig.get();
            PayUserPinEntity entity = requireNormal(userUuid, config);
            verifyPinInternal(entity, oldPin, config, request, "CHANGE");
            PayCredentialSupport.validatePinFormat(newPin, config);
            PayCredentialSupport.assertPinConfirm(newPin, confirm);
            String salt = PayCredentialSupport.newSalt();
            entity.setSalt(salt);
            entity.setPinHash(PayCredentialSupport.hashPin(newPin, salt));
            entity.setFailCount(0);
            entity.setLockedUntil(null);
            entity.setStatus(PayUserPinEntity.STATUS_NORMAL);
            entity.setUpdateTime(new Date());
            updateById(entity);
            removeCache(PayUserPinEntity.class, "userUuid", userUuid);
            payCredentialLogService.append(userUuid, "CHANGE", PayCredentialLogService.METHOD_PIN, true, null, request);
            return null;
        });
    }

    @Transactional(rollbackFor = Exception.class)
    public void resetPin(String userUuid, String newPin, String confirm, String loginPassword, String smsCode, HttpServletRequest request) throws Exception {
        withLock(LOCK_KEY_PREFIX + userUuid, () -> {
            PayResetContext ctx = new PayResetContext();
            ctx.setUserUuid(userUuid);
            ctx.setLoginPassword(loginPassword);
            ctx.setSmsCode(smsCode);
            payCredentialResetOrchestrator.verify(ctx);
            PayCredentialConfig config = safeConfig.get();
            PayCredentialSupport.validatePinFormat(newPin, config);
            PayCredentialSupport.assertPinConfirm(newPin, confirm);
            PayUserPinEntity entity = getByUserUuid(userUuid);
            Date now = new Date();
            if (entity == null) {
                entity = new PayUserPinEntity();
                entity.setUuid(Uuid.uuid());
                entity.setUserUuid(userUuid);
                entity.setSetTime(now);
            }
            String salt = PayCredentialSupport.newSalt();
            entity.setSalt(salt);
            entity.setPinHash(PayCredentialSupport.hashPin(newPin, salt));
            entity.setStatus(PayUserPinEntity.STATUS_NORMAL);
            entity.setFailCount(0);
            entity.setLockedUntil(null);
            entity.setUpdateTime(now);
            insertOrUpdate(entity);
            removeCache(PayUserPinEntity.class, "userUuid", userUuid);
            payCredentialLogService.append(userUuid, "RESET", PayCredentialLogService.METHOD_PIN, true, null, request);
            return null;
        });
    }

    public PayPinVerifyResult verifyPin(String userUuid, String pin, String gateToken, long amountCent, HttpServletRequest request) throws Exception {
        PayCredentialConfig config = safeConfig.get();
        if (config.isGateEnabled()) {
            if (StringUtils.isBlank(gateToken))
                throw new CodeException(Error.PAY_GATE_REQUIRED);
            PayGateTokenStore.GateEntry entry = payGateTokenStore.consumeGateToken(userUuid, gateToken, amountCent);
            if (PayGateAttemptEntity.AUTH_PASSWORDLESS.equals(entry.authMode))
                throw new CodeException(Error.OPERATION_NOT_ALLOWED);
            PayPinVerifyResult result = doVerifyPinSuccess(userUuid, pin, config, request, amountCent, entry.orderId);
            return result;
        }
        return doVerifyPinSuccess(userUuid, pin, config, request, amountCent, "");
    }

    private PayPinVerifyResult doVerifyPinSuccess(String userUuid, String pin, PayCredentialConfig config, HttpServletRequest request, long amountCent, String orderId) throws Exception {
        PayUserPinEntity entity = requireNormal(userUuid, config);
        verifyPinInternal(entity, pin, config, request, "VERIFY");
        entity.setFailCount(0);
        entity.setLockedUntil(null);
        entity.setUpdateTime(new Date());
        updateById(entity);
        removeCache(PayUserPinEntity.class, "userUuid", userUuid);
        PayUserSecuritySettingService.UserSecurityEffective effective = payUserSecuritySettingService.resolveEffective(userUuid);
        payGateTokenStore.markPasswordlessWindow(userUuid, effective.getPasswordlessWindowMinutes());
        PayPinVerifyResult result = new PayPinVerifyResult();
        result.setVerifyToken(payCredentialTokenStore.issueVerifyToken(userUuid, PayCredentialVerifyMethods.PIN, amountCent, orderId));
        payCredentialLogService.append(userUuid, "VERIFY", PayCredentialLogService.METHOD_PIN, true, null, request);
        return result;
    }

    @Override
    public void requireVerified(String userUuid, String pin) throws Exception {
        PayCredentialConfig config = safeConfig.get();
        PayUserPinEntity entity = requireNormal(userUuid, config);
        verifyPinInternal(entity, pin, config, null, "REQUIRE");
    }

    @Override
    public void requireVerifyToken(String userUuid, String verifyToken) throws Exception {
        payCredentialTokenStore.consumeVerifyToken(userUuid, verifyToken);
    }

    @Override
    public void requireVerifyToken(String userUuid, String verifyToken, long amountCent, String orderId) throws Exception {
        payCredentialTokenStore.consumeVerifyToken(userUuid, verifyToken, amountCent, orderId);
    }

    @Override
    public void requireGateToken(String userUuid, String gateToken, long amountCent) throws Exception {
        PayCredentialConfig config = safeConfig.get();
        if (!config.isGateEnabled())
            return;
        if (StringUtils.isBlank(gateToken))
            throw new CodeException(Error.PAY_GATE_REQUIRED);
        payGateTokenStore.consumeGateToken(userUuid, gateToken, amountCent);
    }

    private PayUserPinEntity requireNormal(String userUuid, PayCredentialConfig config) throws CodeException {
        PayUserPinEntity entity = getByUserUuid(userUuid);
        if (entity == null || entity.getStatus() == PayUserPinEntity.STATUS_UNSET)
            throw new CodeException(Error.PAY_PIN_NOT_SET);
        refreshLockState(entity, config);
        if (entity.getStatus() == PayUserPinEntity.STATUS_LOCKED)
            throw new CodeException(Error.PAY_PIN_LOCKED);
        return entity;
    }

    private void verifyPinInternal(PayUserPinEntity entity, String pin, PayCredentialConfig config, HttpServletRequest request, String action) throws Exception {
        if (entity.getStatus() == PayUserPinEntity.STATUS_LOCKED)
            throw new CodeException(Error.PAY_PIN_LOCKED);
        PayCredentialSupport.validatePinFormat(pin, config);
        String hashed = PayCredentialSupport.hashPin(pin, entity.getSalt());
        if (StringUtils.equals(hashed, entity.getPinHash()))
            return;
        onVerifyFail(entity, config, request, action);
        throw new CodeException(Error.PAY_PIN_MISMATCH);
    }

    private void onVerifyFail(PayUserPinEntity entity, PayCredentialConfig config, HttpServletRequest request, String action) {
        int max = config.getMaxFailAttempts() > 0 ? config.getMaxFailAttempts() : 5;
        entity.setFailCount(entity.getFailCount() + 1);
        if (entity.getFailCount() >= max) {
            entity.setStatus(PayUserPinEntity.STATUS_LOCKED);
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.MINUTE, config.getLockMinutes() > 0 ? config.getLockMinutes() : 30);
            entity.setLockedUntil(cal.getTime());
            payCredentialLogService.append(entity.getUserUuid(), "LOCK", PayCredentialLogService.METHOD_PIN, false, null, request);
        }
        entity.setUpdateTime(new Date());
        updateById(entity);
        removeCache(PayUserPinEntity.class, "userUuid", entity.getUserUuid());
        payCredentialLogService.append(entity.getUserUuid(), "VERIFY_FAIL", PayCredentialLogService.METHOD_PIN, false, action, request);
    }

    private void refreshLockState(PayUserPinEntity entity, PayCredentialConfig config) {
        if (entity.getStatus() != PayUserPinEntity.STATUS_LOCKED)
            return;
        if (entity.getLockedUntil() != null && entity.getLockedUntil().before(new Date())) {
            entity.setStatus(PayUserPinEntity.STATUS_NORMAL);
            entity.setFailCount(0);
            entity.setLockedUntil(null);
            entity.setUpdateTime(new Date());
            updateById(entity);
            removeCache(PayUserPinEntity.class, "userUuid", entity.getUserUuid());
        }
    }
}
