package cn.org.autumn.utils;

import org.springframework.util.StringUtils;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import java.net.URL;

/**
 * 处理 Servlet context-path，避免在子路径部署时把浏览器导航到站点根（误落到网关/其它应用 JSON 接口）。
 */
public final class WebPathUtils {

    private WebPathUtils() {
    }

    public static String contextPath(HttpServletRequest request) {
        if (request == null)
            return "";
        String cp = request.getContextPath();
        return cp == null ? "" : cp;
    }

    public static String contextPath(ServletContext servletContext) {
        if (servletContext == null)
            return "";
        String cp = servletContext.getContextPath();
        return cp == null ? "" : cp;
    }

    /**
     * 供浏览器地址栏、{@code direct.html} 等使用的应用内 URL。
     * <ul>
     *   <li>{@code http(s)://} 与 {@code //} 原样返回；</li>
     *   <li>已带当前 context-path 前缀的以 {@code /} 开头的路径原样返回；</li>
     *   <li>以 {@code /} 开头且未带 context 的，补上 context；</li>
     *   <li>其它相对片段（如 {@code index.html}）补为 {@code context/片段}。</li>
     * </ul>
     */
    public static String forBrowser(HttpServletRequest request, String path) {
        if (request == null)
            return path;
        String ctx = contextPath(request);
        if (!StringUtils.hasText(path)) {
            return ctx.isEmpty() ? "/" : (ctx + "/");
        }
        String p = path.trim();
        if (p.startsWith("http://") || p.startsWith("https://") || p.startsWith("//"))
            return p;
        if (!ctx.isEmpty() && (p.equals(ctx) || p.startsWith(ctx + "/")))
            return p;
        if (p.startsWith("/"))
            return ctx + p;
        return ctx.isEmpty() ? p : (ctx + "/" + p);
    }

    /**
     * 登录成功后的浏览器跳转目标：拦截明显为接口/文档的 SavedRequest，避免整页打开 JSON。
     */
    public static String safePostLoginRedirect(HttpServletRequest request, String candidate) {
        String home = forBrowser(request, "/");
        if (!StringUtils.hasText(candidate))
            return home;
        String c = candidate.trim();
        try {
            if (c.startsWith("http://") || c.startsWith("https://")) {
                URL u = new URL(c);
                URL base = new URL(request.getRequestURL().toString());
                if (u.getPort() != base.getPort() || !hostEquals(u.getHost(), base.getHost()))
                    return home;
                if (isUnsafeNavPath(u.getPath(), u.getQuery()))
                    return home;
                return c;
            }
        } catch (Exception ignored) {
            return home;
        }
        int qi = c.indexOf('?');
        String pathPart = qi < 0 ? c : c.substring(0, qi);
        String queryPart = qi < 0 ? null : c.substring(qi + 1);
        String ctx = contextPath(request);
        String pathForCheck = pathPart;
        if (StringUtils.hasText(ctx) && pathPart.startsWith(ctx)) {
            pathForCheck = pathPart.substring(ctx.length());
            if (!pathForCheck.startsWith("/"))
                pathForCheck = "/" + pathForCheck;
        }
        if (isUnsafeNavPath(pathForCheck, queryPart))
            return home;
        return forBrowser(request, c);
    }

    private static boolean hostEquals(String a, String b) {
        if (a == null || b == null)
            return false;
        return a.equalsIgnoreCase(b);
    }

    /**
     * 明显不应作为「整页」打开的地址（多为 JSON/XML 接口或监控端点）。
     */
    public static boolean isUnsafeNavPath(String path, String query) {
        if (!StringUtils.hasText(path))
            return false;
        if ("/".equals(path))
            return false;
        String p = path.toLowerCase();
        if (p.contains("/api/"))
            return true;
        if (p.endsWith(".json"))
            return true;
        if (p.startsWith("/sys/") && !p.endsWith(".html"))
            return true;
        if (p.startsWith("/actuator/"))
            return true;
        if (p.startsWith("/druid/"))
            return true;
        if (p.contains("/v2/api-docs") || p.contains("/v3/api-docs"))
            return true;
        if (p.startsWith("/oauth2/token"))
            return true;
        if (p.contains("/oauth2/userinfo"))
            return true;
        if (p.contains("/swagger-resources") || p.contains("/webjars/springfox"))
            return true;
        return false;
    }
}
