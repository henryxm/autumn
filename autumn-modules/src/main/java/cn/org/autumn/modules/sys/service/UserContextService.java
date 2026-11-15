package cn.org.autumn.modules.sys.service;

import cn.org.autumn.config.ContextHandler;
import cn.org.autumn.model.UserContext;
import cn.org.autumn.modules.bot.entity.GuestEntity;
import cn.org.autumn.modules.bot.entity.RobotEntity;
import cn.org.autumn.modules.bot.service.GuestService;
import cn.org.autumn.modules.bot.service.RobotService;
import cn.org.autumn.modules.bot.service.RobotTokenService;
import cn.org.autumn.modules.bot.shiro.RobotAccessTokenToken;
import cn.org.autumn.modules.bot.shiro.RobotPrincipal;
import cn.org.autumn.modules.sys.entity.SysUserEntity;
import cn.org.autumn.modules.sys.entity.User;
import cn.org.autumn.modules.sys.shiro.ShiroUtils;
import cn.org.autumn.modules.sys.support.ApiAuthSupport;
import cn.org.autumn.modules.sys.support.ApiCredential;
import cn.org.autumn.modules.sys.support.ApiTokenLoginSupport;
import cn.org.autumn.modules.sys.support.AuthContextBinder;
import cn.org.autumn.modules.sys.support.OAuthTokenInfo;
import cn.org.autumn.opl.OplConstants;
import cn.org.autumn.web.AuthSource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.NativeWebRequest;

/**
 * 解析无 Session API 的 {@link UserContext}（直接返回 {@link SysUserEntity} / {@link RobotEntity} / {@link GuestEntity} 等业务实体）。
 * <p>
 * 解析成功时绑定请求级 {@link cn.org.autumn.web.AuthContext}（见 {@link AuthContextBinder}）。
 */
@Slf4j
@Service
public class UserContextService implements ContextHandler {

    @Autowired
    @Lazy
    private SysUserService sysUserService;

    @Autowired
    @Lazy
    private RobotService robotService;

    @Autowired
    @Lazy
    private RobotTokenService robotTokenService;

    @Autowired
    @Lazy
    private GuestService guestService;

    @Autowired
    @Lazy
    private SysConfigService sysConfigService;

    public UserContext getUserContext(String uuid) {
        SysUserEntity user = sysUserService.getCache(uuid);
        if (user != null) {
            if (user.getStatus() < 1)
                return null;
            return user;
        }
        RobotEntity robot = robotService.getCache(uuid);
        if (robot != null && robot.isActive())
            return robot;
        GuestEntity guest = guestService.getActive(uuid);
        if (guest != null)
            return guest;
        return null;
    }

    /**
     * 从当前请求解析调用者：Shiro → 头令牌 / userUuid hint。
     * 成功路径绑定 AuthContext。
     */
    public UserContext resolve(NativeWebRequest webRequest) {
        ApiCredential credential = ApiAuthSupport.extractCredential(webRequest);
        String hintUuid = resolveUserUuidHint(webRequest);
        HttpServletRequest http = webRequest != null ? webRequest.getNativeRequest(HttpServletRequest.class) : null;
        String token = credential.getToken();
        boolean robotToken = StringUtils.isNotBlank(token) && token.startsWith(RobotTokenService.TOKEN_PREFIX);
        OAuthTokenInfo oauth = StringUtils.isNotBlank(token) && !robotToken ? ApiTokenLoginSupport.resolveOAuth(token) : null;

        UserContext fromShiro = fromShiro();
        if (fromShiro != null) {
            AuthSource source = fromShiro instanceof RobotEntity ? AuthSource.ROBOT_TOKEN : AuthSource.SESSION;
            String accessKey = "";
            if (fromShiro instanceof SysUserEntity) {
                SysUserEntity su = (SysUserEntity) fromShiro;
                AuthContextBinder.EnrichResult enriched = AuthContextBinder.enrichSessionWithOAuth(source, su.getUuid(), su.getUsername(), oauth, "");
                source = enriched.getSource();
                accessKey = enriched.getAccessKey();
                if (StringUtils.isBlank(accessKey) && AuthContextBinder.looksLikeAccessKeyShadow(su))
                    accessKey = StringUtils.defaultString(su.getUsername());
            }
            bindResolved(http, credential, hintUuid, fromShiro, source, accessKey);
            return fromShiro;
        }

        if (StringUtils.isNotBlank(token)) {
            UserContext fromToken = robotToken ? fromRobotToken(token) : fromUserToken(token, oauth);
            if (StringUtils.isNotBlank(hintUuid)) {
                if (fromToken == null || !hintUuid.equals(fromToken.getUuid()))
                    return null;
            } else if (fromToken == null) {
                return null;
            }
            AuthSource source = robotToken ? AuthSource.ROBOT_TOKEN : (oauth != null ? AuthSource.OAUTH_TOKEN : AuthSource.USER_TOKEN);
            String accessKey = robotToken || oauth == null ? "" : oauth.getClientId();
            bindResolved(http, credential, hintUuid, fromToken, source, accessKey);
            return fromToken;
        }

        if (StringUtils.isNotBlank(hintUuid)) {
            if (isOpenPlatformApiRequest(webRequest) || !sysConfigService.isAllowUserUuidHint())
                return null;
            SysUserEntity fromHint = fromUserUuid(hintUuid);
            if (fromHint == null)
                return null;
            bindResolved(http, credential, hintUuid, fromHint, AuthSource.UUID_HINT, "");
            return fromHint;
        }
        return null;
    }

