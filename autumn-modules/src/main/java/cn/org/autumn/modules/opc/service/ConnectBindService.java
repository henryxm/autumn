package cn.org.autumn.modules.opc.service;

import cn.org.autumn.base.ModuleService;
import cn.org.autumn.config.AccountHandler;
import cn.org.autumn.modules.opc.dao.ConnectBindDao;
import cn.org.autumn.opc.OpcConstants;
import cn.org.autumn.opl.model.OpenUserInfoSnapshot;
import cn.org.autumn.modules.opc.entity.ConnectAppEntity;
import cn.org.autumn.modules.opc.entity.ConnectBindEntity;
import cn.org.autumn.modules.sys.entity.SysUserEntity;
import cn.org.autumn.modules.sys.service.SysUserService;
import cn.org.autumn.modules.usr.dto.UserProfile;
import cn.org.autumn.modules.usr.entity.UserProfileEntity;
import cn.org.autumn.modules.usr.service.UserProfileService;
import cn.org.autumn.utils.Uuid;
import com.qiniu.util.Md5;
import java.util.Date;
import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConnectBindService extends ModuleService<ConnectBindDao, ConnectBindEntity> implements AccountHandler {

    @Autowired
    @Lazy
    private ConnectAppService connectAppService;

    @Autowired
    @Lazy
    private SysUserService sysUserService;

    @Autowired
    @Lazy
    private UserProfileService userProfileService;

    @Transactional(rollbackFor = Exception.class)
    public UserProfile resolveAndBind(ConnectAppEntity app, OpenUserInfoSnapshot userInfo) {
        if (app == null || userInfo == null || StringUtils.isBlank(userInfo.getOpenId())) {
            throw new IllegalArgumentException("用户信息无效");
        }
        ConnectBindEntity byOpenId = baseMapper.getByConnectAppAndOpenId(app.getUuid(), userInfo.getOpenId());
        if (byOpenId != null) {
            updateProfileFields(byOpenId, userInfo);
            return toUserProfile(byOpenId.getUser());
        }
        if (StringUtils.isNotBlank(userInfo.getUnionId())) {
            ConnectBindEntity byUnion = baseMapper.getByConnectAppAndUnionId(app.getUuid(), userInfo.getUnionId());
            if (byUnion != null) {
                byUnion.setOpenId(userInfo.getOpenId());
                byUnion.setUpdate(new Date());
                updateById(byUnion);
                updateProfileFields(byUnion, userInfo);
                return toUserProfile(byUnion.getUser());
            }
        }
        if (!connectAppService.isAutoRegisterEnabled()) {
            throw new IllegalStateException("该第三方账号尚未关联本地用户，请在「第三方登录接入管理」中添加关联，或开启自动注册");
        }
        SysUserEntity created = createLocalUser(userInfo);
        ConnectBindEntity bind = new ConnectBindEntity();
        bind.setConnectApp(app.getUuid());
        bind.setUser(created.getUuid());
        bind.setOpenId(userInfo.getOpenId());
        bind.setUnionId(userInfo.getUnionId());
        Date now = new Date();
        bind.setCreate(now);
        bind.setUpdate(now);
        try {
            insert(bind);
        } catch (DuplicateKeyException e) {
            ConnectBindEntity existing = baseMapper.getByConnectAppAndOpenId(app.getUuid(), userInfo.getOpenId());
            if (existing != null) {
                return toUserProfile(existing.getUser());
            }
            throw e;
        }
        return toUserProfile(created.getUuid());
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void canceled(Account obj) {
        if (obj == null || StringUtils.isBlank(obj.getUuid())) {
            return;
        }
        List<ConnectBindEntity> binds = baseMapper.selectList(new com.baomidou.mybatisplus.mapper.EntityWrapper<ConnectBindEntity>().eq("user", obj.getUuid()));
        if (binds == null || binds.isEmpty()) {
            return;
        }
        for (ConnectBindEntity bind : binds) {
            deleteById(bind.getId());
        }
    }

    private void updateProfileFields(ConnectBindEntity bind, OpenUserInfoSnapshot userInfo) {
        if (StringUtils.isNotBlank(userInfo.getUnionId()) && !StringUtils.equals(bind.getUnionId(), userInfo.getUnionId())) {
            bind.setUnionId(userInfo.getUnionId());
            bind.setUpdate(new Date());
            updateById(bind);
        }
        UserProfileEntity profile = userProfileService.getByUuid(bind.getUser());
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
        String username = OpcConstants.AUTO_REGISTER_USERNAME_PREFIX + Uuid.prefix(userInfo.getOpenId(), 12);
        String password = Md5.md5(Uuid.uuid().getBytes()).substring(0, 16);
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
}
