package cn.org.autumn.modules.sys.shiro;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.org.autumn.modules.sys.entity.SysUserEntity;
import cn.org.autumn.modules.sys.service.SysConfigService;
import cn.org.autumn.modules.sys.service.SysShiroSessionService;
import cn.org.autumn.modules.sys.service.SysUserService;
import cn.org.autumn.modules.usr.service.UserProfileService;
import com.google.gson.Gson;
import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import org.apache.shiro.session.Session;
import org.apache.shiro.session.mgt.SimpleSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class RedisShiroSessionDAOTest {

    @Mock
    private RedisTemplate redisTemplate;

    @Mock
    private ValueOperations valueOperations;

    @Mock
    private SysShiroSessionService sysShiroSessionService;

    @Mock
    private SysUserService sysUserService;

    @Mock
    private UserProfileService userProfileService;

    @Mock
    private SysConfigService sysConfigService;

    @Mock
    private Gson gson;

    @InjectMocks
    private RedisShiroSessionDAO dao;

    @BeforeEach
    void clearLocalCache() {
        RedisShiroSessionDAO.cache.clear();
        RedisShiroSessionDAO.update.clear();
        RedisShiroSessionDAO.userPersisted.clear();
        lenient().when(sysConfigService.getNameSpace()).thenReturn("testns");
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(redisTemplate.execute(any(), any(RedisSerializer.class), any(RedisSerializer.class), anyList(), any()))
                .thenReturn(1L);
    }

    @Test
    void doReadSession_backfillsFromDbWhenRedisMiss() {
        SimpleSession fromDb = new SimpleSession();
        fromDb.setId("backfill-1");
        when(valueOperations.get(anyString())).thenReturn(null);
        when(sysShiroSessionService.getValidBySessionId("backfill-1")).thenReturn(fromDb);

        Session loaded = ReflectionTestUtils.invokeMethod(dao, "doReadSession", "backfill-1");

        assertSame(fromDb, loaded);
        assertSame(fromDb, RedisShiroSessionDAO.cache.get("backfill-1"));
        verify(valueOperations).set(anyString(), eq(fromDb));
        verify(sysShiroSessionService).getValidBySessionId("backfill-1");
    }

    @Test
    void doReadSession_skipsDbWhenRedisHit() {
        SimpleSession fromRedis = new SimpleSession();
        fromRedis.setId("redis-1");
        when(valueOperations.get(anyString())).thenReturn(fromRedis);

        Session loaded = ReflectionTestUtils.invokeMethod(dao, "doReadSession", "redis-1");

        assertSame(fromRedis, loaded);
        verify(sysShiroSessionService, never()).getValidBySessionId(any());
    }

    @Test
    void doDelete_removesDbRow() {
        SimpleSession session = new SimpleSession();
        session.setId("del-1");
        RedisShiroSessionDAO.cache.put("del-1", session);
        when(redisTemplate.delete(anyString())).thenReturn(true);

        ReflectionTestUtils.invokeMethod(dao, "doDelete", session);

        assertNull(RedisShiroSessionDAO.cache.get("del-1"));
        verify(sysShiroSessionService).deleteBySessionId(session.getId());
        verify(redisTemplate).delete(anyString());
    }

    @Test
    void doCreate_writesRedisButSkipsDbWhenAnonymous() {
        SimpleSession session = new SimpleSession();

        Object id = ReflectionTestUtils.invokeMethod(dao, "doCreate", session);

        assertNotNull(id);
        assertEquals(String.valueOf(id), String.valueOf(session.getId()));
        verify(sysShiroSessionService, never()).saveOrUpdateBySessionId(any());
        verify(valueOperations).set(anyString(), eq(session));
    }

    @Test
    void refreshAfterLogin_persistsSevenDayExpireOnNativeSession() {
        SimpleSession nativeSession = new SimpleSession();
        nativeSession.setId("remember-1");
        RedisShiroSessionDAO.cache.put("remember-1", nativeSession);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // 模拟 Subject 代理：只转发 timeout/attribute，本身不可当作落库对象
        Session proxy = new Session() {
            @Override
            public Serializable getId() {
                return nativeSession.getId();
            }

            @Override
            public void setTimeout(long maxIdleTimeInMillis) {
                nativeSession.setTimeout(maxIdleTimeInMillis);
            }

            @Override
            public long getTimeout() {
                return nativeSession.getTimeout();
            }

            @Override
            public void setAttribute(Object key, Object value) {
                nativeSession.setAttribute(key, value);
            }

            @Override
            public Object getAttribute(Object key) {
                return nativeSession.getAttribute(key);
            }

            @Override
            public Date getStartTimestamp() {
                return nativeSession.getStartTimestamp();
            }

            @Override
            public Date getLastAccessTime() {
                return nativeSession.getLastAccessTime();
            }

            @Override
            public void touch() {
            }

            @Override
            public void stop() {
            }

            @Override
            public Collection<Object> getAttributeKeys() throws org.apache.shiro.session.InvalidSessionException {
                return nativeSession.getAttributeKeys();
            }

            @Override
            public Object removeAttribute(Object key) {
                return nativeSession.removeAttribute(key);
            }

            @Override
            public String getHost() {
                return nativeSession.getHost();
            }
        };

        SysUserEntity user = new SysUserEntity();
        user.setUuid("u-remember");
        nativeSession.setAttribute(org.apache.shiro.subject.support.DefaultSubjectContext.PRINCIPALS_SESSION_KEY,
                new org.apache.shiro.subject.SimplePrincipalCollection(user, "realm"));

        dao.refreshAfterLogin(proxy, true);

        verify(sysShiroSessionService).saveOrUpdateBySessionId(nativeSession);
        assertEquals(ShiroSessionTimeouts.REMEMBER_ME_TIMEOUT_MS, nativeSession.getTimeout());
        assertEquals(Boolean.TRUE, nativeSession.getAttribute(ShiroSessionTimeouts.REMEMBER_ME_ATTR));
    }
}
