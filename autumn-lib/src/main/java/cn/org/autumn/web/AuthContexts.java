package cn.org.autumn.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 请求级 {@link AuthContext} 的绑定与读取。
 * <p>
 * 绑定失败不抛、不阻断业务。用法见 {@code docs/AI_AUTH_CONTEXT.md}。
 */
public final class AuthContexts {

    public static final String REQUEST_ATTRIBUTE = "autumn.web.AuthContext";

    /** 短于该长度的令牌不写 prefix，避免把接近全文的 token 写入上下文 */
    private static final int MIN_TOKEN_LENGTH_FOR_PREFIX = 16;
    private static final int TOKEN_PREFIX_LEN = 8;

    private AuthContexts() {
    }

    public static void bind(HttpServletRequest request, AuthContext context) {
        if (request == null || context == null)
            return;
        try {
            request.setAttribute(REQUEST_ATTRIBUTE, context);
        } catch (Exception ignored) {
        }
    }

    public static AuthContext get(HttpServletRequest request) {
        if (request == null)
            return null;
        try {
            Object attr = request.getAttribute(REQUEST_ATTRIBUTE);
            if (attr instanceof AuthContext)
                return (AuthContext) attr;
        } catch (Exception ignored) {
        }
        return null;
    }

    public static AuthContext current() {
        try {
            RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
            if (!(attributes instanceof ServletRequestAttributes))
                return null;
            return get(((ServletRequestAttributes) attributes).getRequest());
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * 令牌可安全记入上下文的前缀：仅当长度 ≥ {@value #MIN_TOKEN_LENGTH_FOR_PREFIX} 时取前 {@value #TOKEN_PREFIX_LEN} 位；
     * 短令牌返回空串，避免整段 token 落入日志/属性。
     */
    public static String tokenPrefix(String token) {
        if (token == null || token.isEmpty())
            return "";
        String t = token.trim();
        if (t.length() < MIN_TOKEN_LENGTH_FOR_PREFIX)
            return "";
        return t.substring(0, TOKEN_PREFIX_LEN);
    }

    /**
     * 组装并绑定；不二次查库。accessKey / header / prefix 由调用方填好。
     */
    public static AuthContext bind(HttpServletRequest request, AuthSource source, String userUuid, String actorUuid, String accessKey, String credentialHeader, String tokenForPrefix, String hintUuid) {
        AuthContext auth = AuthContext.of(source);
        auth.userUuid(userUuid);
        if (actorUuid != null && !actorUuid.isBlank())
            auth.actorUuid(actorUuid);
        if (accessKey != null && !accessKey.isBlank())
            auth.accessKey(accessKey);
        if (credentialHeader != null && !credentialHeader.isBlank())
            auth.credentialHeader(credentialHeader);
        auth.tokenPrefix(tokenPrefix(tokenForPrefix));
        auth.hint(hintUuid);
        bind(request, auth);
        return auth;
    }
}
