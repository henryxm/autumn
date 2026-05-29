package cn.org.autumn.modules.safe.service;

import cn.org.autumn.base.ModuleService;
import cn.org.autumn.modules.safe.dao.PayUserTrustedIpDao;
import cn.org.autumn.modules.safe.entity.PayUserTrustedIpEntity;
import cn.org.autumn.utils.Uuid;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
public class PayUserTrustedIpService extends ModuleService<PayUserTrustedIpDao, PayUserTrustedIpEntity> {

    @Override
    public String ico() {
        return "fa-globe";
    }

    public boolean isTrustedIp(String userUuid, String ip) {
        if (StringUtils.isBlank(userUuid) || StringUtils.isBlank(ip))
            return false;
        return baseMapper.getByUserAndIp(userUuid, ip) != null;
    }

    public List<PayUserTrustedIpEntity> listByUser(String userUuid) {
        return baseMapper.listByUser(userUuid);
    }

    @Transactional(rollbackFor = Exception.class)
    public void trust(String userUuid, String ip, String locationLabel) {
        if (StringUtils.isBlank(userUuid) || StringUtils.isBlank(ip))
            return;
        PayUserTrustedIpEntity entity = baseMapper.getByUserAndIp(userUuid, ip);
        Date now = new Date();
        if (entity == null) {
            entity = new PayUserTrustedIpEntity();
            entity.setUuid(Uuid.uuid());
            entity.setUserUuid(userUuid);
            entity.setIp(ip);
            entity.setTrustTime(now);
        }
        entity.setLocationLabel(locationLabel);
        entity.setLastUsedTime(now);
        insertOrUpdate(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    public void untrust(String userUuid, String ip) {
        if (StringUtils.isBlank(userUuid) || StringUtils.isBlank(ip))
            return;
        PayUserTrustedIpEntity entity = baseMapper.getByUserAndIp(userUuid, ip);
        if (entity != null)
            deleteById(entity.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    public void touchSuccess(String userUuid, String ip, String locationLabel) {
        if (StringUtils.isBlank(userUuid) || StringUtils.isBlank(ip))
            return;
        PayUserTrustedIpEntity entity = baseMapper.getByUserAndIp(userUuid, ip);
        Date now = new Date();
        if (entity == null) {
            entity = new PayUserTrustedIpEntity();
            entity.setUuid(Uuid.uuid());
            entity.setUserUuid(userUuid);
            entity.setIp(ip);
            entity.setLocationLabel(locationLabel);
            entity.setTrustTime(now);
        } else {
            entity.setLocationLabel(locationLabel);
        }
        entity.setLastUsedTime(now);
        insertOrUpdate(entity);
    }
}
