package cn.org.autumn.modules.oauth.oauth2.support;

import cn.org.autumn.utils.Uuid;
import java.util.concurrent.ConcurrentHashMap;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.apache.commons.lang.StringUtils;

/**
 * OAuth/OPL 授权确认页 CSRF 防护：session 绑定一次性 token。
 */
public final class OAuthConsentCsrfSupport {

    private static final String SESSION_KEY = "oauth_consent_csrf_tokens";
    private static final long TTL_MS = 10 * 60 * 1000L;

    private OAuthConsentCsrfSupport() {
    }

    public static String issue(HttpServletRequest request) {
        if (request == null) {
            return "";
        }
        String token = Uuid.uuid().replace("-", "");
        HttpSession session = request.getSession(true);
        @SuppressWarnings("unchecked")
        ConcurrentHashMap<String, Long> tokens = (ConcurrentHashMap<String, Long>) session.getAttribute(SESSION_KEY);
        if (tokens == null) {
            tokens = new ConcurrentHashMap<>();
            session.setAttribute(SESSION_KEY, tokens);
        }
        purgeExpired(tokens);
        tokens.put(token, System.currentTimeMillis() + TTL_MS);
        return token;
    }

    public static boolean validateAndConsume(HttpServletRequest request, String token) {
        if (request == null || StringUtils.isBlank(token)) {
            return false;
        }
        HttpSession session = request.getSession(false);
        if (session == null) {
            return false;
        }
        @SuppressWarnings("unchecked")
        ConcurrentHashMap<String, Long> tokens = (ConcurrentHashMap<String, Long>) session.getAttribute(SESSION_KEY);
        if (tokens == null) {
            return false;
        }
        purgeExpired(tokens);
        Long expireAt = tokens.remove(token.trim());
        return expireAt != null && expireAt >= System.currentTimeMillis();
    }

    private static void purgeExpired(ConcurrentHashMap<String, Long> tokens) {
        long now = System.currentTimeMillis();
        tokens.entrySet().removeIf(entry -> entry.getValue() == null || entry.getValue() < now);
    }
}
