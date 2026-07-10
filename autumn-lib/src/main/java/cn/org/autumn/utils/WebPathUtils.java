package cn.org.autumn.utils;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import org.springframework.util.StringUtils;

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
     * 登录成功后的浏览器跳转目标：拦截非文档形态的 SavedRequest，避免整页打开 JSON REST。
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
        String pathForCheck = pathForCheck(request, pathPart);
        if (isUnsafeNavPath(pathForCheck, queryPart))
            return home;
        return forBrowser(request, c);
    }

    /**
     * OAuth 登录页下发给前端的 callback：REST/API 等非法整页目标返回空串，避免 {@code login.js} 误跳 JSON 端点。
     */
    public static String safeOauthCallbackForClient(HttpServletRequest request, String rawCallback) {
        if (request == null || !StringUtils.hasText(rawCallback)) return "";
        String raw = rawCallback.trim();
        String home = forBrowser(request, "/");
        String safe = safePostLoginRedirect(request, raw);
        if (!home.equals(safe)) return safe;
        String[] parts = candidatePathAndQuery(request, raw);
        if (parts != null && isUnsafeNavPath(parts[0], parts[1])) return "";
        return safe;
    }

    /**
     * 授权 Provider 登录入口 callback 幂等化：剥除嵌套的 {@code /oauth2/login}、{@code /open/oauth2/login}，同站回退 {@code /login}。
     */
    public static String canonicalOauthLoginCallback(HttpServletRequest request, String rawCallback) {
        if (request == null || !StringUtils.hasText(rawCallback)) {
            return "";
        }
        String loginPage = forBrowser(request, "/login");
        String current = rawCallback.trim();
        for (int depth = 0; depth < 8; depth++) {
            String[] parts = candidatePathAndQuery(request, current);
            if (parts == null) {
                break;
            }
            String path = pathForCheck(request, parts[0]);
            if (!HttpNavigationUtils.isOauthLoginEntryPath(path)) {
                String safe = safeOauthCallbackForClient(request, current);
                return StringUtils.hasText(safe) ? safe : loginPage;
            }
            String inner = queryParam(parts[1], "callback");
            if (!StringUtils.hasText(inner)) {
                inner = queryParam(extractAbsoluteQuery(current), "callback");
            }
            if (!StringUtils.hasText(inner)) {
                return loginPage;
            }
            current = decodeCallbackValue(inner);
        }
        return loginPage;
    }

    /** 若 {@code callback} 已嵌套 OAuth 登录入口 URL，302 到 canonical 形态（地址栏不再递增）。 */
    public static String oauthLoginEntryUrlIfCallbackNeedsCanonical(HttpServletRequest request, String loginEntryPath, String clientIdParam, String clientId, String rawCallback) {
        if (request == null || !StringUtils.hasText(rawCallback) || !StringUtils.hasText(loginEntryPath)) {
            return null;
        }
        String canonical = canonicalOauthLoginCallback(request, rawCallback);
        if (!StringUtils.hasText(canonical) || rawCallback.trim().equals(canonical)) {
            return null;
        }
        StringBuilder url = new StringBuilder(forBrowser(request, loginEntryPath));
        boolean hasQuery = false;
        if (StringUtils.hasText(clientIdParam) && StringUtils.hasText(clientId)) {
            url.append("?").append(clientIdParam).append("=").append(encodeUrlParam(clientId));
            hasQuery = true;
        }
        url.append(hasQuery ? "&" : "?").append("callback=").append(encodeUrlParam(canonical));
        return url.toString();
    }

    private static String encodeUrlParam(String value) {
        try {
            return java.net.URLEncoder.encode(StringUtils.hasText(value) ? value.trim() : "", StandardCharsets.UTF_8.name());
        } catch (Exception e) {
            return value;
        }
    }

    private static String decodeCallbackValue(String value) {
        try {
            return URLDecoder.decode(value.trim(), StandardCharsets.UTF_8.name());
        } catch (Exception e) {
            return value.trim();
        }
    }

    private static String extractAbsoluteQuery(String candidate) {
        if (!StringUtils.hasText(candidate)) {
            return null;
        }
        String c = candidate.trim();
        try {
            if (c.startsWith("http://") || c.startsWith("https://")) {
                URL u = new URI(c).toURL();
                return u.getQuery();
            }
        } catch (Exception ignored) {
            return null;
        }
        int qi = c.indexOf('?');
        return qi < 0 ? null : c.substring(qi + 1);
    }

    private static String queryParam(String query, String name) {
        if (!StringUtils.hasText(query) || !StringUtils.hasText(name)) {
            return null;
        }
        for (String part : query.split("&")) {
            if (!StringUtils.hasText(part)) {
                continue;
            }
            int eq = part.indexOf('=');
            String key = eq < 0 ? part : part.substring(0, eq);
            if (name.equals(key)) {
                return eq < 0 ? "" : part.substring(eq + 1);
            }
        }
        return null;
    }

    /**
     * 校验 {@link cn.org.autumn.model.AccountAuthConfig#postLoginRedirect} 是否为有效网页地址（无 Request 上下文）。
     */
    public static boolean isValidPostLoginRedirectConfig(String configuredValue) {
        if (!StringUtils.hasText(configuredValue))
            return true;
        String c = configuredValue.trim();
        try {
            if (c.startsWith("http://") || c.startsWith("https://")) {
                URL u = new URI(c).toURL();
                return !isUnsafeNavPath(u.getPath(), u.getQuery());
            }
        } catch (Exception ignored) {
            return false;
        }
        int qi = c.indexOf('?');
        String pathPart = qi < 0 ? c : c.substring(0, qi);
        String queryPart = qi < 0 ? null : c.substring(qi + 1);
        String pathForCheck = pathPart.startsWith("/") ? pathPart : ("/" + pathPart);
        return !isUnsafeNavPath(pathForCheck, queryPart);
    }

    /**
     * 将账号认证配置中的登录后跳转转为安全目标；无效配置回退 {@code systemDefault}（显式 {@code /} 除外）。
     */
    public static String configuredPostLoginFallback(HttpServletRequest request, String configuredValue, String systemDefault) {
        if (!StringUtils.hasText(configuredValue))
            return systemDefault;
        String trimmed = configuredValue.trim();
        String safe = safePostLoginRedirect(request, trimmed);
        String home = forBrowser(request, "/");
        if (!home.equals(safe))
            return safe;
        String ctx = contextPath(request);
        if ("/".equals(trimmed) || home.equals(trimmed) || (StringUtils.hasText(ctx) && (ctx.equals(trimmed) || (ctx + "/").equals(trimmed))))
            return home;
        return systemDefault;
    }

    /**
     * AJAX 登录成功后的跳转：优先使用 Shiro SavedRequest 中的安全文档页，否则回退 {@code fallbackTarget}。
     */
    public static String resolveLoginRedirectWithSavedRequest(HttpServletRequest request, String savedRequestUrl, String fallbackTarget) {
        if (request == null || !StringUtils.hasText(savedRequestUrl)) return fallbackTarget;
        String safe = safePostLoginRedirect(request, savedRequestUrl.trim());
        String home = forBrowser(request, "/");
        if (home.equals(safe)) return fallbackTarget;
        return safe;
    }

    private static String[] candidatePathAndQuery(HttpServletRequest request, String candidate) {
        if (!StringUtils.hasText(candidate)) return null;
        String c = candidate.trim();
        try {
            if (c.startsWith("http://") || c.startsWith("https://")) {
                URL u = new URI(c).toURL();
                return new String[]{u.getPath(), u.getQuery()};
            }
        } catch (Exception ignored) {
            return null;
        }
        int qi = c.indexOf('?');
        String pathPart = qi < 0 ? c : c.substring(0, qi);
        String queryPart = qi < 0 ? null : c.substring(qi + 1);
        return new String[]{pathForCheck(request, pathPart), queryPart};
    }

    private static String pathForCheck(HttpServletRequest request, String pathPart) {
        String ctx = contextPath(request);
        String pathForCheck = pathPart;
        if (StringUtils.hasText(ctx) && pathPart.startsWith(ctx)) {
            pathForCheck = pathPart.substring(ctx.length());
            if (!pathForCheck.startsWith("/"))
                pathForCheck = "/" + pathForCheck;
        }
        return pathForCheck;
    }

    private static boolean hostEquals(String a, String b) {
        if (a == null || b == null)
            return false;
        return a.equalsIgnoreCase(b);
    }

    /**
     * 是否不宜作为登录后的整页跳转（与业务 Controller 路径无关的通用规则）。
     *
     * @see HttpNavigationUtils
     */
    public static boolean isUnsafeNavPath(String path, String query) {
        if (HttpNavigationUtils.isInfrastructureApiPath(path)) return true;
        return !HttpNavigationUtils.isLikelyBrowserDocumentUrl(path, query);
    }

    /**
     * 当前请求是否应写入 Shiro {@code SavedRequest}（登录成功后浏览器回跳）。
     * 排除 API/AJAX 语义及非文档形态 URL，避免 REST 端点污染回跳目标。
     */
    public static boolean shouldPersistSavedRequest(HttpServletRequest request) {
        if (request == null) return false;
        if (HttpNavigationUtils.isApiOrAjaxRequest(request)) return false;
        return !isUnsafeNavPath(requestPathWithoutContext(request), request.getQueryString());
    }

    static String requestPathWithoutContext(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (!StringUtils.hasText(uri)) return "/";
        String ctx = contextPath(request);
        if (StringUtils.hasText(ctx) && uri.startsWith(ctx)) {
            String rest = uri.substring(ctx.length());
            return rest.isEmpty() ? "/" : rest;
        }
        return uri;
    }

    /**
     * 远端根地址与站点 {@code baseUrl} 是否同实例；{@code originOrBase} 为空视为同实例（本地 AS+RP/OPL+OPC）。
     */
    public static boolean isSameSiteUrl(String originOrBase, String siteBaseUrl) {
        if (!StringUtils.hasText(originOrBase)) {
            return true;
        }
        if (!StringUtils.hasText(siteBaseUrl)) {
            return false;
        }
        String left = originOrBase.trim();
        String right = siteBaseUrl.trim();
        while (left.endsWith("/")) {
            left = left.substring(0, left.length() - 1);
        }
        while (right.endsWith("/")) {
            right = right.substring(0, right.length() - 1);
        }
        return left.equalsIgnoreCase(right);
    }

    /**
     * 当前请求的绝对站点根 URL（scheme + host [+ port] + contextPath），供二维码等多域名场景使用。
     */
    public static String absoluteBaseUrl(HttpServletRequest request) {
        return absoluteBaseUrl(request, null);
    }

    /**
     * 当前请求 Host 下的绝对 URL（scheme + host + context-path + path），供站外 Webhook 等回调使用。
     * {@code path} 可为 {@code /client/...} 或已含 context 的浏览器路径；Host 未解析时回退 {@link #forBrowser}。
     */
    public static String absoluteUrl(HttpServletRequest request, String path, Boolean preferSiteSsl) {
        if (request == null) {
            return path;
        }
        if (!StringUtils.hasText(path)) {
            return absoluteBaseUrl(request, preferSiteSsl);
        }
        String trimmed = path.trim();
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://") || trimmed.startsWith("//")) {
            return trimmed;
        }
        String base = absoluteBaseUrl(request, preferSiteSsl);
        if (!StringUtils.hasText(base)) {
            return forBrowser(request, path);
        }
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        String browserPath = forBrowser(request, path);
        if (!browserPath.startsWith("/")) {
            browserPath = "/" + browserPath;
        }
        String ctx = contextPath(request);
        if (StringUtils.hasText(ctx) && browserPath.startsWith(ctx + "/")) {
            return base + browserPath.substring(ctx.length());
        }
        if (StringUtils.hasText(ctx) && browserPath.equals(ctx)) {
            return base;
        }
        return base + browserPath;
    }

    /**
     * @param preferSiteSsl 为 true 且无反向代理 scheme 时，将 http 提升为 https（对齐 SITE_SSL）
     */
    public static String absoluteBaseUrl(HttpServletRequest request, Boolean preferSiteSsl) {
        if (request == null) {
            return "";
        }
        String scheme = resolveRequestScheme(request);
        if (Boolean.TRUE.equals(preferSiteSsl) && "http".equals(scheme)) {
            scheme = "https";
        }
        String host = resolveRequestHost(request);
        if (!StringUtils.hasText(host)) {
            return "";
        }
        String ctx = contextPath(request);
        StringBuilder sb = new StringBuilder();
        sb.append(scheme).append("://").append(host);
        int port = resolveRequestPort(request, scheme);
        if (port > 0 && !isDefaultPort(scheme, port)) {
            sb.append(':').append(port);
        }
        if (StringUtils.hasText(ctx)) {
            sb.append(ctx);
        }
        return sb.toString();
    }

    static String resolveRequestScheme(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-Proto");
        if (StringUtils.hasText(forwarded)) {
            String first = forwarded.split(",")[0].trim().toLowerCase();
            if ("https".equals(first) || "http".equals(first)) {
                return first;
            }
        }
        String scheme = request.getScheme();
        return StringUtils.hasText(scheme) ? scheme.toLowerCase() : "http";
    }

    static String resolveRequestHost(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-Host");
        if (StringUtils.hasText(forwarded)) {
            String first = forwarded.split(",")[0].trim();
            int colon = first.indexOf(':');
            if (colon > 0) {
                first = first.substring(0, colon);
            }
            return first;
        }
        String host = request.getHeader("Host");
        if (StringUtils.hasText(host)) {
            int colon = host.indexOf(':');
            if (colon > 0) {
                host = host.substring(0, colon);
            }
            return host.trim();
        }
        return request.getServerName();
    }

    static int resolveRequestPort(HttpServletRequest request, String scheme) {
        String forwarded = request.getHeader("X-Forwarded-Host");
        if (StringUtils.hasText(forwarded)) {
            String first = forwarded.split(",")[0].trim();
            int colon = first.indexOf(':');
            if (colon > 0 && colon < first.length() - 1) {
                try {
                    return Integer.parseInt(first.substring(colon + 1));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        String hostHeader = request.getHeader("Host");
        if (StringUtils.hasText(hostHeader)) {
            int colon = hostHeader.indexOf(':');
            if (colon > 0 && colon < hostHeader.length() - 1) {
                try {
                    return Integer.parseInt(hostHeader.substring(colon + 1));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return request.getServerPort();
    }

    static boolean isDefaultPort(String scheme, int port) {
        return ("http".equals(scheme) && port == 80) || ("https".equals(scheme) && port == 443);
    }
}
