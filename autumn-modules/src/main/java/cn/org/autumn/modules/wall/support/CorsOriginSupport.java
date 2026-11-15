package cn.org.autumn.modules.wall.support;

import org.apache.commons.lang3.StringUtils;

import java.net.URI;
import java.util.Collection;
import java.util.Locale;

/**
 * CORS Origin 白名单匹配：精确或与站点基址同源。
 */
public final class CorsOriginSupport {

    private CorsOriginSupport() {
    }

    /**
     * @param origin           请求 Origin 头
     * @param siteBaseUrl      站点基址（可含 path）
     * @param allowedOrigins   额外白名单（完整 Origin，如 https://app.example.com）
     */
    public static boolean isAllowed(String origin, String siteBaseUrl, Collection<String> allowedOrigins) {
        if (StringUtils.isBlank(origin)) {
            return false;
        }
        String o = origin.trim();
        if (allowedOrigins != null) {
            for (String raw : allowedOrigins) {
                if (StringUtils.isNotBlank(raw) && o.equalsIgnoreCase(raw.trim())) {
                    return true;
                }
            }
        }
        if (StringUtils.isBlank(siteBaseUrl)) {
            return false;
        }
        try {
            URI originUri = URI.create(o);
            URI siteUri = URI.create(siteBaseUrl.trim());
            if (originUri.getScheme() == null || originUri.getHost() == null
                    || siteUri.getScheme() == null || siteUri.getHost() == null) {
                return false;
            }
            if (!originUri.getScheme().equalsIgnoreCase(siteUri.getScheme())) {
                return false;
            }
            if (!originUri.getHost().equalsIgnoreCase(siteUri.getHost())) {
                return false;
            }
            int op = originUri.getPort() > 0 ? originUri.getPort() : defaultPort(originUri.getScheme());
            int sp = siteUri.getPort() > 0 ? siteUri.getPort() : defaultPort(siteUri.getScheme());
            return op == sp;
        } catch (Exception e) {
            return false;
        }
    }

    private static int defaultPort(String scheme) {
        if (scheme == null) {
            return -1;
        }
        String s = scheme.toLowerCase(Locale.ROOT);
        if ("https".equals(s)) {
            return 443;
        }
        if ("http".equals(s)) {
            return 80;
        }
        return -1;
    }
}
