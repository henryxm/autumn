package cn.org.autumn.modules.sys.filter;

import cn.org.autumn.modules.bot.service.RobotTokenService;
import cn.org.autumn.modules.sys.service.SysConfigService;
import cn.org.autumn.modules.sys.shiro.ShiroUtils;
import cn.org.autumn.modules.sys.support.ApiAuthSupport;
import cn.org.autumn.modules.sys.support.CsrfTokenSupport;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Locale;

/**
 * Cookie 会话状态变更 CSRF 防护；Bearer / {@code rbt_} API 豁免。
 * 开关以 SysConfig {@code SECURITY_AUTH_CONFIG.csrfEnforce} 为准（集群同步）。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 50)
public class CsrfProtectFilter implements Filter {

    @Autowired
    @Lazy
    private SysConfigService sysConfigService;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        boolean enforce = sysConfigService != null && sysConfigService.isCsrfEnforce();
        if (!enforce || !isStateChanging(req.getMethod()) || isExempt(req)) {
            chain.doFilter(request, response);
            return;
        }
        if (!ShiroUtils.isLogin()) {
            chain.doFilter(request, response);
            return;
        }
        String token = req.getHeader(CsrfTokenSupport.HEADER);
        if (StringUtils.isBlank(token)) {
            token = req.getParameter("_csrf");
        }
        if (!CsrfTokenSupport.validate(token)) {
            resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
            resp.setContentType("application/json;charset=UTF-8");
            resp.getWriter().write("{\"code\":403,\"msg\":\"CSRF token invalid\"}");
            return;
        }
        chain.doFilter(request, response);
    }

    private static boolean isStateChanging(String method) {
        if (method == null) {
            return false;
        }
        String m = method.toUpperCase(Locale.ROOT);
        return "POST".equals(m) || "PUT".equals(m) || "DELETE".equals(m) || "PATCH".equals(m);
    }

    private boolean isExempt(HttpServletRequest req) {
        String path = req.getRequestURI();
        String ctx = StringUtils.defaultString(req.getContextPath());
        if (path.startsWith(ctx)) {
            path = path.substring(ctx.length());
        }
        if (path.startsWith("/http/proxy/v1")
                || path.startsWith("/bot/api/v1")
                || path.startsWith("/oauth2/")
                || path.startsWith("/sys/login")
                || path.startsWith("/sys/register")
                || path.startsWith("/sys/forgotpassword")
                || path.startsWith("/sys/autologin")
                || path.startsWith("/install")
                || path.startsWith("/captcha")) {
            return true;
        }
        String auth = req.getHeader("Authorization");
        if (StringUtils.isNotBlank(auth) && auth.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return true;
        }
        String apiToken = ApiAuthSupport.extractToken(req);
        return StringUtils.isNotBlank(apiToken) && (apiToken.startsWith(RobotTokenService.TOKEN_PREFIX) || apiToken.length() >= 16);
    }
}
