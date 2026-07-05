package cn.org.autumn.modules.opl.service;

import cn.org.autumn.base.ModuleService;
import cn.org.autumn.modules.opl.dao.OpenUnionDao;
import cn.org.autumn.modules.opl.entity.OpenUnionEntity;
import cn.org.autumn.opl.OplConstants;
import cn.org.autumn.opl.model.OpenPlatformEvent;
import cn.org.autumn.service.DistributedLockService;
import cn.org.autumn.utils.Uuid;
import com.qiniu.util.Md5;
import java.util.Date;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * unionId：开发者账号（account）下对平台用户唯一。
 */
@Service
public class OpenUnionService extends ModuleService<OpenUnionDao, OpenUnionEntity> {

    private static final String LOCK_PREFIX = "opl:union:";

    @Autowired
    private OpenAccountService openAccountService;

    @Autowired
    private DistributedLockService distributedLockService;

    @Autowired
    private OplExtensionService oplExtensionService;

    @Transactional(rollbackFor = Exception.class)
    public String getOrCreate(String accountUuid, String platformUserUuid) {
        Uuid.requireValid(accountUuid);
        Uuid.requireValid(platformUserUuid);
        openAccountService.requireActiveAccount(accountUuid);
        OpenUnionEntity existing = baseMapper.getByAccountAndUser(accountUuid, platformUserUuid);
        if (existing != null && StringUtils.isNotBlank(existing.getUnionId())) {
            return existing.getUnionId();
        }
        String lockKey = LOCK_PREFIX + accountUuid + ":" + platformUserUuid;
        return distributedLockService.withLockUnchecked(lockKey, () -> {
            OpenUnionEntity again = baseMapper.getByAccountAndUser(accountUuid, platformUserUuid);
            if (again != null && StringUtils.isNotBlank(again.getUnionId())) {
                return again.getUnionId();
            }
            OpenUnionEntity entity = new OpenUnionEntity();
            entity.setAccount(accountUuid);
            entity.setUser(platformUserUuid);
            String defaultUnionId = generateUnionId(accountUuid, platformUserUuid);
            entity.setUnionId(oplExtensionService.resolveUnionId(accountUuid, platformUserUuid, defaultUnionId));
            Date now = new Date();
            entity.setCreate(now);
            entity.setUpdate(now);
            insert(entity);
            OpenPlatformEvent event = OpenPlatformEvent.of(OplConstants.Event.UNION_CREATED);
            event.setAccount(accountUuid);
            event.setUser(platformUserUuid);
            event.setUnionId(entity.getUnionId());
            oplExtensionService.publish(event);
            return entity.getUnionId();
        });
    }

    public OpenUnionEntity getByUnionId(String unionId) {
        if (StringUtils.isBlank(unionId)) {
            return null;
        }
        return baseMapper.getByUnionId(unionId);
    }

    public OpenUnionEntity getByAccountAndUser(String accountUuid, String platformUserUuid) {
        if (StringUtils.isBlank(accountUuid) || StringUtils.isBlank(platformUserUuid)) {
            return null;
        }
        return baseMapper.getByAccountAndUser(accountUuid, platformUserUuid);
    }

    private String generateUnionId(String accountUuid, String platformUserUuid) {
        return "u_" + Uuid.norm(Md5.md5((accountUuid + ":" + platformUserUuid + ":union").getBytes()));
    }
}
