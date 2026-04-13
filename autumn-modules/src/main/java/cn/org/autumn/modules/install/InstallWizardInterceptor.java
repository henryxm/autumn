package cn.org.autumn.modules.install;

import cn.org.autumn.config.InterceptorHandler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.List;

/**
 * 安装模式下将非向导流量重定向到 {@code /install}。
 */
@Component
@ConditionalOnProperty(prefix = "autumn.install", name = "mode", havingValue = "true")
public class InstallWizardInterceptor implements HandlerInterceptor, InterceptorHandler {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String uri = request.getRequestURI();
        String ctx = request.getContextPath();
        if (ctx != null && !ctx.isEmpty() && uri.startsWith(ctx)) {
            uri = uri.substring(ctx.length());
        }
        if (uri.startsWith("/install")) {
            return true;
        }
        if (uri.startsWith("/statics/") || uri.startsWith("/static/") || uri.startsWith("/js/")
                || uri.startsWith("/css/") || uri.startsWith("/images/") || uri.startsWith("/webjars/")
                || uri.equals("/favicon.ico") || uri.startsWith("/error")) {
            return true;
        }
        String target = (ctx == null ? "" : ctx) + "/install";
        response.sendRedirect(response.encodeRedirectURL(target));
        return false;
    }

    @Override
    public HandlerInterceptor getHandlerInterceptor() {
        return this;
    }

    @Override
    public List<String> getPatterns() {
        return Arrays.asList("/**");
    }

    @Override
    public List<String> getExcludePatterns() {
        return Arrays.asList(
                "/install",
                "/install/**",
                "/statics/**",
                "/static/**",
                "/js/**",
                "/css/**",
                "/images/**",
                "/webjars/**",
                "/favicon.ico",
                "/error",
                "/error/**"
        );
    }
}
