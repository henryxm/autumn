package cn.org.autumn.modules.safe.service;

import cn.org.autumn.base.ModuleService;
import cn.org.autumn.exception.CodeException;
import cn.org.autumn.model.Error;
import cn.org.autumn.model.PayCredentialConfig;
import cn.org.autumn.modules.safe.dao.PayUserBiometricDao;
import cn.org.autumn.modules.safe.dto.PayBiometricChallengeResult;
import cn.org.autumn.modules.safe.dto.PayBiometricDeviceView;
import cn.org.autumn.modules.safe.dto.PayPinVerifyResult;
import cn.org.autumn.modules.safe.entity.PayGateAttemptEntity;
import cn.org.autumn.modules.safe.entity.PayUserBiometricEntity;
import cn.org.autumn.modules.safe.site.SafeConfig;
import cn.org.autumn.modules.safe.support.PayBiometricSignatureSupport;
import cn.org.autumn.modules.safe.support.PayCredentialVerifyMethods;
import cn.org.autumn.utils.Uuid;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PayUserBiometricService extends ModuleService<PayUserBiometricDao, PayUserBiometricEntity> {

    private static final String LOCK_KEY_PREFIX = "safe:credential:bio:";

    @Autowired
    private SafeConfig safeConfig;

    @Autowired
    private PayCredentialTokenStore payCredentialTokenStore;

    @Autowired
    @Lazy
    private PayCredentialLogService payCredentialLogService;

    @Autowired
    @Lazy
    private PayGateTokenStore payGateTokenStore;

    @Autowired
    @Lazy
    private PayUserSecuritySettingService payUserSecuritySettingService;

    @Override
    public String ico() {
        return "fa-mobile";
    }

    @Transactional(rollbackFor = Exception.class)
    public void register(String userUuid, String deviceId, String platform, String credentialId, String publicKey, HttpServletRequest request) throws Exception {
        withLock(LOCK_KEY_PREFIX + userUuid, () -> {
            if (StringUtils.isBlank(deviceId) || StringUtils.isBlank(publicKey))
                throw new CodeException(Error.DATA_FORMAT_ERROR);
            PayCredentialConfig config = safeConfig.get();
            PayUserBiometricEntity existing = baseMapper.getByUserAndDevice(userUuid, deviceId);
            Date now = new Date();
            if (existing != null && existing.getStatus() == PayUserBiometricEntity.STATUS_ACTIVE) {
                existing.setPlatform(platform);
                existing.setCredentialId(credentialId);
                existing.setPublicKey(publicKey.trim());
                existing.setUpdateTime(now);
                updateById(existing);
                payCredentialLogService.append(userUuid, "REGISTER", PayCredentialLogService.METHOD_BIO, true, deviceId, request);
                return null;
            }
            if (existing == null) {
                int max = config.getMaxBiometricDevices();
                if (max > 0 && baseMapper.countActiveByUser(userUuid) >= max)
                    throw new CodeException(Error.OPERATION_NOT_ALLOWED);
                existing = new PayUserBiometricEntity();
                existing.setUuid(Uuid.uuid());
                existing.setUserUuid(userUuid);
                existing.setDeviceId(deviceId);
                existing.setCreateTime(now);
            }
            existing.setPlatform(platform);
            existing.setCredentialId(credentialId);
            existing.setPublicKey(publicKey.trim());
            existing.setStatus(PayUserBiometricEntity.STATUS_ACTIVE);
            existing.setUpdateTime(now);
            insertOrUpdate(existing);
            payCredentialLogService.append(userUuid, "REGISTER", PayCredentialLogService.METHOD_BIO, true, deviceId, request);
            return null;
        });
    }

    public List<PayBiometricDeviceView> listDevices(String userUuid) {
        List<PayUserBiometricEntity> list = baseMapper.listActiveByUser(userUuid);
        List<PayBiometricDeviceView> views = new ArrayList<>();
        if (list == null)
            return views;
        for (PayUserBiometricEntity entity : list)
            views.add(PayBiometricDeviceView.of(entity));
        return views;
    }

    @Transactional(rollbackFor = Exception.class)
    public void revoke(String userUuid, String deviceId, HttpServletRequest request) throws Exception {
        withLock(LOCK_KEY_PREFIX + userUuid, () -> {
            if (StringUtils.isBlank(deviceId))
                throw new CodeException(Error.DATA_FORMAT_ERROR);
            baseMapper.revokeByUserAndDevice(userUuid, deviceId, new Date());
            payCredentialLogService.append(userUuid, "REVOKE", PayCredentialLogService.METHOD_BIO, true, deviceId, request);
            return null;
        });
    }

    public PayBiometricChallengeResult challenge(String userUuid, String deviceId) throws CodeException {
        PayUserBiometricEntity entity = requireActive(userUuid, deviceId);
        String challenge = payCredentialTokenStore.issueChallenge(userUuid, deviceId);
        PayBiometricChallengeResult result = new PayBiometricChallengeResult();
        result.setDeviceId(entity.getDeviceId());
        result.setChallenge(challenge);
        return result;
    }

    public PayPinVerifyResult verify(String userUuid, String deviceId, String challenge, String signature, String gateToken, long amountCent, HttpServletRequest request) throws Exception {
        PayCredentialConfig config = safeConfig.get();
        String orderId = "";
        if (config.isGateEnabled()) {
            if (StringUtils.isBlank(gateToken))
                throw new CodeException(Error.PAY_GATE_REQUIRED);
            PayGateTokenStore.GateEntry entry = payGateTokenStore.consumeGateToken(userUuid, gateToken, amountCent);
            if (PayGateAttemptEntity.AUTH_PASSWORDLESS.equals(entry.authMode))
                throw new CodeException(Error.OPERATION_NOT_ALLOWED);
            orderId = entry.orderId == null ? "" : entry.orderId;
        }
        PayUserBiometricEntity entity = requireActive(userUuid, deviceId);
        payCredentialTokenStore.requireChallenge(userUuid, deviceId, challenge);
        if (!PayBiometricSignatureSupport.verifySha256Rsa(entity.getPublicKey(), challenge, signature))
            throw new CodeException(Error.PAY_BIOMETRIC_VERIFY_FAILED);
        entity.setLastUsedTime(new Date());
        updateById(entity);
        PayUserSecuritySettingService.UserSecurityEffective effective = payUserSecuritySettingService.resolveEffective(userUuid);
        payGateTokenStore.markPasswordlessWindow(userUuid, effective.getPasswordlessWindowMinutes());
        PayPinVerifyResult result = new PayPinVerifyResult();
        result.setVerifyToken(payCredentialTokenStore.issueVerifyToken(userUuid, PayCredentialVerifyMethods.BIO, amountCent, orderId));
        payCredentialLogService.append(userUuid, "VERIFY", PayCredentialLogService.METHOD_BIO, true, deviceId, request);
        return result;
    }

    private PayUserBiometricEntity requireActive(String userUuid, String deviceId) throws CodeException {
        PayUserBiometricEntity entity = baseMapper.getByUserAndDevice(userUuid, deviceId);
        if (entity == null || entity.getStatus() != PayUserBiometricEntity.STATUS_ACTIVE)
            throw new CodeException(Error.PAY_BIOMETRIC_NOT_FOUND);
        return entity;
    }
}
