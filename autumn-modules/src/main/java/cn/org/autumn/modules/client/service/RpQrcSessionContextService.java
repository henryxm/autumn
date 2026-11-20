package cn.org.autumn.modules.client.service;

import cn.org.autumn.modules.sys.shiro.ShiroSessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.session.Session;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;

/** 将服务端逻辑绑定到建票时记录的浏览器 Shiro Session。 */
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
            return false;
        }
        Subject subject = shiroSessionService.buildSubjectForSession(session);
        if (subject == null) {
            return false;
        }
        Subject previous = ThreadContext.getSubject();
        try {
            ThreadContext.bind(subject);
            action.run();
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
