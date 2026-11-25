package cn.org.autumn.modules.sys.shiro;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.shiro.session.InvalidSessionException;
import org.apache.shiro.session.Session;
import org.apache.shiro.subject.Subject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SessionFixationSupportTest {

    @Test
    void regenerate_suppressed_doesNotStopSession() {
        Subject subject = mock(Subject.class);
        Session session = mock(Session.class);
        when(subject.getSession(false)).thenReturn(session);
        when(session.getId()).thenReturn("sid-1");

        SessionFixationSupport.runWithoutRegeneration(() -> {
            assertTrue(SessionFixationSupport.isRegenerationSuppressed());
            SessionFixationSupport.regenerate(subject);
        });

        assertFalse(SessionFixationSupport.isRegenerationSuppressed());
        verify(session, never()).stop();
        verify(subject, never()).getSession(true);
    }

    @Test
    void regenerate_withoutSuppress_stopsAndCreatesNew() {
        Subject subject = mock(Subject.class);
        MutableIdSession old = new MutableIdSession("old-id");
        MutableIdSession neu = new MutableIdSession("new-id");
        when(subject.getSession(false)).thenReturn(old);
        when(subject.getSession(true)).thenReturn(neu);

        SessionFixationSupport.regenerate(subject);

        assertTrue(old.stopped);
        assertEquals("new-id", neu.getId());
        assertNotEquals("old-id", neu.getId());
    }

    @Test
    void runWithoutRegeneration_restoresPreviousFlag() {
        AtomicReference<Boolean> mid = new AtomicReference<>();
        SessionFixationSupport.runWithoutRegeneration(() -> {
            SessionFixationSupport.runWithoutRegeneration(() -> mid.set(SessionFixationSupport.isRegenerationSuppressed()));
            assertTrue(SessionFixationSupport.isRegenerationSuppressed());
        });
        assertTrue(mid.get());
        assertFalse(SessionFixationSupport.isRegenerationSuppressed());
    }

    @Test
    void briefSessionId_doesNotEchoRaw() {
        String brief = SessionFixationSupport.briefSessionId("secret-session-cookie-value");
        assertNotNull(brief);
        assertFalse(brief.contains("secret-session"));
        assertTrue(brief.contains("…"));
    }

    /** 最小可变 Session，避免 mock stop/getSession 语义纠缠。 */
    private static final class MutableIdSession implements Session {
        private final Serializable id;
        private final Map<Object, Object> attrs = new HashMap<>();
        private long timeout = 1800000L;
        boolean stopped;

        MutableIdSession(Serializable id) {
            this.id = id;
        }

        @Override
        public Serializable getId() {
            return id;
        }

        @Override
        public java.util.Date getStartTimestamp() {
            return null;
        }

        @Override
        public java.util.Date getLastAccessTime() {
            return null;
        }

        @Override
        public long getTimeout() throws InvalidSessionException {
            return timeout;
        }

        @Override
        public void setTimeout(long maxIdleTimeInMillis) throws InvalidSessionException {
            this.timeout = maxIdleTimeInMillis;
        }

        @Override
        public String getHost() {
            return null;
        }

        @Override
        public void touch() throws InvalidSessionException {
        }

        @Override
        public void stop() throws InvalidSessionException {
            stopped = true;
        }

        @Override
        public Collection<Object> getAttributeKeys() throws InvalidSessionException {
            return Collections.unmodifiableCollection(attrs.keySet());
        }

        @Override
        public Object getAttribute(Object key) throws InvalidSessionException {
            return attrs.get(key);
        }

        @Override
        public void setAttribute(Object key, Object value) throws InvalidSessionException {
            attrs.put(key, value);
        }

        @Override
        public Object removeAttribute(Object key) throws InvalidSessionException {
            return attrs.remove(key);
        }
    }
}
