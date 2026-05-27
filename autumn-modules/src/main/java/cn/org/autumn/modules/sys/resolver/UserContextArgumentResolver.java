package cn.org.autumn.modules.sys.resolver;

import cn.org.autumn.annotation.Authenticated;
import cn.org.autumn.config.ResolverHandler;
import cn.org.autumn.exception.CodeException;
import cn.org.autumn.model.UserContext;
import cn.org.autumn.modules.sys.entity.SysUserEntity;
import cn.org.autumn.modules.sys.entity.User;
import cn.org.autumn.modules.sys.service.UserContextService;
import cn.org.autumn.web.AuthenticatedSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * 新方案：无 Session API 的 {@link UserContext} / {@link User} / {@link SysUserEntity} 注入（须标 {@link Authenticated}）。
 * <p>
 * 与 minclouds 遗留 {@code UserInfoResolver} 并存、互不抢占：
 * <ul>
 *   <li>{@code UserInfo}（含 {@code @Authenticated UserInfo}）→ 仅 {@code UserInfoResolver}</li>
 *   <li>{@code SysUserEntity} 且无 {@link Authenticated} → 仅 {@code UserInfoResolver}</li>
 *   <li>{@code @Authenticated} + {@code UserContext} / {@code User} / {@code SysUserEntity} → 本解析器</li>
 * </ul>
 * {@link User} 仅匹配类型本身，不匹配子类 {@code UserInfo}。
 */
@Component
public class UserContextArgumentResolver implements HandlerMethodArgumentResolver, ResolverHandler {

    /**
     * 在 {@code UserInfoResolver}（order 0）之后注册
     */
    private static final int ORDER = 100;

    @Autowired
    private UserContextService userContextService;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        Class<?> type = parameter.getParameterType();
        if (UserContext.class.isAssignableFrom(type))
            return true;
        if (!AuthenticatedSupport.hasAuthenticated(parameter))
            return false;
        return type == User.class || type == SysUserEntity.class;
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer, NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
        boolean required = AuthenticatedSupport.authRequired(parameter);
        Authenticated paramAuth = AuthenticatedSupport.onParameter(parameter);
        UserContext context = userContextService.resolve(webRequest);
        if (context == null && required)
            throw new CodeException("请登录", -10000);
        if (context == null)
            return null;
        if (!context.isActive() && required && !context.isRobot())
            throw new CodeException("账号不可用", -10000);
        Class<?> type = parameter.getParameterType();
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

    @Override
    public int getOrder() {
        return ORDER;
    }

    @Override
    public HandlerMethodArgumentResolver getResolver() {
        return this;
    }
}
