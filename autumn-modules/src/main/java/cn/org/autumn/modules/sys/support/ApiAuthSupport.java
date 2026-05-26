package cn.org.autumn.modules.sys.support;

import org.apache.commons.lang3.StringUtils;
import org.springframework.web.context.request.NativeWebRequest;

import javax.servlet.http.HttpServletRequest;

/**
 * 从请求头解析 API 访问令牌（与历史 UserInfoResolver / SpmFilter 习惯一致）。
 */
public final class ApiAuthSupport {

    private ApiAuthSupport() {
    }

    public static String extractToken(NativeWebRequest webRequest) {
        if (webRequest == null)
            return "";
        String token = header(webRequest, "X-Robot-Token");
        if (StringUtils.isBlank(token))
            token = header(webRequest, "Robot-Token");
        if (StringUtils.isBlank(token))
            token = header(webRequest, "Token");
        if (StringUtils.isBlank(token))
            token = header(webRequest, "Auth");
        if (StringUtils.isBlank(token))
            token = header(webRequest, "Authenticated");
        if (StringUtils.isBlank(token))
            token = header(webRequest, "Authorization");
        return normalizeBearer(token);
    }

    public static String extractToken(HttpServletRequest request) {
        if (request == null)
            return "";
        String token = request.getHeader("X-Robot-Token");
        if (StringUtils.isBlank(token))
            token = request.getHeader("Robot-Token");
        if (StringUtils.isBlank(token))
            token = request.getHeader("Token");
        if (StringUtils.isBlank(token))
            token = request.getHeader("Auth");
        if (StringUtils.isBlank(token))
            token = request.getHeader("Authenticated");
        if (StringUtils.isBlank(token))
            token = request.getHeader("Authorization");
        return normalizeBearer(token);
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
            return token.substring(7).trim();
        return token;
    }
}
