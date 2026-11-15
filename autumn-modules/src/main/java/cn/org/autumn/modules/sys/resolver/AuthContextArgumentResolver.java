package cn.org.autumn.modules.sys.resolver;

import cn.org.autumn.config.ResolverHandler;
import cn.org.autumn.web.AuthContext;
import cn.org.autumn.web.AuthContexts;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * 注入请求级 {@link AuthContext}（由 {@code UserContextService} / 遗留 UserInfoResolver 等绑定）。
 * <p>
 * 未绑定时返回 {@code null}，不抛错。方法参数请写在身份参数（如 {@code @Authenticated UserInfo}）
 * <strong>之后</strong>，以便先完成绑定再注入。
 */
@Component
public class AuthContextArgumentResolver implements HandlerMethodArgumentResolver, ResolverHandler {

    /** 早于业务 UserContext 解析器，便于同方法同时注入 */
    private static final int ORDER = 50;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return AuthContext.class.equals(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer, NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        if (webRequest == null)
            return null;
        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        return AuthContexts.get(request);
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
