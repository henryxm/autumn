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
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
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

/** OAuth uuid 绑定：已登录直接绑定；跨实例未登录走绑定选择页；同实例幂等。 */
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

    /**
     * 解析上游 userInfo 与本地账号关系并写入绑定。
     * <ol>
     *   <li>上游 uuid 已有绑定 → 登录对应本地用户</li>
     *   <li>当前 Session 已登录 → 绑定到 Session 用户</li>
     *   <li>同实例且本地已有同 uuid 用户 → 幂等绑定</li>
     *   <li>跨实例且未登录 → 抛出 {@code BIND_CHOICE_REQUIRED}</li>
     * </ol>
     */
    @Transactional(rollbackFor = Exception.class)
    public WebOauthBindResolveResult resolveAndBind(WebAuthenticationEntity webAuth, UserProfile upstream, HttpServletRequest request) {
        if (webAuth == null || upstream == null || StringUtils.isBlank(upstream.getUuid())) {
            throw WebOauthBindException.invalidUpstream(webAuth);
        }
        String upstreamUuid = upstream.getUuid().trim();
        String sessionUser = sessionUserUuid();
        String webAuthUuid = webAuth.getUuid();

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

        if (webOauthBindSupport.isSameInstance(webAuth, request) && sysUserService.getByUuid(upstreamUuid) != null) {
            return resolveSameInstanceIdempotent(webAuth, upstream, webAuthUuid, upstreamUuid, upstreamUuid);
        }

        throw WebOauthBindException.bindChoiceRequired(webAuth, upstreamUuid);
    }

    @Transactional(rollbackFor = Exception.class)
    public WebOauthBindResolveResult bindCreateNewUser(WebAuthenticationEntity webAuth, UserProfile upstream) {
        if (webAuth == null || upstream == null || StringUtils.isBlank(upstream.getUuid())) {
            throw WebOauthBindException.invalidUpstream(webAuth);
        }
        String upstreamUuid = upstream.getUuid().trim();
        String webAuthUuid = webAuth.getUuid();
        WebOauthBindEntity existing = baseMapper.getByAuthenticationAndUpper(webAuthUuid, upstreamUuid);
        if (existing != null) {
            return WebOauthBindResolveResult.of(toUserProfile(existing.getUser()), false);
        }
        SysUserEntity created = createLocalUser(upstream);
        insertBind(webAuthUuid, upstreamUuid, created.getUuid());
        return WebOauthBindResolveResult.of(toUserProfile(created.getUuid()), false);
    }

    @Transactional(rollbackFor = Exception.class)
    public WebOauthBindResolveResult bindSessionUser(WebAuthenticationEntity webAuth, UserProfile upstream) {
        if (webAuth == null || upstream == null || StringUtils.isBlank(upstream.getUuid())) {
            throw WebOauthBindException.invalidUpstream(webAuth);
        }
        if (!ShiroUtils.isLogin()) {
            throw new IllegalStateException("请先登录本地账号");
        }
        String sessionUser = ShiroUtils.getUserUuid();
        String upstreamUuid = upstream.getUuid().trim();
        String webAuthUuid = webAuth.getUuid();
        WebOauthBindEntity byUpstream = baseMapper.getByAuthenticationAndUpper(webAuthUuid, upstreamUuid);
        if (byUpstream != null) {
            return resolveExistingUpstreamBind(webAuth, upstream, byUpstream, sessionUser);
        }
        WebOauthBindEntity byUser = baseMapper.getByAuthenticationAndUser(webAuthUuid, sessionUser);
        if (byUser != null && !webOauthBindSupport.isSameUser(byUser.getUpper(), upstreamUuid)) {
            throw WebOauthBindException.localAlreadyBound(webAuth, upstreamUuid, sessionUser, byUser);
        }
        insertBind(webAuthUuid, upstreamUuid, sessionUser);
        syncProfileFields(sessionUser, upstream);
        return WebOauthBindResolveResult.of(toUserProfile(sessionUser), false);
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
        List<WebOauthBindEntity> binds = baseMapper.selectList(new QueryWrapper<WebOauthBindEntity>().eq("user", obj.getUuid()));
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
        if (profile != null
                && StringUtils.isNotBlank(upstream.getNickname())
                && !StringUtils.equals(profile.getNickname(), upstream.getNickname())) {
            profile.setNickname(upstream.getNickname());
            userProfileService.updateById(profile);
        }
        if (StringUtils.isNotBlank(upstream.getIcon())) {
            SysUserEntity user = sysUserService.getByUuid(localUserUuid);
            if (user != null && !StringUtils.equals(user.getIcon(), upstream.getIcon())) {
                user.setIcon(upstream.getIcon());
                user.setIconId("");
                sysUserService.updateById(user);
                sysUserService.clear(localUserUuid);
            }
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
                boolean userChanged = false;
                if (StringUtils.isNotBlank(upstream.getNickname())) {
                    created.setNickname(upstream.getNickname());
                    userChanged = true;
                }
                if (StringUtils.isNotBlank(upstream.getIcon())) {
                    created.setIcon(upstream.getIcon());
                    created.setIconId("");
                    userChanged = true;
                }
                if (userChanged) {
                    sysUserService.updateById(created);
                    sysUserService.clear(created.getUuid());
                }
                UserProfileEntity profile = userProfileService.getByUuid(created.getUuid());
                if (profile != null && StringUtils.isNotBlank(upstream.getNickname())) {
                    profile.setNickname(upstream.getNickname());
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
        SysUserEntity user = sysUserService.getByUuid(userUuid);
        if (user == null) {
            throw new IllegalStateException("本地用户不存在");
        }
        UserProfileEntity profile = userProfileService.getByUuid(userUuid);
        if (profile == null) {
            profile = userProfileService.from(user);
        }
        UserProfile dto = UserProfile.from(profile);
        dto.setIcon(StringUtils.defaultString(user.getIcon()));
        dto.setIconId(StringUtils.defaultString(user.getIconId()));
        if (StringUtils.isBlank(dto.getNickname())) {
            dto.setNickname(StringUtils.defaultString(user.getNickname()));
        }
        if (StringUtils.isBlank(dto.getUsername())) {
            dto.setUsername(StringUtils.defaultString(user.getUsername()));
        }
        dto.setUuid(user.getUuid());
        return dto;
    }

    /** 供单测 stub；生产环境读取 Shiro Session。 */
    String sessionUserUuid() {
        return ShiroUtils.isLogin() ? ShiroUtils.getUserUuid() : null;
    }
}
