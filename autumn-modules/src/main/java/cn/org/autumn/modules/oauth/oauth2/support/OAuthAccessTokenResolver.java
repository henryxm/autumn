package cn.org.autumn.modules.oauth.oauth2.support;

import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang.StringUtils;
import org.apache.oltu.oauth2.common.OAuth;
import org.apache.oltu.oauth2.common.message.types.ParameterStyle;
import org.apache.oltu.oauth2.rs.request.OAuthAccessResourceRequest;
import org.springframework.http.HttpHeaders;

/**
 * 从 HTTP 请求解析 OAuth2 access_token（支持多种 Autumn 并行 Profile 策略）。
 */
public final class OAuthAccessTokenResolver {

    public enum Policy {
        /** /oauth2/userInfo：JSON 包裹优先，其次 Bearer / 裸 query */
        LEGACY_JSON_WRAP,
        /** /open/oauth2/userInfo：Bearer 或裸 query access_token */
        STANDARD,
        /** SpmFilter 等：JSON 包裹或裸 token */
        PERMISSIVE
    }

    private OAuthAccessTokenResolver() {
    }

    public static String resolve(HttpServletRequest request, Policy policy) {
        if (request == null || policy == null) {
            return "";
        }
        String raw = readRawToken(request);
        if (StringUtils.isBlank(raw)) {
            return "";
        }
        switch (policy) {
            case STANDARD:
                return resolveStandard(raw);
            case PERMISSIVE:
                return resolveLegacyJsonWrap(raw);
            case LEGACY_JSON_WRAP:
            default:
                return resolveLegacyJsonWrap(raw);
        }
    }

    private static String readRawToken(HttpServletRequest request) {
        String auth = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (StringUtils.isNotBlank(auth) && auth.toLowerCase().startsWith("bearer ")) {
            return auth.substring(7).trim();
        }
        try {
            OAuthAccessResourceRequest oauthRequest = new OAuthAccessResourceRequest(request, ParameterStyle.QUERY, ParameterStyle.HEADER);
            return StringUtils.defaultString(oauthRequest.getAccessToken()).trim();
        } catch (Exception e) {
            String queryToken = request.getParameter(OAuth.OAUTH_ACCESS_TOKEN);
            return StringUtils.defaultString(queryToken).trim();
        }
    }

    private static String resolveStandard(String raw) {
        if (raw.startsWith("Bearer ")) {
            return raw.substring(7).trim();
        }
        if (OAuthTokenResponseParser.isJsonWrappedToken(raw)) {
            return OAuthTokenResponseParser.extractAccessTokenKey(raw);
        }
        return raw;
    }

    private static String resolveLegacyJsonWrap(String raw) {
        return OAuthTokenResponseParser.extractAccessTokenKey(raw);
    }
}
