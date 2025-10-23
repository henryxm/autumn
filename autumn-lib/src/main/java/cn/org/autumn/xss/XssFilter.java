package cn.org.autumn.xss;

import cn.org.autumn.annotation.DisableXssFilter;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * XSS过滤器
 * 支持通过@DisableXssFilter注解跳过XSS过滤
 */
public class XssFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        // 检查是否需要跳过XSS过滤
        if (shouldSkipXssFilter(httpRequest)) {
            // 跳过XSS过滤，直接传递原始请求
            chain.doFilter(request, response);
        } else {
            // 进行XSS过滤
            XssHttpServletRequestWrapper xssRequest = new XssHttpServletRequestWrapper(httpRequest);
            chain.doFilter(xssRequest, response);
        }
    }

    /**
     * 检查是否需要跳过XSS过滤
     *
     * @param request HTTP请求
     * @return true-跳过XSS过滤，false-进行XSS过滤
     */
    private boolean shouldSkipXssFilter(HttpServletRequest request) {
        try {
            // 获取请求的URI
            String requestURI = request.getRequestURI();
            // 获取HandlerMapping
            ServletContext servletContext = request.getServletContext();
            RequestMappingHandlerMapping handlerMapping =
                    (RequestMappingHandlerMapping) servletContext.getAttribute("org.springframework.web.servlet.DispatcherServlet.HANDLER_MAPPING");
            if (handlerMapping != null) {
                try {
                    // 获取HandlerExecutionChain
                    HandlerExecutionChain handlerChain = handlerMapping.getHandler(request);
                    if (handlerChain != null && handlerChain.getHandler() instanceof HandlerMethod) {
                        HandlerMethod handlerMethod = (HandlerMethod) handlerChain.getHandler();
                        // 检查方法上的注解
                        DisableXssFilter methodAnnotation = handlerMethod.getMethodAnnotation(DisableXssFilter.class);
                        if (methodAnnotation != null && methodAnnotation.value()) {
                            return true;
                        }
                        // 检查类上的注解
                        DisableXssFilter classAnnotation = handlerMethod.getBeanType().getAnnotation(DisableXssFilter.class);
                        if (classAnnotation != null && classAnnotation.value()) {
                            return true;
                        }
                    }
                } catch (Exception e) {
                    // 如果获取Handler失败，继续执行XSS过滤
                }
            }
            // 检查请求头中是否有跳过XSS过滤的标识
            String skipXss = request.getHeader("X-Skip-XSS-Filter");
            if ("true".equalsIgnoreCase(skipXss)) {
                return true;
            }
        } catch (Exception e) {
            // 发生异常时，默认进行XSS过滤
        }
        return false;
    }
}