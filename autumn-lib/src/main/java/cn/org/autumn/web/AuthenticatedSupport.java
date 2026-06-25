package cn.org.autumn.web;

import cn.org.autumn.annotation.Authenticated;
import java.lang.reflect.AnnotatedElement;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotatedElementUtils;

/**
 * {@link Authenticated} 在参数 / 方法 / 类上的合并查找，供多套用户参数解析器共用，避免职责重叠。
 * <p>
 * 与框架 {@code UserContextArgumentResolver}、minclouds {@code UserInfoResolver} 并存时的划分见各解析器 JavaDoc。
 */
public final class AuthenticatedSupport {

    private AuthenticatedSupport() {
    }

    public static boolean hasAuthenticated(MethodParameter parameter) {
        return findAuthenticated(parameter.getParameter()) != null || findAuthenticated(parameter.getMethod()) != null || findAuthenticated(parameter.getContainingClass()) != null;
    }

    public static boolean authRequired(MethodParameter parameter) {
        Authenticated auth = findAuthenticated(parameter.getParameter());
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

    public static Authenticated onParameter(MethodParameter parameter) {
        return findAuthenticated(parameter.getParameter());
    }

    public static Authenticated findAuthenticated(AnnotatedElement element) {
        if (element == null)
            return null;
        return AnnotatedElementUtils.findMergedAnnotation(element, Authenticated.class);
    }
}
