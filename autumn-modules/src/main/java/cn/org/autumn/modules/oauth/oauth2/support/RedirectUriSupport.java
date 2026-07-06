package cn.org.autumn.modules.oauth.oauth2.support;

import java.net.URI;
import org.apache.commons.lang.StringUtils;

/**
 * OAuth redirect_uri 格式校验（HTTPS 与本地开发例外）。
 */
public final class RedirectUriSupport {

    public static final String CONFIG_ALLOW_HTTP = "OPL_ALLOW_HTTP_REDIRECT";

    private RedirectUriSupport() {
    }

    public static void validateFormat(String redirectUri, boolean allowHttp) {
        if (StringUtils.isBlank(redirectUri)) {
            throw new IllegalArgumentException("redirect_uri不能为空");
        }
        URI uri;
        try {
            uri = URI.create(redirectUri.trim());
        } catch (Exception e) {
            throw new IllegalArgumentException("redirect_uri格式无效");
        }
        String scheme = uri.getScheme();
        if (StringUtils.isBlank(scheme)) {
            throw new IllegalArgumentException("redirect_uri须包含协议");
        }
        String lower = scheme.toLowerCase();
        if ("https".equals(lower)) {
            return;
        }
        if ("http".equals(lower) && (allowHttp || isLocalDevHost(uri.getHost()))) {
            return;
        }
        throw new IllegalArgumentException("redirect_uri须使用https协议");
    }

    public static boolean isLocalDevHost(String host) {
        if (StringUtils.isBlank(host)) {
            return false;
        }
        String h = host.toLowerCase();
        return "localhost".equals(h) || "127.0.0.1".equals(h) || h.startsWith("192.168.") || h.startsWith("10.");
    }
}
