package cn.org.autumn.modules.safe.service;

import cn.org.autumn.base.ModuleService;
import cn.org.autumn.model.PayCredentialConfig;
import cn.org.autumn.modules.safe.dao.PayUserSecuritySettingDao;
import cn.org.autumn.modules.safe.entity.PayUserSecuritySettingEntity;
import cn.org.autumn.modules.safe.site.SafeConfig;
import cn.org.autumn.utils.Uuid;
import java.util.Date;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PayUserSecuritySettingService extends ModuleService<PayUserSecuritySettingDao, PayUserSecuritySettingEntity> {

    @Autowired
    private SafeConfig safeConfig;

    @Override
    public String ico() {
        return "fa-cog";
    }

    public PayUserSecuritySettingEntity getByUserUuid(String userUuid) {
        if (StringUtils.isBlank(userUuid))
            return null;
        PayUserSecuritySettingEntity cached = getCache(PayUserSecuritySettingEntity.class, "userUuid", userUuid);
        if (cached != null)
            return cached;
        return baseMapper.getByUserUuid(userUuid);
    }

    public UserSecurityEffective resolveEffective(String userUuid) {
        PayCredentialConfig global = safeConfig.get();
        PayUserSecuritySettingEntity user = getByUserUuid(userUuid);
        UserSecurityEffective effective = new UserSecurityEffective();
        effective.setGateEnabled(global.isGateEnabled());
        effective.setPasswordlessEnabled(global.isPasswordlessEnabled());
        effective.setPasswordlessMaxAmountCent(global.getPasswordlessMaxAmountCent() > 0 ? global.getPasswordlessMaxAmountCent() : 1000L);
        effective.setPasswordlessWindowMinutes(global.getPasswordlessWindowMinutes() > 0 ? global.getPasswordlessWindowMinutes() : 15);
        effective.setHighAmountThresholdCent(global.getHighAmountThresholdCent() > 0 ? global.getHighAmountThresholdCent() : 50000L);
        effective.setPasswordlessRequireTrustedDevice(global.isPasswordlessRequireTrustedDevice());
        effective.setPasswordlessRequireTrustedIp(global.isPasswordlessRequireTrustedIp());
        effective.setGesturePaymentEnabled(false);
        if (user != null) {
            effective.setPasswordlessEnabled(user.isPasswordlessEnabled());
            if (user.getPasswordlessMaxAmountCent() > 0)
                effective.setPasswordlessMaxAmountCent(user.getPasswordlessMaxAmountCent());
            if (user.getPasswordlessWindowMinutes() > 0)
                effective.setPasswordlessWindowMinutes(user.getPasswordlessWindowMinutes());
            effective.setGesturePaymentEnabled(user.isGesturePaymentEnabled());
        }
        return effective;
    }

    @Transactional(rollbackFor = Exception.class)
    public PayUserSecuritySettingEntity saveUserSettings(String userUuid, Boolean passwordlessEnabled, Long passwordlessMaxAmountCent, Integer passwordlessWindowMinutes, Boolean gesturePaymentEnabled) {
        PayUserSecuritySettingEntity entity = getByUserUuid(userUuid);
        Date now = new Date();
        if (entity == null) {
            entity = new PayUserSecuritySettingEntity();
            entity.setUuid(Uuid.uuid());
            entity.setUserUuid(userUuid);
        }
        if (passwordlessEnabled != null)
            entity.setPasswordlessEnabled(passwordlessEnabled);
        if (passwordlessMaxAmountCent != null && passwordlessMaxAmountCent >= 0)
            entity.setPasswordlessMaxAmountCent(passwordlessMaxAmountCent);
        if (passwordlessWindowMinutes != null && passwordlessWindowMinutes >= 0)
            entity.setPasswordlessWindowMinutes(passwordlessWindowMinutes);
        if (gesturePaymentEnabled != null)
            entity.setGesturePaymentEnabled(gesturePaymentEnabled);
        entity.setUpdateTime(now);
        insertOrUpdate(entity);
        removeCache(PayUserSecuritySettingEntity.class, "userUuid", userUuid);
        return entity;
    }

    @Getter
    @Setter
    public static class UserSecurityEffective {
        private boolean gateEnabled = true;
        private boolean passwordlessEnabled = true;
        private long passwordlessMaxAmountCent = 1000L;
        private int passwordlessWindowMinutes = 15;
        private long highAmountThresholdCent = 50000L;
        private boolean passwordlessRequireTrustedDevice;
        private boolean passwordlessRequireTrustedIp;
        private boolean gesturePaymentEnabled;
    }
}
