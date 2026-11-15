package cn.org.autumn.modules.sys.shiro;

import org.apache.shiro.session.Session;
import org.apache.shiro.subject.Subject;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * 登录成功后换发 SessionId，缓解 Session Fixation。
 */
public final class SessionFixationSupport {

    private SessionFixationSupport() {
    }

    public static void regenerate(Subject subject) {
        if (subject == null) {
            return;
        }
        Session session = subject.getSession(false);
        if (session == null) {
            subject.getSession(true);
            return;
        }
        Map<Object, Object> attrs = new HashMap<>();
        for (Object key : session.getAttributeKeys()) {
            attrs.put(key, session.getAttribute(key));
        }
        long timeout = session.getTimeout();
        try {
            session.stop();
        } catch (Exception ignored) {
        }
        Session neu = subject.getSession(true);
        neu.setTimeout(timeout);
        for (Map.Entry<Object, Object> e : attrs.entrySet()) {
            neu.setAttribute(e.getKey(), e.getValue());
        }
    }

    public static Serializable currentSessionId(Subject subject) {
        if (subject == null) {
            return null;
        }
        Session session = subject.getSession(false);
        return session == null ? null : session.getId();
    }
}
