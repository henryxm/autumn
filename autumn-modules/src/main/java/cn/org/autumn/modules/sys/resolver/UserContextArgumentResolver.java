package cn.org.autumn.modules.sys.resolver;

import cn.org.autumn.annotation.Authenticated;
import cn.org.autumn.config.ResolverHandler;
import cn.org.autumn.exception.CodeException;
import cn.org.autumn.model.UserContext;
import cn.org.autumn.modules.sys.entity.SysUserEntity;
import cn.org.autumn.modules.sys.entity.User;
import cn.org.autumn.modules.sys.service.UserContextService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.lang.reflect.AnnotatedElement;

/**
 * 无 Session API 用户上下文注入：{@link UserContext} / {@link User} / {@link SysUserEntity}。
 * 是否强制鉴权由 {@link Authenticated#notNull()} 控制（参数、方法、类上均可声明）。
 */
@Component
public class UserContextArgumentResolver implements HandlerMethodArgumentResolver, ResolverHandler {

    @Autowired
    private UserContextService userContextService;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        Class<?> type = parameter.getParameterType();
        if (UserContext.class.isAssignableFrom(type))
            return true;
        if (!hasAuthenticated(parameter))
            return false;
        return User.class.isAssignableFrom(type) || SysUserEntity.class.isAssignableFrom(type);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
        boolean required = authRequired(parameter);
        Authenticated paramAuth = findAuthenticated(parameter);

        UserContext context = userContextService.resolve(webRequest);
        if (context == null && required)
            throw new CodeException("请登录", -10000);
        if (context == null)
            return null;
        if (!context.isActive() && required && !context.isRobot())
            throw new CodeException("账号不可用", -10000);

        Class<?> type = parameter.getParameterType();
        if (SysUserEntity.class.isAssignableFrom(type)) {
            boolean subject = paramAuth != null && paramAuth.subject();
            SysUserEntity user = subject
                    ? userContextService.loadSubjectUser(context)
                    : userContextService.loadActorUser(context);
            if (user == null && required) {
                if (context.isRobot() && !subject)
                    throw new CodeException("机器人身份请使用 @Authenticated(subject = true) 注入主人", -10000);
                throw new CodeException("用户不可用", -10000);
            }
            if (user != null)
                user.checkThrow();
            return user;
        }
        if (User.class.isAssignableFrom(type))
            return userContextService.toUser(context);
        return context;
    }

    private static boolean hasAuthenticated(MethodParameter parameter) {
        return findAuthenticated(parameter) != null
                || findAuthenticated(parameter.getMethod()) != null
                || findAuthenticated(parameter.getContainingClass()) != null;
    }

    private static boolean authRequired(MethodParameter parameter) {
        Authenticated auth = findAuthenticated(parameter);
        if (auth != null)
            return auth.notNull();
        auth = findAuthenticated(parameter.getMethod());
        if (auth != null)
            return auth.notNull();
        auth = findAuthenticated(parameter.getContainingClass());
        if (auth != null)
            return auth.notNull();
        return false;
    }

    private static Authenticated findAuthenticated(AnnotatedElement element) {
        if (element == null)
            return null;
        return AnnotatedElementUtils.findMergedAnnotation(element, Authenticated.class);
    }

    private static Authenticated findAuthenticated(MethodParameter parameter) {
        return findAuthenticated((AnnotatedElement) parameter.getParameter());
    }

    @Override
    public HandlerMethodArgumentResolver getResolver() {
        return this;
    }
}
