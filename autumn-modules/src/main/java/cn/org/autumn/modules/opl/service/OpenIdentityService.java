package cn.org.autumn.modules.opl.service;

import cn.org.autumn.base.ModuleService;
import cn.org.autumn.modules.opl.dao.OpenIdentityDao;
import cn.org.autumn.modules.opl.entity.OpenAppEntity;
import cn.org.autumn.modules.opl.entity.OpenIdentityEntity;
import cn.org.autumn.modules.opl.entity.OpenUnionEntity;
import cn.org.autumn.opl.OplConstants;
import cn.org.autumn.opl.model.OpenIdentitySnapshot;
import cn.org.autumn.opl.model.OpenPlatformEvent;
import cn.org.autumn.modules.opl.support.OplSnapshots;
import cn.org.autumn.utils.Uuid;
import com.qiniu.util.Md5;
import java.util.Date;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * openId：App（appId）下对平台用户唯一；unionId 由 {@link OpenUnionService} 维护。
 */
@Service
public class OpenIdentityService extends ModuleService<OpenIdentityDao, OpenIdentityEntity> {

    @Autowired
    private OpenAppService openAppService;

    @Autowired
    private OpenUnionService openUnionService;

    @Autowired
    private OplExtensionService oplExtensionService;

    @Transactional(rollbackFor = Exception.class)
    public OpenIdentitySnapshot resolveOrCreate(String appId, String userUuid) {
        Uuid.requireValid(userUuid);
        OpenAppEntity app = openAppService.requireActiveApp(appId);
        OpenIdentityEntity existing = baseMapper.getByAppIdAndUser(appId, userUuid);
        if (existing != null) {
            OpenIdentitySnapshot cached = toSnapshot(app.getAccount(), existing);
            publishIdentityResolved(cached);
            return cached;
        }
        String unionId = openUnionService.getOrCreate(app.getAccount(), userUuid);
        OpenIdentityEntity identity = new OpenIdentityEntity();
        identity.setAppId(appId);
        identity.setUser(userUuid);
        String defaultOpenId = generateOpenId(appId, userUuid);
        identity.setOpenId(oplExtensionService.resolveOpenId(OplSnapshots.toAppSnapshot(app), userUuid, defaultOpenId));
        Date now = new Date();
        identity.setCreate(now);
        identity.setUpdate(now);
        insert(identity);
        OpenIdentitySnapshot snapshot = toSnapshot(app.getAccount(), identity);
        snapshot.setUnionId(unionId);
        publishIdentityResolved(snapshot);
        return snapshot;
    }

    public OpenIdentitySnapshot getIdentity(String appId, String userUuid) {
        OpenAppEntity app = openAppService.getByAppId(appId);
        if (app == null) {
            return null;
        }
        OpenIdentityEntity identity = baseMapper.getByAppIdAndUser(appId, userUuid);
        if (identity == null) {
            return null;
        }
        return toSnapshot(app.getAccount(), identity);
    }

    public OpenIdentitySnapshot getIdentityByOpenId(String openId) {
        OpenIdentityEntity identity = getByOpenId(openId);
        if (identity == null) {
            return null;
        }
        OpenAppEntity app = openAppService.getByAppId(identity.getAppId());
        if (app == null) {
            return null;
        }
        return toSnapshot(app.getAccount(), identity);
    }

    public OpenIdentityEntity getByOpenId(String openId) {
        if (StringUtils.isBlank(openId)) {
            return null;
        }
        return baseMapper.getByOpenId(openId);
    }

    private void publishIdentityResolved(OpenIdentitySnapshot snapshot) {
        if (snapshot == null) {
            return;
        }
        OpenPlatformEvent event = OpenPlatformEvent.of(OplConstants.Event.IDENTITY_RESOLVED);
        event.setAppId(snapshot.getAppId());
        event.setAccount(snapshot.getAccount());
        event.setUser(snapshot.getUser());
        event.setOpenId(snapshot.getOpenId());
        event.setUnionId(snapshot.getUnionId());
        oplExtensionService.publish(event);
    }

    private OpenIdentitySnapshot toSnapshot(String accountUuid, OpenIdentityEntity entity) {
        OpenIdentitySnapshot snapshot = new OpenIdentitySnapshot();
        snapshot.setOpenId(entity.getOpenId());
        snapshot.setAppId(entity.getAppId());
        snapshot.setAccount(accountUuid);
        snapshot.setUser(entity.getUser());
        OpenUnionEntity union = openUnionService.getByAccountAndUser(accountUuid, entity.getUser());
        if (union != null) {
            snapshot.setUnionId(union.getUnionId());
        }
        return snapshot;
    }

    private String generateOpenId(String appId, String userUuid) {
        return "o_" + Uuid.norm(Md5.md5((appId + ":" + userUuid + ":open").getBytes()));
    }
}
