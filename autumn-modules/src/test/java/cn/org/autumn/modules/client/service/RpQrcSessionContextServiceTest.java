package cn.org.autumn.modules.client.service;

import cn.org.autumn.modules.sys.shiro.SessionFixationSupport;
import cn.org.autumn.modules.sys.shiro.ShiroSessionService;
import java.io.Serializable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.shiro.session.Session;
import org.apache.shiro.subject.Subject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RpQrcSessionContextServiceTest {

    @Mock
    private ShiroSessionService shiroSessionService;

    @InjectMocks
    private RpQrcSessionContextService service;

    @Test
    void runWithBrowserSession_suppressesFixationDuringAction() {
        Session session = mock(Session.class);
        Subject subject = mock(Subject.class);
        when(shiroSessionService.readSessionById("sid-a")).thenReturn(session);
        when(shiroSessionService.buildSubjectForSession(session)).thenReturn(subject);
        when(subject.getSession(false)).thenReturn(session);
        when(session.getId()).thenReturn("sid-a");
        when(subject.isAuthenticated()).thenReturn(true);

        AtomicBoolean suppressedInside = new AtomicBoolean(false);
        AtomicReference<Serializable> idDuring = new AtomicReference<>();
        boolean ok = service.runWithBrowserSession("sid-a", () -> {
            suppressedInside.set(SessionFixationSupport.isRegenerationSuppressed());
            SessionFixationSupport.regenerate(subject);
            idDuring.set(SessionFixationSupport.currentSessionId(subject));
        });

        assertTrue(ok);
        assertTrue(suppressedInside.get());
        assertEquals("sid-a", idDuring.get());
        assertFalse(SessionFixationSupport.isRegenerationSuppressed());
    }

    @Test
    void runWithBrowserSession_missingSession_returnsFalse() {
        when(shiroSessionService.readSessionById(any())).thenReturn(null);
        assertFalse(service.runWithBrowserSession("gone", () -> {
        }));
    }
}
