package cn.org.autumn.modules.sys.service;

import cn.org.autumn.config.ContextHandler;
import cn.org.autumn.model.UserContext;
import cn.org.autumn.modules.bot.entity.RobotEntity;
import cn.org.autumn.modules.bot.service.RobotService;
import cn.org.autumn.modules.bot.service.RobotTokenService;
import cn.org.autumn.modules.bot.shiro.RobotAccessTokenToken;
import cn.org.autumn.modules.bot.shiro.RobotPrincipal;
import cn.org.autumn.modules.sys.entity.SysUserEntity;
import cn.org.autumn.modules.sys.entity.User;
import cn.org.autumn.modules.sys.shiro.ShiroUtils;
import cn.org.autumn.modules.sys.support.ApiAuthSupport;
import cn.org.autumn.modules.usr.entity.UserTokenEntity;
import cn.org.autumn.modules.usr.service.UserTokenService;
import cn.org.autumn.site.UserTokenFactory;
import java.util.Date;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.NativeWebRequest;

/**
 * 解析无 Session API 的 {@link UserContext}（直接返回 {@link SysUserEntity} / {@link RobotEntity} 等业务实体）。
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
    private UserTokenService userTokenService;

    @Autowired
    @Lazy
    private UserTokenFactory userTokenFactory;

    /**
     * 按业务 uuid 从缓存取账号实体（用户或机器人），不解析请求头令牌。
     */
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
        return null;
    }

    /**
     * 从当前请求解析调用者：Shiro → 头令牌 / userUuid 参数；与 {@link #getUserContext(String)} 互补。
     * <p>
     * 同时携带令牌与 {@code userUuid} hint 时，hint 须与令牌解析出的 {@link UserContext#getUuid()} 一致；
     * 仅 hint、无令牌时保留遗留行为（与 minclouds {@code UserInfoResolver} 一致，依赖网关或内网约束）。
     */
    public UserContext resolve(NativeWebRequest webRequest) {
        UserContext fromShiro = fromShiro();
        if (fromShiro != null)
            return fromShiro;
        String userUuidHint = resolveUserUuidHint(webRequest);
        String token = ApiAuthSupport.extractToken(webRequest);
        if (StringUtils.isNotBlank(token)) {
            UserContext fromToken = token.startsWith(RobotTokenService.TOKEN_PREFIX)
                    ? fromRobotToken(token)
                    : fromUserToken(token);
            if (StringUtils.isNotBlank(userUuidHint)) {
                if (fromToken == null || !userUuidHint.equals(fromToken.getUuid()))
                    return null;
                return fromToken;
            }
            return fromToken;
        }
        if (StringUtils.isNotBlank(userUuidHint))
            return fromUserUuid(userUuidHint);
        return null;
    }

    /**
     * 仅当参数类型为 {@link User} 视图时使用；{@link UserContext} 参数应直接 {@code instanceof} 实体。
     */
    public User toUser(UserContext context) {
        if (context == null)
            return null;
        if (context instanceof User)
            return (User) context;
        if (context instanceof SysUserEntity)
            return ((SysUserEntity) context).toUser();
        if (context instanceof RobotEntity)
            return ((RobotEntity) context).toUser();
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

    /**
     * 真人令牌：与 minclouds {@code UserInfoResolverToken} 一致，先走 {@link UserTokenFactory}（业务系统令牌），再回落框架 {@code usr_user_token}。
     */
    private UserContext fromUserToken(String token) {
        if (StringUtils.isBlank(token))
            return null;
        String userUuid = userTokenFactory.getUser(token);
        if (StringUtils.isNotBlank(userUuid))
            return fromUserUuid(userUuid);
        UserTokenEntity entity = userTokenService.getToken(token);
        if (entity == null)
            return null;
        if (entity.getExpireTime() != null && entity.getExpireTime().before(new Date()))
            return null;
        return fromUserUuid(entity.getUserUuid());
    }

    private SysUserEntity fromUserUuid(String userUuid) {
        if (StringUtils.isBlank(userUuid))
            return null;
        SysUserEntity user = sysUserService.getCache(userUuid);
        if (user == null || user.getStatus() < 1)
            return null;
        return user;
    }

    /**
     * 与遗留 {@code UserInfoResolver} 一致：query / header 中的 userUuid。
     */
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
