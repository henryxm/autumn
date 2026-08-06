package cn.org.autumn.modules.sys.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.org.autumn.modules.sys.dao.SysShiroSessionDao;
import cn.org.autumn.modules.sys.entity.SysShiroSessionEntity;
import cn.org.autumn.modules.sys.entity.SysUserEntity;
import cn.org.autumn.modules.sys.shiro.ShiroSessionTimeouts;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.Date;
import org.apache.shiro.session.Session;
import org.apache.shiro.session.mgt.SimpleSession;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.apache.shiro.subject.support.DefaultSubjectContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class SysShiroSessionServiceTest {

    @Mock
    private SysShiroSessionDao baseMapper;

    @InjectMocks
    private SysShiroSessionService service;

    @BeforeEach
    void wireMapper() {
        ReflectionTestUtils.setField(service, "baseMapper", baseMapper);
    }

    @Test
    void serializeRoundTrip_preservesSessionId() {
        SimpleSession session = new SimpleSession();
        session.setId("sid-round");
        session.setTimeout(ShiroSessionTimeouts.SESSION_TIMEOUT_MS);
        String payload = SysShiroSessionService.serializeSession(session);
        assertNotNull(payload);
        Session restored = SysShiroSessionService.deserializeSession(payload);
        assertNotNull(restored);
        assertEquals("sid-round", String.valueOf(restored.getId()));
    }

    @Test
    void getValidBySessionId_returnsNullWhenExpiredAndDeletes() {
        SysShiroSessionEntity row = new SysShiroSessionEntity();
        row.setSessionId("expired-1");
        row.setPayload(SysShiroSessionService.serializeSession(sampleSession("expired-1")));
        row.setExpireTime(new Date(System.currentTimeMillis() - 60_000L));
        when(baseMapper.getBySessionId("expired-1")).thenReturn(row);

        assertNull(service.getValidBySessionId("expired-1"));
        verify(baseMapper).deleteBySessionId("expired-1");
    }

    @Test
    void getValidBySessionId_returnsSessionWhenValid() {
        SimpleSession session = sampleSession("ok-1");
        SysShiroSessionEntity row = new SysShiroSessionEntity();
        row.setSessionId("ok-1");
        row.setPayload(SysShiroSessionService.serializeSession(session));
        row.setExpireTime(new Date(System.currentTimeMillis() + ShiroSessionTimeouts.SESSION_TIMEOUT_MS));
        when(baseMapper.getBySessionId("ok-1")).thenReturn(row);

        Session loaded = service.getValidBySessionId("ok-1");
        assertNotNull(loaded);
        assertEquals("ok-1", String.valueOf(loaded.getId()));
        verify(baseMapper, never()).deleteBySessionId(anyString());
    }

    @Test
    void saveOrUpdateBySessionId_insertsWhenMissing() {
        when(baseMapper.updateBySessionId(any(SysShiroSessionEntity.class))).thenReturn(0);
        when(baseMapper.insert(any(SysShiroSessionEntity.class))).thenReturn(1);
        SimpleSession session = sampleSession("new-1");
        bindUser(session, "user-new-1");

        service.saveOrUpdateBySessionId(session);

        ArgumentCaptor<SysShiroSessionEntity> captor = ArgumentCaptor.forClass(SysShiroSessionEntity.class);
        verify(baseMapper).updateBySessionId(captor.capture());
        verify(baseMapper).insert(captor.getValue());
        assertEquals("new-1", captor.getValue().getSessionId());
        assertEquals("user-new-1", captor.getValue().getUser());
        assertNotNull(captor.getValue().getPayload());
        assertNotNull(captor.getValue().getExpireTime());
    }

    @Test
    void saveOrUpdateBySessionId_updatesWhenExists() {
        when(baseMapper.updateBySessionId(any(SysShiroSessionEntity.class))).thenReturn(1);
        SimpleSession session = sampleSession("exist-1");
        bindUser(session, "user-exist-1");

        service.saveOrUpdateBySessionId(session);

        verify(baseMapper).updateBySessionId(any(SysShiroSessionEntity.class));
        verify(baseMapper, never()).insert(any(SysShiroSessionEntity.class));
        verify(baseMapper, never()).getBySessionId(anyString());
    }

    @Test
    void saveOrUpdateBySessionId_retriesUpdateOnDuplicateKey() {
        when(baseMapper.updateBySessionId(any(SysShiroSessionEntity.class))).thenReturn(0, 1);
        when(baseMapper.insert(any(SysShiroSessionEntity.class)))
                .thenThrow(new DuplicateKeyException("dup", new SQLIntegrityConstraintViolationException("Duplicate entry")));
        SimpleSession session = sampleSession("race-1");
        bindUser(session, "user-race-1");

        service.saveOrUpdateBySessionId(session);

        verify(baseMapper).insert(any(SysShiroSessionEntity.class));
        verify(baseMapper, times(2)).updateBySessionId(any(SysShiroSessionEntity.class));
    }

    @Test
    void deleteBySessionIdReturning_reportsRows() {
        when(baseMapper.deleteBySessionId("x-1")).thenReturn(1);
        assertEquals(1, service.deleteBySessionIdReturning("x-1"));
    }

    @Test
    void findUserBySessionId_readsUserColumn() {
        SysShiroSessionEntity row = new SysShiroSessionEntity();
        row.setSessionId("x-2");
        row.setUser("user-x-2");
        when(baseMapper.getBySessionId("x-2")).thenReturn(row);
        assertEquals("user-x-2", service.findUserBySessionId("x-2"));
    }

    @Test
    void deleteExpiredAll_usesSingleSql() {
        when(baseMapper.deleteExpiredBefore(any(Date.class))).thenReturn(42);
        assertEquals(42, service.deleteExpiredAll());
        verify(baseMapper).deleteExpiredBefore(any(Date.class));
    }

    @Test
    void extractUserUuid_readsSysUserPrincipal() {
        SimpleSession session = sampleSession("u-1");
        session.setAttribute(DefaultSubjectContext.PRINCIPALS_SESSION_KEY,
                new SimplePrincipalCollection(sampleUser("user-uuid-1"), "realm"));
        assertEquals("user-uuid-1", SysShiroSessionService.extractUserUuid(session));
    }

    @Test
    void saveOrUpdateBySessionId_setsUserFromPrincipal() {
        when(baseMapper.updateBySessionId(any(SysShiroSessionEntity.class))).thenReturn(0);
        when(baseMapper.insert(any(SysShiroSessionEntity.class))).thenReturn(1);
        SimpleSession session = sampleSession("u-3");
        session.setAttribute(DefaultSubjectContext.PRINCIPALS_SESSION_KEY,
                new SimplePrincipalCollection(sampleUser("user-uuid-3"), "realm"));

        service.saveOrUpdateBySessionId(session);

        ArgumentCaptor<SysShiroSessionEntity> captor = ArgumentCaptor.forClass(SysShiroSessionEntity.class);
        verify(baseMapper).insert(captor.capture());
        assertEquals("user-uuid-3", captor.getValue().getUser());
    }

    @Test
    void saveOrUpdateBySessionId_skipsWhenNoUserPrincipal() {
        SimpleSession session = sampleSession("anon-1");
        service.saveOrUpdateBySessionId(session);
        verify(baseMapper, never()).updateBySessionId(any(SysShiroSessionEntity.class));
        verify(baseMapper, never()).insert(any(SysShiroSessionEntity.class));
    }

    @Test
    void saveOrUpdateBySessionId_expireUsesOneDayEvenIfSessionTimeoutIsDefault30m() {
        when(baseMapper.updateBySessionId(any(SysShiroSessionEntity.class))).thenReturn(0);
        when(baseMapper.insert(any(SysShiroSessionEntity.class))).thenReturn(1);
        SimpleSession session = sampleSession("t-30");
        session.setTimeout(30L * 60 * 1000);
        bindUser(session, "user-t-30");

        long before = System.currentTimeMillis();
        service.saveOrUpdateBySessionId(session);
        long after = System.currentTimeMillis();

        ArgumentCaptor<SysShiroSessionEntity> captor = ArgumentCaptor.forClass(SysShiroSessionEntity.class);
        verify(baseMapper).insert(captor.capture());
        long expireAt = captor.getValue().getExpireTime().getTime();
        assertTrue(expireAt >= before + ShiroSessionTimeouts.SESSION_TIMEOUT_MS - 1000);
        assertTrue(expireAt <= after + ShiroSessionTimeouts.SESSION_TIMEOUT_MS + 1000);
    }

    @Test
    void saveOrUpdateBySessionId_expireUsesSevenDaysWhenRememberMe() {
        when(baseMapper.updateBySessionId(any(SysShiroSessionEntity.class))).thenReturn(0);
        when(baseMapper.insert(any(SysShiroSessionEntity.class))).thenReturn(1);
        SimpleSession session = sampleSession("t-7");
        bindUser(session, "user-t-7");
        ShiroSessionTimeouts.applyRememberMe(session, true);

        long before = System.currentTimeMillis();
        service.saveOrUpdateBySessionId(session);
        long after = System.currentTimeMillis();

        ArgumentCaptor<SysShiroSessionEntity> captor = ArgumentCaptor.forClass(SysShiroSessionEntity.class);
        verify(baseMapper).insert(captor.capture());
        long expireAt = captor.getValue().getExpireTime().getTime();
        assertTrue(expireAt >= before + ShiroSessionTimeouts.REMEMBER_ME_TIMEOUT_MS - 1000);
        assertTrue(expireAt <= after + ShiroSessionTimeouts.REMEMBER_ME_TIMEOUT_MS + 1000);
    }

    private static SimpleSession sampleSession(String id) {
        SimpleSession session = new SimpleSession();
        session.setId(id);
        session.setTimeout(ShiroSessionTimeouts.SESSION_TIMEOUT_MS);
        session.setLastAccessTime(new Date());
        return session;
    }

    private static void bindUser(SimpleSession session, String uuid) {
        session.setAttribute(DefaultSubjectContext.PRINCIPALS_SESSION_KEY,
                new SimplePrincipalCollection(sampleUser(uuid), "realm"));
    }

    private static SysUserEntity sampleUser(String uuid) {
        SysUserEntity user = new SysUserEntity();
        user.setUuid(uuid);
        return user;
    }
}
