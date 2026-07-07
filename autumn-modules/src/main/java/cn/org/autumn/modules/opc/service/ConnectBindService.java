package cn.org.autumn.modules.opc.service;

import cn.org.autumn.base.ModuleService;
import cn.org.autumn.config.AccountHandler;
import cn.org.autumn.modules.opc.dao.ConnectBindDao;
import cn.org.autumn.modules.opc.dto.ConnectBindResolveResult;
import cn.org.autumn.opc.OpcConstants;
import cn.org.autumn.opl.model.OpenUserInfoSnapshot;
import cn.org.autumn.modules.opc.entity.ConnectAppEntity;
import cn.org.autumn.modules.opc.entity.ConnectBindEntity;
import cn.org.autumn.modules.opc.support.ConnectBindException;
import cn.org.autumn.modules.opc.support.ConnectBindSupport;
import cn.org.autumn.modules.sys.entity.SysUserEntity;
import cn.org.autumn.modules.sys.service.SysUserService;
import cn.org.autumn.modules.sys.shiro.ShiroUtils;
import cn.org.autumn.modules.usr.dto.UserProfile;
import cn.org.autumn.modules.usr.entity.UserProfileEntity;
import cn.org.autumn.modules.usr.service.UserProfileService;
import cn.org.autumn.utils.Uuid;
import com.qiniu.util.Md5;
import java.util.Date;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConnectBindService extends ModuleService<ConnectBindDao, ConnectBindEntity> implements AccountHandler {

    @Autowired
    @Lazy
    private SysUserService sysUserService;

    @Autowired
    @Lazy
    private UserProfileService userProfileService;

    @Autowired
    private ConnectBindSupport connectBindSupport;

    @Transactional(rollbackFor = Exception.class)
    public ConnectBindResolveResult resolveAndBind(ConnectAppEntity app, OpenUserInfoSnapshot userInfo, String platformUser) {
        if (app == null || userInfo == null || StringUtils.isBlank(userInfo.getOpenId())) {
            throw ConnectBindException.invalidUserInfo(app);
        }
        String openId = userInfo.getOpenId().trim();
        String connectAppUuid = app.getUuid();

        ConnectBindEntity byOpenId = baseMapper.getByConnectAppAndOpenId(connectAppUuid, openId);
        if (byOpenId != null) {
            assertPlatformUserMatchesBind(app, openId, platformUser, byOpenId);
            updateProfileFields(byOpenId, userInfo);
            return ConnectBindResolveResult.of(toUserProfile(byOpenId.getUser()), true);
        }

        if (StringUtils.isNotBlank(userInfo.getUnionId())) {
            ConnectBindEntity byUnion = baseMapper.getByConnectAppAndUnionId(connectAppUuid, userInfo.getUnionId());
            if (byUnion != null) {
                assertPlatformUserMatchesBind(app, openId, platformUser, byUnion);
                byUnion.setOpenId(openId);
                byUnion.setUpdate(new Date());
                updateById(byUnion);
                updateProfileFields(byUnion, userInfo);
                return ConnectBindResolveResult.of(toUserProfile(byUnion.getUser()), true);
            }
        }

        if (StringUtils.isNotBlank(platformUser)) {
            SysUserEntity platform = sysUserService.getByUuid(platformUser.trim());
            if (platform != null) {
                ConnectBindEntity byUser = baseMapper.getByConnectAppAndUser(connectAppUuid, platform.getUuid());
                if (byUser != null && !connectBindSupport.isSameOpenId(byUser.getOpenId(), openId)) {
                    throw ConnectBindException.localAlreadyBound(app, openId, platform.getUuid(), byUser);
                }
                insertBind(connectAppUuid, platform.getUuid(), openId, userInfo.getUnionId());
                syncProfileFields(platform.getUuid(), userInfo);
                return ConnectBindResolveResult.of(toUserProfile(platform.getUuid()), false);
            }
        }

        String sessionUser = sessionUserUuid();
        if (StringUtils.isNotBlank(sessionUser)) {
            ConnectBindEntity byUser = baseMapper.getByConnectAppAndUser(connectAppUuid, sessionUser);
            if (byUser != null && !connectBindSupport.isSameOpenId(byUser.getOpenId(), openId)) {
                throw ConnectBindException.localAlreadyBound(app, openId, sessionUser, byUser);
            }
            insertBind(connectAppUuid, sessionUser, openId, userInfo.getUnionId());
            syncProfileFields(sessionUser, userInfo);
            return ConnectBindResolveResult.of(toUserProfile(sessionUser), false);
        }

        throw ConnectBindException.bindChoiceRequired(app, openId);
    }

    @Transactional(rollbackFor = Exception.class)
    public ConnectBindResolveResult bindCreateNewUser(ConnectAppEntity app, OpenUserInfoSnapshot userInfo) {
        if (app == null || userInfo == null || StringUtils.isBlank(userInfo.getOpenId())) {
            throw ConnectBindException.invalidUserInfo(app);
        }
        String openId = userInfo.getOpenId().trim();
        String connectAppUuid = app.getUuid();
        ConnectBindEntity existing = baseMapper.getByConnectAppAndOpenId(connectAppUuid, openId);
        if (existing != null) {
            return ConnectBindResolveResult.of(toUserProfile(existing.getUser()), false);
        }
        SysUserEntity created = createLocalUser(userInfo);
        insertBind(connectAppUuid, created.getUuid(), openId, userInfo.getUnionId());
        return ConnectBindResolveResult.of(toUserProfile(created.getUuid()), false);
    }

    @Transactional(rollbackFor = Exception.class)
    public ConnectBindResolveResult bindSessionUser(ConnectAppEntity app, OpenUserInfoSnapshot userInfo) {
        if (app == null || userInfo == null || StringUtils.isBlank(userInfo.getOpenId())) {
            throw ConnectBindException.invalidUserInfo(app);
        }
        if (!ShiroUtils.isLogin()) {
            throw new IllegalStateException("请先登录本地账号");
        }
        String sessionUser = ShiroUtils.getUserUuid();
        String openId = userInfo.getOpenId().trim();
        String connectAppUuid = app.getUuid();
        ConnectBindEntity byOpenId = baseMapper.getByConnectAppAndOpenId(connectAppUuid, openId);
        if (byOpenId != null) {
            if (!connectBindSupport.isSameUser(byOpenId.getUser(), sessionUser)) {
                throw ConnectBindException.upstreamBoundToOther(app, openId, sessionUser, byOpenId.getUser(), byOpenId);
            }
            updateProfileFields(byOpenId, userInfo);
            return ConnectBindResolveResult.of(toUserProfile(byOpenId.getUser()), true);
        }
        ConnectBindEntity byUser = baseMapper.getByConnectAppAndUser(connectAppUuid, sessionUser);
        if (byUser != null && !connectBindSupport.isSameOpenId(byUser.getOpenId(), openId)) {
            throw ConnectBindException.localAlreadyBound(app, openId, sessionUser, byUser);
        }
        insertBind(connectAppUuid, sessionUser, openId, userInfo.getUnionId());
        syncProfileFields(sessionUser, userInfo);
        return ConnectBindResolveResult.of(toUserProfile(sessionUser), false);
    }

    public ConnectBindEntity getByOpenId(String connectAppUuid, String openId) {
        if (StringUtils.isBlank(connectAppUuid) || StringUtils.isBlank(openId)) {
            return null;
        }
        return baseMapper.getByConnectAppAndOpenId(connectAppUuid, openId);
    }

    public ConnectBindEntity getByConnectAppAndUser(String connectAppUuid, String userUuid) {
        if (StringUtils.isBlank(connectAppUuid) || StringUtils.isBlank(userUuid)) {
            return null;
        }
        return baseMapper.getByConnectAppAndUser(connectAppUuid, userUuid);
    }

    public ConnectBindEntity getByUnionId(String connectAppUuid, String unionId) {
        if (StringUtils.isBlank(connectAppUuid) || StringUtils.isBlank(unionId)) {
            return null;
        }
        return baseMapper.getByConnectAppAndUnionId(connectAppUuid, unionId);
    }

    public String getLocalUserUuid(String connectAppUuid, String openId) {
        ConnectBindEntity bind = getByOpenId(connectAppUuid, openId);
        return bind == null ? null : bind.getUser();
    }

    @Transactional(rollbackFor = Exception.class)
    public void unbindForSessionUser(ConnectAppEntity app) {
        if (app == null || !ShiroUtils.isLogin()) {
            throw new IllegalStateException("请先登录");
        }
        ConnectBindEntity bind = baseMapper.getByConnectAppAndUser(app.getUuid(), ShiroUtils.getUserUuid());
        if (bind == null) {
            throw new IllegalStateException("当前账号未绑定该开放平台应用");
        }
        deleteById(bind.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void canceled(Account obj) {
        if (obj == null || StringUtils.isBlank(obj.getUuid())) {
            return;
        }
        List<ConnectBindEntity> binds = baseMapper.selectList(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<ConnectBindEntity>().eq("user", obj.getUuid()));
        if (binds == null || binds.isEmpty()) {
            return;
        }
        for (ConnectBindEntity bind : binds) {
            deleteById(bind.getId());
        }
    }

    private void assertPlatformUserMatchesBind(ConnectAppEntity app, String openId, String platformUser, ConnectBindEntity bind) {
        if (StringUtils.isBlank(platformUser) || bind == null) {
            return;
        }
        if (!connectBindSupport.isSameUser(bind.getUser(), platformUser)) {
            throw ConnectBindException.upstreamBoundToOther(app, openId, platformUser, bind.getUser(), bind);
        }
    }

    private void insertBind(String connectAppUuid, String localUserUuid, String openId, String unionId) {
        ConnectBindEntity bind = new ConnectBindEntity();
        bind.setUuid(Uuid.uuid());
        bind.setConnectApp(connectAppUuid);
        bind.setUser(localUserUuid);
        bind.setOpenId(openId);
        bind.setUnionId(unionId);
        Date now = new Date();
        bind.setCreate(now);
        bind.setUpdate(now);
        try {
            insert(bind);
        } catch (DuplicateKeyException e) {
            ConnectBindEntity existing = baseMapper.getByConnectAppAndOpenId(connectAppUuid, openId);
            if (existing != null) {
                return;
            }
            existing = baseMapper.getByConnectAppAndUser(connectAppUuid, localUserUuid);
            if (existing != null) {
                return;
            }
            throw e;
        }
    }

    private void updateProfileFields(ConnectBindEntity bind, OpenUserInfoSnapshot userInfo) {
        if (StringUtils.isNotBlank(userInfo.getUnionId()) && !StringUtils.equals(bind.getUnionId(), userInfo.getUnionId())) {
            bind.setUnionId(userInfo.getUnionId());
            bind.setUpdate(new Date());
            updateById(bind);
        }
        syncProfileFields(bind.getUser(), userInfo);
    }

    private void syncProfileFields(String localUserUuid, OpenUserInfoSnapshot userInfo) {
        UserProfileEntity profile = userProfileService.getByUuid(localUserUuid);
        if (profile == null) {
            return;
        }
        boolean changed = false;
        if (StringUtils.isNotBlank(userInfo.getNickname()) && !StringUtils.equals(profile.getNickname(), userInfo.getNickname())) {
            profile.setNickname(userInfo.getNickname());
            changed = true;
        }
        if (StringUtils.isNotBlank(userInfo.getIcon()) && !StringUtils.equals(profile.getIcon(), userInfo.getIcon())) {
            profile.setIcon(userInfo.getIcon());
            changed = true;
        }
        if (changed) {
            userProfileService.updateById(profile);
        }
    }

    private SysUserEntity createLocalUser(OpenUserInfoSnapshot userInfo) {
        String baseUsername = OpcConstants.AUTO_REGISTER_USERNAME_PREFIX + Uuid.prefix(userInfo.getOpenId(), 12);
        String password = Md5.md5(Uuid.uuid().getBytes()).substring(0, 16);
        IllegalArgumentException lastError = null;
        for (int attempt = 0; attempt < OpcConstants.AUTO_REGISTER_USERNAME_RETRY; attempt++) {
            String username = attempt == 0 ? baseUsername : baseUsername + "_" + attempt;
            try {
                SysUserEntity created = sysUserService.provisionConnectUser(username, password);
                UserProfileEntity profile = userProfileService.getByUuid(created.getUuid());
                if (profile != null) {
                    if (StringUtils.isNotBlank(userInfo.getNickname())) {
                        profile.setNickname(userInfo.getNickname());
                    }
                    if (StringUtils.isNotBlank(userInfo.getIcon())) {
                        profile.setIcon(userInfo.getIcon());
                    }
                    userProfileService.updateById(profile);
                }
                return created;
            } catch (IllegalArgumentException e) {
                lastError = e;
                if (!StringUtils.contains(e.getMessage(), "已被注册")) {
                    throw e;
                }
            }
        }
        throw lastError == null ? new IllegalStateException("创建 OPC 本地用户失败") : lastError;
    }

    private UserProfile toUserProfile(String userUuid) {
        UserProfileEntity profile = userProfileService.getByUuid(userUuid);
        if (profile == null) {
            SysUserEntity user = sysUserService.getByUuid(userUuid);
            profile = user == null ? null : userProfileService.from(user);
        }
        if (profile == null) {
            throw new IllegalStateException("本地用户不存在");
        }
        return UserProfile.from(profile);
    }

    /** 供单测 stub；生产环境读取 Shiro Session。 */
    String sessionUserUuid() {
        return ShiroUtils.isLogin() ? ShiroUtils.getUserUuid() : null;
    }
}
