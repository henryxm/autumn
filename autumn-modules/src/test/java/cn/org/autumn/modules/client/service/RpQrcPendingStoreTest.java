package cn.org.autumn.modules.client.service;

import cn.org.autumn.modules.client.model.RpQrcPendingSession;
import cn.org.autumn.modules.sys.service.SysConfigService;
import cn.org.autumn.thread.TagRunnable;
import cn.org.autumn.thread.TagTaskExecutor;
import cn.org.autumn.utils.RedisUtils;
import com.alibaba.fastjson2.JSON;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RpQrcPendingStoreTest {

    @Mock
    private SysConfigService sysConfigService;

    @Mock
    private TagTaskExecutor asyncTaskExecutor;

    @Mock
    private RedisUtils redisUtils;

    @InjectMocks
    private RpQrcPendingStore rpQrcPendingStore;

    @Test
    void onThirtySecond_enqueuesThenAsyncDrainRemovesExpired() {
        when(redisUtils.isAvailable()).thenReturn(false);
        doAnswer(invocation -> {
            TagRunnable task = invocation.getArgument(0);
            task.run();
            return true;
        }).when(asyncTaskExecutor).execute(any(TagRunnable.class));

        RpQrcPendingSession live = new RpQrcPendingSession();
        live.setUuid("live-1");
        live.setStatus("PENDING");
        live.setExpiredAt(System.currentTimeMillis() + 60_000L);
        rpQrcPendingStore.save(live);

        RpQrcPendingSession expired = new RpQrcPendingSession();
        expired.setUuid("expired-1");
        expired.setStatus("COMPLETED");
        expired.setExpiredAt(System.currentTimeMillis() - 1_000L);
        rpQrcPendingStore.save(expired);

        rpQrcPendingStore.onThirtySecond();

        verify(asyncTaskExecutor).execute(any(TagRunnable.class));
        assertNotNull(rpQrcPendingStore.get("live-1"));
        assertNull(rpQrcPendingStore.get("expired-1"));
    }

    @Test
    void enqueueExpiredKeys_onlyMarksMemoryWithoutRemoving() {
        when(redisUtils.isAvailable()).thenReturn(false);

        RpQrcPendingSession expired = new RpQrcPendingSession();
        expired.setUuid("expired-2");
        expired.setStatus("COMPLETED");
        expired.setExpiredAt(System.currentTimeMillis() - 1_000L);
        rpQrcPendingStore.save(expired);

        rpQrcPendingStore.enqueueExpiredKeys();

        assertNotNull(rpQrcPendingStore.get("expired-2"));

        rpQrcPendingStore.drainExpired();
        assertNull(rpQrcPendingStore.get("expired-2"));
    }

    @Test
    void saveOrFail_withoutRedis_rollsBackAndThrows() {
        when(redisUtils.isAvailable()).thenReturn(false);

        RpQrcPendingSession session = new RpQrcPendingSession();
        session.setUuid("ghost-1");
        session.setStatus("PENDING");
        session.setExpiredAt(System.currentTimeMillis() + 60_000L);

        assertThrows(IllegalStateException.class, () -> rpQrcPendingStore.saveOrFail(session));
        assertNull(rpQrcPendingStore.get("ghost-1"));
        verify(redisUtils, never()).trySet(anyString(), any(), anyLong());
    }

    @Test
    void saveOrFail_redisWriteError_rollsBackMemory() {
        when(redisUtils.isAvailable()).thenReturn(true);
        when(redisUtils.trySet(anyString(), any(), anyLong())).thenThrow(new RuntimeException("redis down"));

        RpQrcPendingSession session = new RpQrcPendingSession();
        session.setUuid("fail-1");
        session.setStatus("PENDING");
        session.setExpiredAt(System.currentTimeMillis() + 60_000L);

        assertThrows(IllegalStateException.class, () -> rpQrcPendingStore.saveOrFail(session));
        assertNull(rpQrcPendingStore.get("fail-1"));
    }

    @Test
    void saveOrFail_trySetFalse_rollsBackMemory() {
        when(redisUtils.isAvailable()).thenReturn(true);
        when(redisUtils.trySet(anyString(), any(), anyLong())).thenReturn(false);

        RpQrcPendingSession session = new RpQrcPendingSession();
        session.setUuid("skip-1");
        session.setStatus("PENDING");
        session.setExpiredAt(System.currentTimeMillis() + 60_000L);

        assertThrows(IllegalStateException.class, () -> rpQrcPendingStore.saveOrFail(session));
        assertNull(rpQrcPendingStore.get("skip-1"));
    }

    @Test
    void redisBacked_saveReadableAcrossLogicalNodes() {
        ConcurrentHashMap<String, Object> shared = new ConcurrentHashMap<>();
        when(redisUtils.isAvailable()).thenReturn(true);
        when(redisUtils.trySet(anyString(), any(), anyLong())).thenAnswer(invocation -> {
            shared.put(invocation.getArgument(0), invocation.getArgument(1));
            return true;
        });
        when(redisUtils.get(anyString())).thenAnswer(invocation -> shared.get(invocation.getArgument(0)));

        RpQrcPendingSession session = new RpQrcPendingSession();
        session.setUuid("cross-1");
        session.setStatus("PENDING");
        session.setCallback("https://rp.example/cb");
        session.setExpiredAt(System.currentTimeMillis() + 60_000L);
        rpQrcPendingStore.saveOrFail(session);

        // 另一逻辑节点：无本机内存，仅经同一 RedisUtils 读
        RpQrcPendingStore peer = new RpQrcPendingStore();
        org.springframework.test.util.ReflectionTestUtils.setField(peer, "redisUtils", redisUtils);
        org.springframework.test.util.ReflectionTestUtils.setField(peer, "sysConfigService", sysConfigService);
        org.springframework.test.util.ReflectionTestUtils.setField(peer, "asyncTaskExecutor", asyncTaskExecutor);

        RpQrcPendingSession loaded = peer.get("cross-1");
        assertNotNull(loaded);
        assertEquals("PENDING", loaded.getStatus());
        assertEquals("https://rp.example/cb", loaded.getCallback());
        assertTrue(peer.isRedisBacked());
    }

    @Test
    void get_prefersRedisOverStaleMemory() {
        when(redisUtils.isAvailable()).thenReturn(true);
        when(redisUtils.trySet(anyString(), any(), anyLong())).thenReturn(true);

        RpQrcPendingSession local = new RpQrcPendingSession();
        local.setUuid("stale-1");
        local.setStatus("PENDING");
        local.setExpiredAt(System.currentTimeMillis() + 60_000L);
        rpQrcPendingStore.save(local);

        RpQrcPendingSession remote = new RpQrcPendingSession();
        remote.setUuid("stale-1");
        remote.setStatus("SCANNED");
        remote.setExpiredAt(System.currentTimeMillis() + 60_000L);
        when(redisUtils.get("rp:qrc:pending:stale-1")).thenReturn(JSON.toJSONString(remote));

        RpQrcPendingSession loaded = rpQrcPendingStore.get("stale-1");
        assertNotNull(loaded);
        assertEquals("SCANNED", loaded.getStatus());
    }

    @Test
    void withoutRedis_isNotRedisBacked() {
        when(redisUtils.isAvailable()).thenReturn(false);
        assertFalse(rpQrcPendingStore.isRedisBacked());
    }

    @Test
    void withRedis_isRedisBacked() {
        when(redisUtils.isAvailable()).thenReturn(true);
        assertTrue(rpQrcPendingStore.isRedisBacked());
        verify(redisUtils).isAvailable();
    }

    @Test
    void saveOrFail_writesJsonViaRedisUtils() {
        when(redisUtils.isAvailable()).thenReturn(true);
        when(redisUtils.trySet(anyString(), any(), anyLong())).thenReturn(true);

        RpQrcPendingSession session = new RpQrcPendingSession();
        session.setUuid("ok-1");
        session.setStatus("PENDING");
        session.setExpiredAt(System.currentTimeMillis() + 60_000L);
        rpQrcPendingStore.saveOrFail(session);

        verify(redisUtils).trySet(eq("rp:qrc:pending:ok-1"), anyString(), anyLong());
        assertNotNull(rpQrcPendingStore.get("ok-1"));
    }

    @Test
    void saveTerminal_clearsRemoveQueueSoDrainDoesNotDelete() {
        when(redisUtils.isAvailable()).thenReturn(false);

        RpQrcPendingSession session = new RpQrcPendingSession();
        session.setUuid("revive-1");
        session.setStatus("PENDING");
        session.setExpiredAt(System.currentTimeMillis() - 1_000L);
        rpQrcPendingStore.save(session);
        rpQrcPendingStore.enqueueExpiredKeys();

        session.setStatus("COMPLETED");
        session.setExpiredAt(System.currentTimeMillis() + 30_000L);
        rpQrcPendingStore.saveTerminal(session, RpQrcPendingStore.TERMINAL_GRACE_MS);

        rpQrcPendingStore.drainExpired();
        assertNotNull(rpQrcPendingStore.get("revive-1"));
        assertEquals("COMPLETED", rpQrcPendingStore.get("revive-1").getStatus());
    }

    @Test
    void drainExpired_skipsWhenExpiredAtRevivedWithoutClearingQueue() {
        when(redisUtils.isAvailable()).thenReturn(false);

        RpQrcPendingSession session = new RpQrcPendingSession();
        session.setUuid("race-1");
        session.setStatus("PENDING");
        session.setExpiredAt(System.currentTimeMillis() - 1_000L);
        rpQrcPendingStore.save(session);
        rpQrcPendingStore.enqueueExpiredKeys();

        // 模拟 TOCTOU：入队后直接改 expiredAt（不走 saveInternal 清队）
        session.setExpiredAt(System.currentTimeMillis() + 60_000L);
        session.setStatus("COMPLETED");

        rpQrcPendingStore.drainExpired();
        assertNotNull(rpQrcPendingStore.get("race-1"));
        assertEquals("COMPLETED", rpQrcPendingStore.get("race-1").getStatus());
    }
}