    private void bindResolved(HttpServletRequest request, ApiCredential credential, String hintUuid, UserContext context, AuthSource source, String accessKey) {
        if (request == null || context == null)
            return;
        String userUuid = context.getUuid();
        String actorUuid = null;
        if (context instanceof RobotEntity) {
            actorUuid = context.getUuid();
            userUuid = StringUtils.defaultString(context.getSubject());
        }
        AuthContextBinder.bind(request, credential, hintUuid, source, userUuid, actorUuid, accessKey);
    }

    static boolean isOpenPlatformApiRequest(NativeWebRequest webRequest) {
        if (webRequest == null)
            return false;
        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        if (request == null)
            return false;
        String uri = request.getRequestURI();
        if (StringUtils.isBlank(uri))
            return false;
        String contextPath = StringUtils.defaultString(request.getContextPath());
        String path = uri.startsWith(contextPath) ? uri.substring(contextPath.length()) : uri;
        return path.startsWith(OplConstants.API_PLATFORM + "/") || path.equals(OplConstants.API_PLATFORM);
    }

    public User toUser(UserContext context) {
        if (context == null)
            return null;
        if (context instanceof User)
            return (User) context;
        if (context instanceof SysUserEntity)
            return ((SysUserEntity) context).toUser();
        if (context instanceof RobotEntity)
            return ((RobotEntity) context).toUser();
        if (context instanceof GuestEntity)
            return ((GuestEntity) context).toUser();
        log.warn("UserContext unknown implementation type: {}", context.getClass().getName());
        return null;
    }

    public SysUserEntity loadSubjectUser(UserContext context) {
        if (context == null || StringUtils.isBlank(context.getSubject()))
            return null;
        if (context instanceof SysUserEntity)
            return (SysUserEntity) context;
        SysUserEntity owner = sysUserService.getCache(context.getSubject());
        if (owner == null || owner.getStatus() < 1)
            return null;
        return owner;
    }

    public SysUserEntity loadActorUser(UserContext context) {
        if (context == null)
            return null;
        if (context instanceof SysUserEntity)
            return (SysUserEntity) context;
        return null;
    }

    public RobotEntity loadRobot(UserContext context) {
        if (context instanceof RobotEntity)
            return (RobotEntity) context;
        return null;
    }

    private UserContext fromShiro() {
        SysUserEntity user = ShiroUtils.getUserEntity();
        if (user != null)
            return user;
        Object principal = ShiroUtils.getPrincipal();
        if (principal instanceof RobotPrincipal) {
            RobotPrincipal rp = (RobotPrincipal) principal;
            RobotEntity robot = robotService.getCache(rp.getUuid());
            if (robot == null || !robot.isActive())
                return null;
            SysUserEntity owner = sysUserService.getCache(robot.getOwner());
            if (owner == null || owner.getStatus() < 1)
                return null;
            return robot;
        }
        return null;
    }

    private UserContext fromRobotToken(String plainToken) {
        RobotTokenService.ValidateResult validated = robotTokenService.validate(plainToken);
        if (validated == null)
            return null;
        RobotEntity robot = validated.getRobot();
        SysUserEntity owner = sysUserService.getCache(robot.getOwner());
        if (owner == null || owner.getStatus() < 1)
            return null;
        tryRobotLoginQuietly(plainToken);
        return robot;
    }

    private UserContext fromUserToken(String token, OAuthTokenInfo oauth) {
        if (StringUtils.isBlank(token))
            return null;
        String userUuid = oauth != null && StringUtils.isNotBlank(oauth.getUserUuid())
                ? oauth.getUserUuid()
                : ApiTokenLoginSupport.resolveLegacyUserUuid(token);
        if (StringUtils.isBlank(userUuid))
            return null;
        return fromUserUuid(userUuid);
    }

    private SysUserEntity fromUserUuid(String userUuid) {
        if (StringUtils.isBlank(userUuid))
            return null;
        SysUserEntity user = sysUserService.getCache(userUuid);
        if (user == null || user.getStatus() < 1)
            return null;
        return user;
    }

    private static String resolveUserUuidHint(NativeWebRequest webRequest) {
        if (webRequest == null)
            return null;
        String userUuid = webRequest.getParameter("userUuid");
        if (StringUtils.isBlank(userUuid))
            userUuid = webRequest.getParameter("user_uuid");
        if (StringUtils.isBlank(userUuid))
            userUuid = webRequest.getHeader("userUuid");
        if (StringUtils.isBlank(userUuid))
            userUuid = webRequest.getHeader("user_uuid");
        return StringUtils.isBlank(userUuid) ? null : userUuid.trim();
    }

    private void tryRobotLoginQuietly(String plainToken) {
        try {
            Subject subject = SecurityUtils.getSubject();
            if (subject.getPrincipal() instanceof RobotPrincipal)
                return;
            subject.login(new RobotAccessTokenToken(plainToken));
        } catch (Exception ignored) {
        }
    }
}
