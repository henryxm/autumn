package cn.org.autumn.xss;

import cn.org.autumn.annotation.DisableXssFilter;
import org.springframework.lang.NonNull;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * XSS过滤拦截器
 * 在请求处理前检查@DisableXssFilter注解，设置请求属性
 */
public class XssFilterInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) throws Exception {
        if (handler instanceof HandlerMethod) {
            HandlerMethod handlerMethod = (org.springframework.web.method.HandlerMethod) handler;
            // 检查方法上的注解
            DisableXssFilter methodAnnotation = handlerMethod.getMethod().getAnnotation(DisableXssFilter.class);
            if (methodAnnotation != null && methodAnnotation.value()) {
                request.setAttribute("skipXssFilter", true);
                return true;
            }
            // 检查类上的注解
            DisableXssFilter classAnnotation = handlerMethod.getBeanType().getAnnotation(DisableXssFilter.class);
            if (classAnnotation != null && classAnnotation.value()) {
                request.setAttribute("skipXssFilter", true);
                return true;
            }
        }
        return true;
    }
}