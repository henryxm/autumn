package cn.org.autumn.modules.client.service;

import cn.org.autumn.base.ModuleService;
import cn.org.autumn.config.AccountHandler;
import cn.org.autumn.modules.client.dao.WebOauthBindDao;
import cn.org.autumn.modules.client.dto.WebOauthBindResolveResult;
import cn.org.autumn.modules.client.entity.WebAuthenticationEntity;
import cn.org.autumn.modules.client.entity.WebOauthBindEntity;
import cn.org.autumn.modules.client.oauth2.WebOauthBindException;
import cn.org.autumn.modules.client.oauth2.WebOauthBindSupport;
import cn.org.autumn.modules.client.site.ClientConstants;
import cn.org.autumn.modules.sys.entity.SysUserEntity;
import cn.org.autumn.modules.sys.service.SysUserService;
import cn.org.autumn.modules.sys.shiro.ShiroUtils;
import cn.org.autumn.modules.usr.dto.UserProfile;
import cn.org.autumn.modules.usr.entity.UserProfileEntity;
import cn.org.autumn.modules.usr.service.UserProfileService;
import cn.org.autumn.utils.Uuid;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.qiniu.util.Md5;
import java.util.Date;
import java.util.List;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WebOauthBindService extends ModuleService<WebOauthBindDao, WebOauthBindEntity> implements AccountHandler {

    @Autowired
    @Lazy
    private SysUserService sysUserService;

    @Autowired
    @Lazy
    private UserProfileService userProfileService;

    @Autowired
    private WebOauthBindSupport webOauthBindSupport;

    @Transactional(rollbackFor = Exception.class)
    public WebOauthBindResolveResult resolveAndBind(WebAuthenticationEntity webAuth, UserProfile upstream, HttpServletRequest request) {
        if (webAuth == null || upstream == null || StringUtils.isBlank(upstream.getUuid())) {
            throw WebOauthBindException.invalidUpstream(webAuth);
        }
        String upstreamUuid = upstream.getUuid().trim();
        String sessionUser = sessionUserUuid();
        String webAuthUuid = webAuth.getUuid();

        if (webOauthBindSupport.isSameInstance(webAuth, request) && sysUserService.getByUuid(upstreamUuid) != null) {
            return resolveSameInstanceIdempotent(webAuth, upstream, webAuthUuid, upstreamUuid, upstreamUuid);
        }

        WebOauthBindEntity byUpstream = baseMapper.getByAuthenticationAndUpper(webAuthUuid, upstreamUuid);
        if (byUpstream != null) {
            return resolveExistingUpstreamBind(webAuth, upstream, byUpstream, sessionUser);
        }

        if (StringUtils.isNotBlank(sessionUser)) {
            WebOauthBindEntity byUser = baseMapper.getByAuthenticationAndUser(webAuthUuid, sessionUser);
            if (byUser != null && !webOauthBindSupport.isSameUser(byUser.getUpper(), upstreamUuid)) {
                throw WebOauthBindException.localAlreadyBound(webAuth, upstreamUuid, sessionUser, byUser);
            }
            insertBind(webAuthUuid, upstreamUuid, sessionUser);
            syncProfileFields(sessionUser, upstream);
            return WebOauthBindResolveResult.of(toUserProfile(sessionUser), false);
        }

        SysUserEntity legacy = sysUserService.getByUuid(upstreamUuid);
        if (legacy != null) {
            insertBind(webAuthUuid, upstreamUuid, upstreamUuid);
            syncProfileFields(upstreamUuid, upstream);
            return WebOauthBindResolveResult.of(toUserProfile(upstreamUuid), false);
        }

        SysUserEntity created = createLocalUser(upstream);
        insertBind(webAuthUuid, upstreamUuid, created.getUuid());
        return WebOauthBindResolveResult.of(toUserProfile(created.getUuid()), false);
    }

    @Transactional(rollbackFor = Exception.class)
    public void unbindForSessionUser(WebAuthenticationEntity webAuth) {
        if (webAuth == null || !ShiroUtils.isLogin()) {
            throw new IllegalStateException("请先登录");
        }
        WebOauthBindEntity bind = baseMapper.getByAuthenticationAndUser(webAuth.getUuid(), ShiroUtils.getUserUuid());
        if (bind == null) {
            throw new IllegalStateException("当前账号未绑定该 OAuth 客户端");
        }
        deleteById(bind.getId());
    }

    public WebOauthBindEntity getByAuthenticationAndUser(String authenticationUuid, String userUuid) {
        if (StringUtils.isBlank(authenticationUuid) || StringUtils.isBlank(userUuid)) {
            return null;
        }
        return baseMapper.getByAuthenticationAndUser(authenticationUuid, userUuid);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void canceled(Account obj) {
        if (obj == null || StringUtils.isBlank(obj.getUuid())) {
            return;
        }
        List<WebOauthBindEntity> binds = baseMapper.selectList(new EntityWrapper<WebOauthBindEntity>().eq("user", obj.getUuid()));
        if (binds == null || binds.isEmpty()) {
            return;
        }
        for (WebOauthBindEntity bind : binds) {
            deleteById(bind.getId());
        }
    }

    private WebOauthBindResolveResult resolveSameInstanceIdempotent(WebAuthenticationEntity webAuth, UserProfile upstream, String webAuthUuid, String upstreamUuid, String localUser) {
        WebOauthBindEntity byUpstream = baseMapper.getByAuthenticationAndUpper(webAuthUuid, upstreamUuid);
        if (byUpstream != null && !webOauthBindSupport.isSameUser(byUpstream.getUser(), localUser)) {
            throw WebOauthBindException.upstreamBoundToOther(webAuth, upstreamUuid, localUser, byUpstream.getUser(), byUpstream);
        }
        if (byUpstream == null) {
            insertBind(webAuthUuid, upstreamUuid, localUser);
        }
        syncProfileFields(localUser, upstream);
        return WebOauthBindResolveResult.of(toUserProfile(localUser), true);
    }

    private WebOauthBindResolveResult resolveExistingUpstreamBind(WebAuthenticationEntity webAuth, UserProfile upstream, WebOauthBindEntity bind, String sessionUser) {
        syncProfileFields(bind.getUser(), upstream);
        if (StringUtils.isBlank(sessionUser)) {
            return WebOauthBindResolveResult.of(toUserProfile(bind.getUser()), false);
        }
        if (webOauthBindSupport.isSameUser(bind.getUser(), sessionUser)) {
            return WebOauthBindResolveResult.of(toUserProfile(bind.getUser()), true);
        }
        throw WebOauthBindException.upstreamBoundToOther(webAuth, upstream.getUuid(), sessionUser, bind.getUser(), bind);
    }

    private void insertBind(String webAuthUuid, String upstreamUuid, String localUserUuid) {
        WebOauthBindEntity bind = new WebOauthBindEntity();
        bind.setUser(localUserUuid);
        bind.setAuthentication(webAuthUuid);
        bind.setUpper(upstreamUuid);
        Date now = new Date();
        bind.setCreate(now);
        bind.setUpdate(now);
        try {
            insert(bind);
        } catch (DuplicateKeyException e) {
            WebOauthBindEntity existing = baseMapper.getByAuthenticationAndUpper(webAuthUuid, upstreamUuid);
            if (existing != null) {
                return;
            }
            existing = baseMapper.getByAuthenticationAndUser(webAuthUuid, localUserUuid);
            if (existing != null) {
                return;
            }
            throw e;
        }
    }

    private void syncProfileFields(String localUserUuid, UserProfile upstream) {
        UserProfileEntity profile = userProfileService.getByUuid(localUserUuid);
        if (profile == null) {
            return;
        }
        boolean changed = false;
        if (StringUtils.isNotBlank(upstream.getNickname()) && !StringUtils.equals(profile.getNickname(), upstream.getNickname())) {
            profile.setNickname(upstream.getNickname());
            changed = true;
        }
        if (StringUtils.isNotBlank(upstream.getIcon()) && !StringUtils.equals(profile.getIcon(), upstream.getIcon())) {
            profile.setIcon(upstream.getIcon());
            changed = true;
        }
        if (changed) {
            userProfileService.updateById(profile);
        }
    }

    private SysUserEntity createLocalUser(UserProfile upstream) {
        String baseUsername = ClientConstants.OAUTH_AUTO_REGISTER_USERNAME_PREFIX + Uuid.prefix(upstream.getUuid(), 12);
        String password = Md5.md5(Uuid.uuid().getBytes()).substring(0, 16);
        IllegalArgumentException lastError = null;
        for (int attempt = 0; attempt < ClientConstants.OAUTH_AUTO_REGISTER_USERNAME_RETRY; attempt++) {
            String username = attempt == 0 ? baseUsername : baseUsername + "_" + attempt;
            try {
                SysUserEntity created = sysUserService.provisionConnectUser(username, password);
                UserProfileEntity profile = userProfileService.getByUuid(created.getUuid());
                if (profile != null) {
                    if (StringUtils.isNotBlank(upstream.getNickname())) {
                        profile.setNickname(upstream.getNickname());
                    }
                    if (StringUtils.isNotBlank(upstream.getIcon())) {
                        profile.setIcon(upstream.getIcon());
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
        throw lastError == null ? new IllegalStateException("创建 OAuth 本地用户失败") : lastError;
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
