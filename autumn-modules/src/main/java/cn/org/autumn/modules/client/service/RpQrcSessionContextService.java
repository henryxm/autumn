package cn.org.autumn.modules.client.service;

import cn.org.autumn.modules.sys.shiro.ShiroSessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/** 将服务端逻辑绑定到建票时记录的浏览器 Shiro Session。 */
@Service
public class RpQrcSessionContextService {

    @Autowired
    private ShiroSessionService shiroSessionService;

    public boolean runWithBrowserSession(String sessionId, Runnable action) {
        if (action == null || org.apache.commons.lang3.StringUtils.isBlank(sessionId)) {
            return false;
        }
        org.apache.shiro.session.Session session = shiroSessionService.readSessionById(sessionId);
        if (session == null) {
            return false;
        }
        org.apache.shiro.subject.Subject subject = shiroSessionService.buildSubjectForSession(session);
        if (subject == null) {
            return false;
        }
        org.apache.shiro.subject.Subject previous = org.apache.shiro.util.ThreadContext.getSubject();
        try {
            org.apache.shiro.util.ThreadContext.bind(subject);
            action.run();
            return true;
        } finally {
            if (previous != null) {
                org.apache.shiro.util.ThreadContext.bind(previous);
            } else {
                org.apache.shiro.util.ThreadContext.unbindSubject();
            }
        }
    }
}
