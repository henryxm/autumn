package cn.org.autumn.xss;

import cn.org.autumn.annotation.DisableXssFilter;
import cn.org.autumn.service.BaseHttpProxyService;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * XSS 过滤器
 * <p>
 * 支持通过 @DisableXssFilter 注解、请求头 X-Skip-XSS-Filter: true、或路径排除跳过 XSS 过滤。
 * 以下路径默认不进行 XSS 过滤，避免影响业务且提高效率：
 * - 代理接口：{@link BaseHttpProxyService#proxy}（需透传原始 body，不能包装/改写）
 * - 静态资源：/static/**, /js/**, /css/**, /images/**, /favicon.ico 等
 */
public class XssFilter implements Filter {

    /**
     * 不进行 XSS 过滤的路径前缀（去掉 contextPath 后匹配）
     */
    private static final String[] SKIP_PATH_PREFIXES = {
            BaseHttpProxyService.proxy,  // 代理需透传原始流，不能包装
            "/static/",
            "/statics/",
            "/js/",
            "/css/",
            "/images/",
            "/files/",
            "/actuator/",
    };

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        if (shouldSkipXssFilter(httpRequest)) {
            chain.doFilter(request, response);
        } else {
            chain.doFilter(new XssHttpServletRequestWrapper(httpRequest), response);
        }
    }

    /**
     * 检查是否需要跳过 XSS 过滤
     *
     * @param request HTTP 请求
     * @return true 跳过，false 进行 XSS 过滤
     */
    private boolean shouldSkipXssFilter(HttpServletRequest request) {
        try {
            String requestURI = request.getRequestURI();
            String contextPath = request.getContextPath();
            if (contextPath != null && !contextPath.isEmpty() && requestURI.startsWith(contextPath)) {
                requestURI = requestURI.substring(contextPath.length());
            }
            if (requestURI == null) {
                requestURI = "";
            }
            // 1. 路径排除：代理、静态资源等不需要 XSS 且可能受包装影响的请求
            for (String prefix : SKIP_PATH_PREFIXES) {
                if (requestURI.startsWith(prefix)) {
                    return true;
                }
            }
            if (requestURI.equals("/favicon.ico")) {
                return true;
            }
            // 2. 请求头显式跳过
            if ("true".equalsIgnoreCase(request.getHeader("X-Skip-XSS-Filter"))) {
                return true;
            }
            // 3. Controller 上 @DisableXssFilter（需能解析到 Handler 时才生效）
            ServletContext servletContext = request.getServletContext();
            if (servletContext != null) {
                RequestMappingHandlerMapping handlerMapping = (RequestMappingHandlerMapping) servletContext.getAttribute("org.springframework.web.servlet.DispatcherServlet.HANDLER_MAPPING");
                if (handlerMapping != null) {
                    try {
                        HandlerExecutionChain handlerChain = handlerMapping.getHandler(request);
                        if (handlerChain != null && handlerChain.getHandler() instanceof HandlerMethod) {
                            HandlerMethod handlerMethod = (HandlerMethod) handlerChain.getHandler();
                            DisableXssFilter methodAnnotation = handlerMethod.getMethodAnnotation(DisableXssFilter.class);
                            if (methodAnnotation != null && methodAnnotation.value()) {
                                return true;
                            }
                            DisableXssFilter classAnnotation = handlerMethod.getBeanType().getAnnotation(DisableXssFilter.class);
                            if (classAnnotation != null && classAnnotation.value()) {
                                return true;
                            }
                        }
                    } catch (Exception ignored) {
                        // 解析不到 Handler 时继续走 XSS 过滤
                    }
                }
            }
        } catch (Exception ignored) {
            // 异常时执行 XSS 过滤
        }
        return false;
    }
}