package cn.org.autumn.modules.client.service;

import cn.org.autumn.modules.sys.shiro.SessionFixationSupport;
import cn.org.autumn.modules.sys.shiro.ShiroSessionService;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.session.Session;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 将服务端逻辑绑定到建票时记录的浏览器 Shiro Session（模式 D inbound）。
 * Webhook 无法 Set-Cookie，action 须在 {@link SessionFixationSupport#runWithoutRegeneration} 内<strong>同步</strong>执行；
 * 禁止 action 内异步登录（ThreadLocal 不跨线程）。
 */
@Slf4j
@Service
public class RpQrcSessionContextService {

    @Autowired
    private ShiroSessionService shiroSessionService;

    public boolean runWithBrowserSession(String sessionId, Runnable action) {
        if (action == null || StringUtils.isBlank(sessionId)) {
            return false;
        }
        Session session = shiroSessionService.readSessionById(sessionId);
        if (session == null) {
            log.warn("RP QRC browser session missing sessionId={}", SessionFixationSupport.briefSessionId(sessionId));
            return false;
        }
        Subject subject = shiroSessionService.buildSubjectForSession(session);
        if (subject == null) {
            log.warn("RP QRC browser subject build failed sessionId={}", SessionFixationSupport.briefSessionId(sessionId));
            return false;
        }
        Subject previous = ThreadContext.getSubject();
        try {
            ThreadContext.bind(subject);
            Object beforeId = SessionFixationSupport.currentSessionId(subject);
            final Object[] afterId = new Object[1];
            final boolean[] authenticated = new boolean[1];
            SessionFixationSupport.runWithoutRegeneration(() -> {
                action.run();
                afterId[0] = SessionFixationSupport.currentSessionId(subject);
                authenticated[0] = subject.isAuthenticated();
            });
            Object after = afterId[0];
            String briefBefore = SessionFixationSupport.briefSessionId(sessionId);
            String briefSubjectBefore = SessionFixationSupport.briefSessionId(beforeId);
            String briefAfter = SessionFixationSupport.briefSessionId(after);
            if (!Objects.equals(String.valueOf(beforeId), String.valueOf(after))) {
                log.warn("RP QRC browser session id changed (fixation suppress failed?) sessionId={} subjectBefore={} after={} authenticated={}",
                        briefBefore, briefSubjectBefore, briefAfter, authenticated[0]);
            } else if (!Objects.equals(String.valueOf(sessionId), String.valueOf(beforeId))) {
                log.warn("RP QRC browser session id mismatch arg={} subject={} authenticated={}",
                        briefBefore, briefSubjectBefore, authenticated[0]);
            } else {
                log.info("RP QRC browser session ok sessionId={} authenticated={}", briefBefore, authenticated[0]);
            }
            return true;
        } finally {
            if (previous != null) {
                ThreadContext.bind(previous);
            } else {
                ThreadContext.unbindSubject();
            }
        }
    }
}
