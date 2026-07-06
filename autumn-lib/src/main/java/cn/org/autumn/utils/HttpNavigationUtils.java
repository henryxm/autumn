package cn.org.autumn.utils;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;

/**
 * 区分「浏览器整页导航」与「API / AJAX」请求，供 Shiro 登录拦截、登录后跳转兜底等横切逻辑复用。
 * <p>
 * 设计原则：按请求语义与 Autumn 常见页面形态（{@code *.html}、{@code spm=}）判断，不对各业务项目的 Controller 路径做黑名单维护。
 */
public final class HttpNavigationUtils {

    private HttpNavigationUtils() {
    }

    /** 当前请求是否应按 API/AJAX 处理（401 JSON 等），且不应写入 Shiro {@code SavedRequest}。 */
    public static boolean isApiOrAjaxRequest(HttpServletRequest request) {
        if (request == null) return false;
        String accept = request.getHeader("Accept");
        if (StringUtils.hasText(accept) && accept.contains("application/json")) return true;
        String contentType = request.getContentType();
        if (StringUtils.hasText(contentType) && contentType.contains("application/json")) return true;
        String requestedWith = request.getHeader("X-Requested-With");
        if (StringUtils.hasText(requestedWith) && "XMLHttpRequest".equalsIgnoreCase(requestedWith)) return true;
        String uri = request.getRequestURI();
        if (!StringUtils.hasText(uri)) return false;
        String path = uri.toLowerCase();
        return path.contains("/api/") || path.endsWith(".json");
    }

    /**
     * 登录成功后是否适合作为浏览器整页 {@code location} 的目标（仅基于 URL 形态；无 Request 时的兜底）。
     * <ul>
     *   <li>允许：{@code /}、{@code *.html}、带 {@code spm=} 的 SPM 页、OAuth 授权页（{@code /oauth2/authorize}、{@code /open/oauth2/authorize}）等；</li>
     *   <li>拒绝：基础设施端点、典型 AJAX 缓存参数、其余无文档特征的 REST 路径。</li>
     * </ul>
     */
    public static boolean isLikelyBrowserDocumentUrl(String path, String query) {
        if (!StringUtils.hasText(path) || "/".equals(path)) return true;
        String p = path.toLowerCase();
        if (p.endsWith(".html")) return true;
        if (hasQueryParam(query, "spm")) return true;
        if (isOauthAuthorizeConsentPath(p)) return true;
        if (isInfrastructureApiPath(p)) return false;
        if (isAjaxCacheBusterOnlyQuery(query)) return false;
        return false;
    }

    /** OAuth / Open Platform 授权确认页（登录后须停留并手动确认授权）。 */
    public static boolean isOauthAuthorizeConsentPath(String path) {
        if (!StringUtils.hasText(path)) return false;
        String p = path.toLowerCase();
        return p.startsWith("/oauth2/authorize") || p.startsWith("/open/oauth2/authorize");
    }

    /** 明显为监控/文档/Token 等基础设施，永不应整页打开。 */
    public static boolean isInfrastructureApiPath(String path) {
        if (!StringUtils.hasText(path)) return false;
        if ("/".equals(path)) return false;
        String p = path.toLowerCase();
        if (p.contains("/api/")) return true;
        if (p.endsWith(".json")) return true;
        if (p.startsWith("/sys/") && !p.endsWith(".html")) return true;
        if (p.startsWith("/actuator/")) return true;
        if (p.startsWith("/druid/")) return true;
        if (p.contains("/v2/api-docs") || p.contains("/v3/api-docs")) return true;
        if (p.startsWith("/oauth2/token")) return true;
        if (p.contains("/oauth2/userinfo")) return true;
        return p.contains("/swagger-resources") || p.contains("/webjars/springfox");
    }

    /** 仅含 {@code _=} / {@code _t=} 时间戳类参数，常见于 XHR 防缓存，非用户主动打开的地址栏 URL。 */
    public static boolean isAjaxCacheBusterOnlyQuery(String query) {
        if (!StringUtils.hasText(query)) return false;
        String q = query.trim();
        return q.matches("^_=[0-9]+$") || q.matches("^_t=[0-9]+$");
    }

    private static boolean hasQueryParam(String query, String name) {
        if (!StringUtils.hasText(query) || !StringUtils.hasText(name)) return false;
        for (String part : query.split("&")) {
            if (part.equals(name) || part.startsWith(name + "=")) return true;
        }
        return false;
    }
}
