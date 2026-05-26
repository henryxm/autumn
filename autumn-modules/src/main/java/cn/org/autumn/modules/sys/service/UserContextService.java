package cn.org.autumn.modules.sys.service;

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
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.NativeWebRequest;

import java.util.Date;

/**
 * 解析无 Session API 的 {@link UserContext}（真人令牌、机器人令牌、已有 Shiro 登录态）。
 */
@Service
public class UserContextService {

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

    public UserContext resolve(NativeWebRequest webRequest) {
        UserContext fromShiro = fromShiro();
        if (fromShiro != null)
            return fromShiro;
        String token = ApiAuthSupport.extractToken(webRequest);
        if (StringUtils.isBlank(token))
            return null;
        if (token.startsWith(RobotTokenService.TOKEN_PREFIX))
            return fromRobotToken(token);
        return fromUserToken(token);
    }

    public User toUser(UserContext context) {
        if (context == null)
            return null;
        User user = new User();
        user.setUuid(context.getUuid());
        user.setNickname(context.getNickname());
        user.setIcon(context.getIcon());
        user.setStatus(context.getStatus());
        user.setRobot(context.isRobot());
        return user;
    }

    public SysUserEntity loadSubjectUser(UserContext context) {
        if (context == null || StringUtils.isBlank(context.getOwner()))
            return null;
        SysUserEntity owner = sysUserService.getByUuid(context.getOwner());
        if (owner == null || owner.getStatus() < 1)
            return null;
        return owner;
    }

    public SysUserEntity loadActorUser(UserContext context) {
        if (context == null)
            return null;
        if (context.isRobot())
            return null;
        return loadSubjectUser(context);
    }

    private UserContext fromShiro() {
        SysUserEntity user = ShiroUtils.getUserEntity();
        if (user != null)
            return fromUser(user);
        Object principal = ShiroUtils.getPrincipal();
        if (principal instanceof RobotPrincipal) {
            RobotPrincipal rp = (RobotPrincipal) principal;
            RobotEntity robot = robotService.getByUuid(rp.getUuid());
            if (robot == null || !robot.isActive())
                return null;
            SysUserEntity owner = sysUserService.getByUuid(robot.getOwner());
            if (owner == null || owner.getStatus() < 1)
                return null;
            return fromRobot(robot);
        }
        return null;
    }

    private UserContext fromUser(SysUserEntity user) {
        UserContext context = new UserContext();
        context.setUuid(user.getUuid());
        context.setOwner(user.getUuid());
        context.setRobot(false);
        context.setNickname(user.getNickname());
        context.setIcon(user.getIcon());
        context.setStatus(user.getStatus());
        return context;
    }

    private UserContext fromRobot(RobotEntity robot) {
        UserContext context = new UserContext();
        context.setUuid(robot.getUuid());
        context.setOwner(robot.getOwner());
        context.setRobot(true);
        context.setNickname(robot.getName());
        context.setIcon(robot.getIcon());
        context.setStatus(robot.getStatus());
        return context;
    }

    private UserContext fromRobotToken(String plainToken) {
        RobotTokenService.ValidateResult validated = robotTokenService.validate(plainToken);
        if (validated == null)
            return null;
        RobotEntity robot = validated.getRobot();
        SysUserEntity owner = sysUserService.getByUuid(robot.getOwner());
        if (owner == null || owner.getStatus() < 1)
            return null;
        tryRobotLoginQuietly(plainToken);
        return fromRobot(robot);
    }

    private UserContext fromUserToken(String token) {
        UserTokenEntity entity = userTokenService.getToken(token);
        if (entity == null)
            return null;
        if (entity.getExpireTime() != null && entity.getExpireTime().before(new Date()))
            return null;
        SysUserEntity user = sysUserService.getByUuid(entity.getUserUuid());
        if (user == null || user.getStatus() < 1)
            return null;
        return fromUser(user);
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
