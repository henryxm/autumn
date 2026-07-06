package cn.org.autumn.modules.opc.support;

import java.util.concurrent.ConcurrentHashMap;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.apache.commons.lang.StringUtils;

/**
 * OPC OAuth state 会话绑定：authorize 写入、callback 校验并 consume，防 CSRF。
 */
public final class OpenOAuthStateSupport {

    private static final String SESSION_KEY = "opc_oauth_state_map";
    private static final long TTL_MS = 10 * 60 * 1000L;

    private OpenOAuthStateSupport() {
    }

    public static void bindState(HttpServletRequest request, String appId, String state) {
        if (request == null || StringUtils.isBlank(appId) || StringUtils.isBlank(state)) {
            return;
        }
        HttpSession session = request.getSession(true);
        @SuppressWarnings("unchecked")
        ConcurrentHashMap<String, StateEntry> map = (ConcurrentHashMap<String, StateEntry>) session.getAttribute(SESSION_KEY);
        if (map == null) {
            map = new ConcurrentHashMap<>();
            session.setAttribute(SESSION_KEY, map);
        }
        purgeExpired(map);
        map.put(appId.trim(), new StateEntry(state.trim(), System.currentTimeMillis() + TTL_MS));
    }

    public static boolean consumeState(HttpServletRequest request, String appId, String state) {
        if (request == null || StringUtils.isBlank(appId) || StringUtils.isBlank(state)) {
            return false;
        }
        HttpSession session = request.getSession(false);
        if (session == null) {
            return false;
        }
        @SuppressWarnings("unchecked")
        ConcurrentHashMap<String, StateEntry> map = (ConcurrentHashMap<String, StateEntry>) session.getAttribute(SESSION_KEY);
        if (map == null) {
            return false;
        }
        purgeExpired(map);
        StateEntry entry = map.remove(appId.trim());
        return entry != null && entry.expireAt >= System.currentTimeMillis() && state.trim().equals(entry.state);
    }

    private static void purgeExpired(ConcurrentHashMap<String, StateEntry> map) {
        long now = System.currentTimeMillis();
        map.entrySet().removeIf(entry -> entry.getValue() == null || entry.getValue().expireAt < now);
    }

    private static final class StateEntry {
        private final String state;
        private final long expireAt;

        private StateEntry(String state, long expireAt) {
            this.state = state;
            this.expireAt = expireAt;
        }
    }
}
