package cn.org.autumn.modules.client.oauth2;

import cn.org.autumn.modules.client.entity.WebAuthenticationEntity;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Component;

/** 从 {@code WebAuthenticationEntity.originUri} 推断 OAuth / QRC 端点；显式 URI 优先。 */
@Component
public class WebOauthEndpointResolver {

    public static final String PATH_AUTHORIZE = "/oauth2/authorize";
    public static final String PATH_TOKEN = "/oauth2/token";
    public static final String PATH_USER_INFO = "/oauth2/userInfo";
    public static final String PATH_QRC_OPEN_CREATE = "/qrc/api/v1/ticket/open/create";
    public static final String PATH_QRC_OPEN_STATUS = "/qrc/api/v1/ticket/open/status";
    public static final String PATH_QRC_OPEN_CANCEL = "/qrc/api/v1/ticket/open/cancel";

    /** 是否配置了远程身份源（{@code originUri} 非空且规范化后有效）。 */
    public boolean hasRemoteOrigin(WebAuthenticationEntity web) {
        return StringUtils.isNotBlank(resolveOriginUri(web));
    }

    public String resolveOriginUri(WebAuthenticationEntity web) {
        if (web == null || StringUtils.isBlank(web.getOriginUri())) {
            return null;
        }
        return normalizeOriginUri(web.getOriginUri());
    }

    public String resolveAuthorizeUri(WebAuthenticationEntity web) {
        if (web != null && StringUtils.isNotBlank(web.getAuthorizeUri())) {
            return web.getAuthorizeUri().trim();
        }
        String origin = resolveOriginUri(web);
        return origin == null ? null : origin + PATH_AUTHORIZE;
    }

    public String resolveAccessTokenUri(WebAuthenticationEntity web) {
        if (web != null && StringUtils.isNotBlank(web.getAccessTokenUri())) {
            return web.getAccessTokenUri().trim();
        }
        String origin = resolveOriginUri(web);
        return origin == null ? null : origin + PATH_TOKEN;
    }

    public String resolveUserInfoUri(WebAuthenticationEntity web, boolean remoteIdp) {
        if (web != null && StringUtils.isNotBlank(web.getUserInfoUri())) {
            return web.getUserInfoUri().trim();
        }
        if (!remoteIdp && (web == null || StringUtils.isBlank(web.getOriginUri()))) {
            return null;
        }
        String origin = resolveOriginUri(web);
        return origin == null ? null : origin + PATH_USER_INFO;
    }

    public String resolveQrcOpenCreateUri(WebAuthenticationEntity web) {
        return requireOriginUri(web) + PATH_QRC_OPEN_CREATE;
    }

    public String resolveQrcOpenStatusUri(WebAuthenticationEntity web) {
        return requireOriginUri(web) + PATH_QRC_OPEN_STATUS;
    }

    public String resolveQrcOpenCancelUri(WebAuthenticationEntity web) {
        return requireOriginUri(web) + PATH_QRC_OPEN_CANCEL;
    }

    public String requireOriginUri(WebAuthenticationEntity web) {
        String origin = resolveOriginUri(web);
        if (StringUtils.isBlank(origin)) {
            throw new IllegalArgumentException("未配置身份源根地址（client_web_authentication.originUri）");
        }
        return origin;
    }

    public String normalizeOriginUri(String originUri) {
        if (StringUtils.isBlank(originUri)) {
            return null;
        }
        String value = originUri.trim();
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }
}
