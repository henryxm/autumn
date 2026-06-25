package cn.org.autumn.modules.safe.service;

import cn.org.autumn.base.ModuleService;
import cn.org.autumn.modules.safe.dao.PayUserBiometricDao;
import cn.org.autumn.modules.safe.dao.PayUserTrustedDeviceDao;
import cn.org.autumn.modules.safe.entity.PayUserBiometricEntity;
import cn.org.autumn.modules.safe.entity.PayUserTrustedDeviceEntity;
import cn.org.autumn.utils.Uuid;
import java.util.Date;
import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PayUserTrustedDeviceService extends ModuleService<PayUserTrustedDeviceDao, PayUserTrustedDeviceEntity> {

    @Autowired
    private PayUserBiometricDao payUserBiometricDao;

    @Override
    public String ico() {
        return "fa-mobile";
    }

    public boolean isTrustedDevice(String userUuid, String deviceId) {
        if (StringUtils.isBlank(userUuid) || StringUtils.isBlank(deviceId))
            return false;
        PayUserTrustedDeviceEntity trusted = baseMapper.getByUserAndDevice(userUuid, deviceId);
        if (trusted != null)
            return true;
        PayUserBiometricEntity bio = payUserBiometricDao.getByUserAndDevice(userUuid, deviceId);
        return bio != null && bio.getStatus() == PayUserBiometricEntity.STATUS_ACTIVE;
    }

    public List<PayUserTrustedDeviceEntity> listByUser(String userUuid) {
        return baseMapper.listByUser(userUuid);
    }

    @Transactional(rollbackFor = Exception.class)
    public void trust(String userUuid, String deviceId, String deviceName, String platform) {
        if (StringUtils.isBlank(userUuid) || StringUtils.isBlank(deviceId))
            return;
        PayUserTrustedDeviceEntity entity = baseMapper.getByUserAndDevice(userUuid, deviceId);
        Date now = new Date();
        if (entity == null) {
            entity = new PayUserTrustedDeviceEntity();
            entity.setUuid(Uuid.uuid());
            entity.setUserUuid(userUuid);
            entity.setDeviceId(deviceId);
            entity.setTrustTime(now);
            entity.setSuccessCount(0);
        }
        entity.setDeviceName(deviceName);
        entity.setPlatform(platform);
        entity.setLastUsedTime(now);
        insertOrUpdate(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    public void untrust(String userUuid, String deviceId) {
        if (StringUtils.isBlank(userUuid) || StringUtils.isBlank(deviceId))
            return;
        PayUserTrustedDeviceEntity entity = baseMapper.getByUserAndDevice(userUuid, deviceId);
        if (entity != null)
            deleteById(entity.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    public void touchSuccess(String userUuid, String deviceId, String platform) {
        if (StringUtils.isBlank(userUuid) || StringUtils.isBlank(deviceId))
            return;
        PayUserTrustedDeviceEntity entity = baseMapper.getByUserAndDevice(userUuid, deviceId);
        Date now = new Date();
        if (entity == null) {
            entity = new PayUserTrustedDeviceEntity();
            entity.setUuid(Uuid.uuid());
            entity.setUserUuid(userUuid);
            entity.setDeviceId(deviceId);
            entity.setPlatform(platform);
            entity.setTrustTime(now);
            entity.setSuccessCount(1);
        } else {
            entity.setSuccessCount(entity.getSuccessCount() + 1);
        }
        entity.setLastUsedTime(now);
        insertOrUpdate(entity);
    }
}
