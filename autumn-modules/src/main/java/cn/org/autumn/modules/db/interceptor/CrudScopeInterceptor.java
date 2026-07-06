package cn.org.autumn.modules.db.interceptor;

import cn.org.autumn.config.InterceptorHandler;
import cn.org.autumn.database.CrudGuard;
import cn.org.autumn.opl.OplConstants;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 将 HTTP 请求标记为用户写作用域，供 {@link CrudGuard} 在用户写入开关关闭时拦截。
 */
@Component
public class CrudScopeInterceptor implements HandlerInterceptor, InterceptorHandler {

    @Autowired
    private CrudGuard crudGuard;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        crudGuard.user();
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, @Nullable Exception ex) {
        crudGuard.clear();
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
                "/statics/**",
                "/static/**",
                "/js/**",
                "/css/**",
                "/images/**",
                "/favicon.ico",
                "/sys/login",
                "/sys/autologin",
                "/oauth2/login",
                OplConstants.OAUTH2_ROOT + "/login",
                OplConstants.OAUTH2_LOGIN,
                "/captcha.jpg",
                "/loading.html"
        );
    }
}
