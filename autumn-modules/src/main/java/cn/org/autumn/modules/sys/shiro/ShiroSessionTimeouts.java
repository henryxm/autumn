package cn.org.autumn.modules.sys.shiro;

import org.apache.shiro.session.Session;

/**
 * Shiro Web Session 与 Redis TTL 统一常量，避免多处漂移。
 */
public final class ShiroSessionTimeouts {

    private ShiroSessionTimeouts() {
    }

    /** Session 属性：本次登录是否勾选 RememberMe。 */
    public static final String REMEMBER_ME_ATTR = "autumn.session.rememberMe";

    /** Shiro {@code globalSessionTimeout}：未勾选记住我时 1 天。 */
    public static final long SESSION_TIMEOUT_MS = 24L * 60 * 60 * 1000;

    /** 勾选记住我后的 Session 空闲超时：与 RememberMe Cookie 对齐为 7 天。 */
    public static final long REMEMBER_ME_TIMEOUT_MS = 7L * 24 * 60 * 60 * 1000;

    /** Redis Session key TTL（天）：普通登录 1 天。 */
    public static final long SESSION_REDIS_TTL_DAYS = 1L;

    /** Redis Session key TTL（天）：记住我 7 天。 */
    public static final long REMEMBER_ME_REDIS_TTL_DAYS = 7L;

    /** SessionId Cookie maxAge（秒）：1 天。 */
    public static final int SESSION_ID_COOKIE_MAX_AGE_SEC = 24 * 60 * 60;

    /** SessionId Cookie maxAge（秒）：记住我 7 天。 */
    public static final int REMEMBER_ME_COOKIE_MAX_AGE_SEC = 7 * 24 * 60 * 60;

    /** 登录成功后按是否记住我设置 Session 超时与标记。 */
    public static void applyRememberMe(Session session, boolean rememberMe) {
        if (session == null) {
            return;
        }
        session.setAttribute(REMEMBER_ME_ATTR, rememberMe);
        session.setTimeout(rememberMe ? REMEMBER_ME_TIMEOUT_MS : SESSION_TIMEOUT_MS);
    }

    /** 落库 / Redis TTL 使用的超时毫秒：优先 RememberMe 标记，否则按 timeout 识别 7 天，默认 1 天。 */
    public static long resolveTimeoutMs(Session session) {
        if (session == null) {
            return SESSION_TIMEOUT_MS;
        }
        Object flag = session.getAttribute(REMEMBER_ME_ATTR);
        if (Boolean.TRUE.equals(flag) || "true".equalsIgnoreCase(String.valueOf(flag))) {
            return REMEMBER_ME_TIMEOUT_MS;
        }
        long t = session.getTimeout();
        if (t == REMEMBER_ME_TIMEOUT_MS) {
            return REMEMBER_ME_TIMEOUT_MS;
        }
        return SESSION_TIMEOUT_MS;
    }

    public static long resolveRedisTtlDays(Session session) {
        return resolveTimeoutMs(session) >= REMEMBER_ME_TIMEOUT_MS
                ? REMEMBER_ME_REDIS_TTL_DAYS
                : SESSION_REDIS_TTL_DAYS;
    }

    /**
     * 纠正异常默认超时（如 create 时尚为 Shiro 默认 30 分钟），保留已设置的 1 天 / 7 天。
     */
    public static void normalizeTimeout(Session session) {
        if (session == null) {
            return;
        }
        long resolved = resolveTimeoutMs(session);
        if (session.getTimeout() != resolved) {
            session.setTimeout(resolved);
        }
    }
}
