package cn.org.autumn.modules.sys.support;

import cn.org.autumn.modules.sys.shiro.ShiroUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.session.Session;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * Cookie 会话 CSRF：Session 绑定一次性令牌，请求头 {@code X-CSRF-Token}。
 */
public final class CsrfTokenSupport {

    public static final String HEADER = "X-CSRF-Token";
    public static final String SESSION_KEY = "autumn.csrf.token";

    private static final SecureRandom RANDOM = new SecureRandom();

    private CsrfTokenSupport() {
    }

    public static String issue() {
        Session session = ShiroUtils.getSession();
        byte[] buf = new byte[32];
        RANDOM.nextBytes(buf);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
        session.setAttribute(SESSION_KEY, token);
        return token;
    }

    public static String peekOrIssue() {
        Session session = ShiroUtils.getSession();
        Object existing = session.getAttribute(SESSION_KEY);
        if (existing != null && StringUtils.isNotBlank(existing.toString())) {
            return existing.toString();
        }
        return issue();
    }

    public static boolean validate(String submitted) {
        if (StringUtils.isBlank(submitted)) {
            return false;
        }
        Session session = ShiroUtils.getSession();
        Object expected = session.getAttribute(SESSION_KEY);
        if (expected == null) {
            return false;
        }
        return constantTimeEquals(expected.toString(), submitted.trim());
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null || a.length() != b.length()) {
            return false;
        }
        int diff = 0;
        for (int i = 0; i < a.length(); i++) {
            diff |= a.charAt(i) ^ b.charAt(i);
        }
        return diff == 0;
    }
}
