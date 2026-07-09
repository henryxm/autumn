package cn.org.autumn.modules.sys.shiro;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;

/**
 * 多域名 / 混合域名场景下，按当前请求 Host 解析 Shiro 会话 Cookie 的 name 与 domain。
 */
public final class HostSessionCookieSupport {

    public static final String DEFAULT_COOKIE_NAME = "autumnid";

    private HostSessionCookieSupport() {
    }

    public static String normalizeHost(String host) {
        if (StringUtils.isBlank(host)) {
            return "";
        }
        String h = host.trim().toLowerCase();
        int colon = h.indexOf(':');
        if (colon > 0) {
            h = h.substring(0, colon);
        }
        return h;
    }

    public static List<String> parseDomainList(String csv) {
        if (StringUtils.isBlank(csv)) {
            return Collections.emptyList();
        }
        List<String> list = new ArrayList<>();
        for (String part : csv.split(",")) {
            if (StringUtils.isBlank(part) || part.trim().startsWith("#")) {
                continue;
            }
            list.add(normalizeHost(part));
        }
        return list;
    }

    public static boolean isSubdomainOf(String host, String root) {
        host = normalizeHost(host);
        root = normalizeHost(root);
        if (StringUtils.isBlank(host) || StringUtils.isBlank(root)) {
            return false;
        }
        if (host.equals(root)) {
            return true;
        }
        return host.endsWith("." + root);
    }

    /**
     * 站点绑定多个互不相关的 registrable 域名（如 a.com 与 b.com）。
     */
    public static boolean hasMultipleIndependentDomains(List<String> domains) {
        if (domains == null || domains.size() <= 1) {
            return false;
        }
        for (int i = 0; i < domains.size(); i++) {
            for (int j = i + 1; j < domains.size(); j++) {
                String a = normalizeHost(domains.get(i));
                String b = normalizeHost(domains.get(j));
                if (StringUtils.isBlank(a) || StringUtils.isBlank(b)) {
                    continue;
                }
                if (!isSubdomainOf(a, b) && !isSubdomainOf(b, a)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 当前 Host 可用的 Cookie domain；无关域或独立域返回 null（host-only Cookie）。
     */
    public static String resolveCookieDomain(String requestHost, String clusterRoot) {
        requestHost = normalizeHost(requestHost);
        clusterRoot = normalizeHost(clusterRoot);
        if (StringUtils.isBlank(requestHost) || StringUtils.isBlank(clusterRoot)) {
            return null;
        }
        if (isSubdomainOf(requestHost, clusterRoot)) {
            return clusterRoot.startsWith(".") ? clusterRoot : "." + clusterRoot;
        }
        return null;
    }

    public static String resolveCookieName(String requestHost, String clusterRoot, boolean multiIndependent) {
        if (multiIndependent) {
            return DEFAULT_COOKIE_NAME;
        }
        requestHost = normalizeHost(requestHost);
        clusterRoot = normalizeHost(clusterRoot);
        if (StringUtils.isNotBlank(clusterRoot) && isSubdomainOf(requestHost, clusterRoot)) {
            return clusterRoot;
        }
        return DEFAULT_COOKIE_NAME;
    }

    public static HostSessionCookieHolder resolveHolder(String requestHost, List<String> siteDomains, String clusterRoot) {
        boolean multiIndependent = hasMultipleIndependentDomains(siteDomains);
        String name = resolveCookieName(requestHost, clusterRoot, multiIndependent);
        String domain = resolveCookieDomain(requestHost, clusterRoot);
        return new HostSessionCookieHolder(name, domain);
    }

    public static String resolveRequestHost(HttpServletRequest request) {
        if (request == null) {
            return "";
        }
        String requestHost = normalizeHost(request.getHeader("Host"));
        if (StringUtils.isBlank(requestHost)) {
            requestHost = normalizeHost(request.getServerName());
        }
        return requestHost;
    }
}
