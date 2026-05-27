package cn.org.autumn.modules.sys.resolver;

import cn.org.autumn.annotation.Authenticated;
import cn.org.autumn.config.ResolverHandler;
import cn.org.autumn.exception.CodeException;
import cn.org.autumn.model.UserContext;
import cn.org.autumn.modules.bot.entity.RobotEntity;
import cn.org.autumn.modules.sys.entity.SysUserEntity;
import cn.org.autumn.modules.sys.entity.User;
import cn.org.autumn.modules.sys.service.UserContextService;
import cn.org.autumn.web.AuthenticatedSupport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.lang.reflect.Method;

/**
 * 无 Session API 参数注入：直接返回 {@link SysUserEntity} / {@link RobotEntity} 等业务实体（实现 {@link UserContext}）。
 * <p>
 * 与 minclouds 遗留 {@code UserInfoResolver} 并存（order 0）：无 {@link Authenticated} 的 {@code SysUserEntity} 仍走遗留解析器。
 * 仅匹配 {@code UserContext} 本身或带 {@link Authenticated} 的实体参数，避免 {@link User} 实现接口后被误拦截。
 */
@Slf4j
@Component
public class UserContextArgumentResolver implements HandlerMethodArgumentResolver, ResolverHandler {

    private static final int ORDER = 100;

    @Autowired
    private UserContextService userContextService;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        Class<?> type = parameter.getParameterType();
        if (type == UserContext.class)
            return true;
        if (!AuthenticatedSupport.hasAuthenticated(parameter))
            return false;
        return type == User.class || type == SysUserEntity.class || type == RobotEntity.class;
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer, NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
        if (log.isDebugEnabled())
            log.debug("解析进入: {}, required={}", handlerLabel(parameter), AuthenticatedSupport.authRequired(parameter));
        try {
            Object result = resolveArgumentInternal(parameter, webRequest);
            if (log.isDebugEnabled())
                log.debug("解析退出: {} -> {}", handlerLabel(parameter), resultLabel(result));
            return result;
        } catch (Exception e) {
            if (log.isDebugEnabled())
                log.debug("解析退出: {}, 异常: {}", handlerLabel(parameter), e.getMessage());
            throw e;
        }
    }

    private Object resolveArgumentInternal(MethodParameter parameter, NativeWebRequest webRequest) throws Exception {
        boolean required = AuthenticatedSupport.authRequired(parameter);
        Authenticated paramAuth = AuthenticatedSupport.onParameter(parameter);
        UserContext context = userContextService.resolve(webRequest);
        if (context == null && required)
            throw new CodeException("请登录", -10000);
        if (context == null)
            return null;
        if (!context.isActive() && required) {
            if (context.isRobot())
                throw new CodeException("机器人未启用");
            throw new CodeException("账号不可用", -10000);
        }
        Class<?> type = parameter.getParameterType();
        if (type == RobotEntity.class) {
            RobotEntity robot = userContextService.loadRobot(context);
            if (robot == null && required)
                throw new CodeException("请使用机器人访问令牌", -10000);
            return robot;
        }
        if (type == SysUserEntity.class) {
            boolean subject = paramAuth != null && paramAuth.subject();
            SysUserEntity user = subject ? userContextService.loadSubjectUser(context) : userContextService.loadActorUser(context);
            if (user == null && required) {
                if (context.isRobot() && !subject)
                    throw new CodeException("机器人身份请使用 @Authenticated(subject = true) 注入主人", -10000);
                throw new CodeException("用户不可用", -10000);
            }
            if (user != null)
                user.checkThrow();
            return user;
        }
        if (type == User.class)
            return userContextService.toUser(context);
        return context;
    }

    private static String handlerLabel(MethodParameter parameter) {
        Method method = parameter.getMethod();
        String methodName = method != null ? method.getName() : "?";
        return parameter.getDeclaringClass().getSimpleName() + "#" + methodName + "(" + parameter.getParameterType().getSimpleName() + ")";
    }

    private static String resultLabel(Object result) {
        if (result == null)
            return "null";
        if (result instanceof UserContext) {
            UserContext ctx = (UserContext) result;
            return (ctx.isRobot() ? "robot" : "user") + ":" + ctx.getUuid();
        }
        return result.getClass().getSimpleName();
    }

    @Override
    public int getOrder() {
        return ORDER;
    }

    @Override
    public HandlerMethodArgumentResolver getResolver() {
        return this;
    }
}
