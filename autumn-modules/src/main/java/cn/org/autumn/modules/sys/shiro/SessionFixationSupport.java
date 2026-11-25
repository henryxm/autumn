package cn.org.autumn.modules.sys.shiro;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.session.Session;
import org.apache.shiro.subject.Subject;

/**
 * 登录成功后换发 SessionId，缓解 Session Fixation。
 * <p>
 * <strong>RP 联邦 inbound 例外</strong>：Webhook 响应无法给浏览器 Set-Cookie，换发会停掉 Cookie 中的旧 Session
 * 导致「确认后跳转未登录」。此时须经 {@link #runWithoutRegeneration} 抑制换发（见 {@code RpQrcSessionContextService}）。
 * <p>
 * <strong>红线</strong>：{@link #runWithoutRegeneration} 内的 action <strong>禁止</strong>异步登录
 * （{@code @Async} / 线程池 / {@code CompletableFuture}）：ThreadLocal 不跨线程，子线程会静默换发。
 */
@Slf4j
public final class SessionFixationSupport {

    private static final ThreadLocal<Boolean> SUPPRESS_REGENERATION = new ThreadLocal<>();

    private SessionFixationSupport() {
    }

    /** 在 runnable 期间 {@link #regenerate} 直接跳过（RP inbound 代浏览器 Session 登录；须同步完成）。 */
    public static void runWithoutRegeneration(Runnable runnable) {
        if (runnable == null) {
            return;
        }
        Boolean previous = SUPPRESS_REGENERATION.get();
        SUPPRESS_REGENERATION.set(Boolean.TRUE);
        try {
            runnable.run();
        } finally {
            if (previous == null) {
                SUPPRESS_REGENERATION.remove();
            } else {
                SUPPRESS_REGENERATION.set(previous);
            }
        }
    }

    public static boolean isRegenerationSuppressed() {
        return Boolean.TRUE.equals(SUPPRESS_REGENERATION.get());
    }

    public static void regenerate(Subject subject) {
        if (subject == null) {
            return;
        }
        if (isRegenerationSuppressed()) {
            log.debug("SessionFixation skipped (RP inbound) sessionId={}", briefSessionId(currentSessionId(subject)));
            return;
        }
        Session session = subject.getSession(false);
        if (session == null) {
            subject.getSession(true);
            return;
        }
        Serializable before = session.getId();
        Map<Object, Object> attrs = new HashMap<>();
        for (Object key : session.getAttributeKeys()) {
            attrs.put(key, session.getAttribute(key));
        }
        long timeout = session.getTimeout();
        try {
            session.stop();
        } catch (Exception e) {
            log.debug("SessionFixation stop old session failed before={}: {}", briefSessionId(before), e.getMessage());
        }
        Session neu = subject.getSession(true);
        neu.setTimeout(timeout);
        for (Map.Entry<Object, Object> e : attrs.entrySet()) {
            neu.setAttribute(e.getKey(), e.getValue());
        }
        log.debug("SessionFixation regenerated before={} after={}", briefSessionId(before), briefSessionId(neu.getId()));
    }

    public static Serializable currentSessionId(Subject subject) {
        if (subject == null) {
            return null;
        }
        Session session = subject.getSession(false);
        return session == null ? null : session.getId();
    }

    /** 日志用：不输出完整 SessionId，降低日志面劫持风险。 */
    public static String briefSessionId(Object sessionId) {
        if (sessionId == null) {
            return null;
        }
        String raw = String.valueOf(sessionId);
        if (raw.isEmpty() || "null".equals(raw)) {
            return null;
        }
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] dig = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(12);
            for (int i = 0; i < 4 && i < dig.length; i++) {
                sb.append(String.format("%02x", dig[i]));
            }
            return sb.append("…").append(Math.min(raw.length(), 64)).toString();
        } catch (Exception e) {
            return "sid*";
        }
    }
}
