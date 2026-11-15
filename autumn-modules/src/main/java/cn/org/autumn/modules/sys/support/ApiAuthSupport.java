package cn.org.autumn.modules.sys.support;

import cn.org.autumn.modules.oauth.oauth2.support.OAuthTokenResponseParser;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.context.request.NativeWebRequest;

/**
 * 从请求头解析 API 访问令牌（与历史 UserInfoResolver / SpmFilter 习惯一致）。
 */
public final class ApiAuthSupport {

    private static final String[] HEADER_NAMES = {
            "X-Robot-Token",
            "Robot-Token",
            "Token",
            "Auth",
            "Authenticated",
            "Authorization"
    };

    private ApiAuthSupport() {
    }

    public static String extractToken(NativeWebRequest webRequest) {
        return extractCredential(webRequest).getToken();
    }

    public static String extractToken(HttpServletRequest request) {
        return extractCredential(request).getToken();
    }

    /**
     * 解析令牌并返回命中的头名（用于 AuthContext.credentialHeader）。
     */
    public static ApiCredential extractCredential(NativeWebRequest webRequest) {
        if (webRequest == null)
            return new ApiCredential("", "");
        for (String name : HEADER_NAMES) {
            String raw = header(webRequest, name);
            if (StringUtils.isBlank(raw))
                continue;
            String token = normalizeBearer(raw);
            if (StringUtils.isNotBlank(token))
                return new ApiCredential(token, name);
        }
        return new ApiCredential("", "");
    }

    public static ApiCredential extractCredential(HttpServletRequest request) {
        if (request == null)
            return new ApiCredential("", "");
        for (String name : HEADER_NAMES) {
            String raw = request.getHeader(name);
            if (StringUtils.isBlank(raw))
                continue;
            String token = normalizeBearer(raw.trim());
            if (StringUtils.isNotBlank(token))
                return new ApiCredential(token, name);
        }
        return new ApiCredential("", "");
    }

    private static String header(NativeWebRequest webRequest, String name) {
        String value = webRequest.getHeader(name);
        return value == null ? "" : value.trim();
    }

    public static String normalizeBearer(String token) {
        if (StringUtils.isBlank(token))
            return "";
        token = token.trim();
        if (token.startsWith("Bearer "))
            token = token.substring(7).trim();
        String extracted = OAuthTokenResponseParser.extractAccessTokenKey(token);
        return StringUtils.isNotBlank(extracted) ? extracted : token;
    }
}
