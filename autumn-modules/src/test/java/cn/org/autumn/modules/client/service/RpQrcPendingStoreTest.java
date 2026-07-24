package cn.org.autumn.modules.client.service;

import cn.org.autumn.modules.client.model.RpQrcPendingSession;
import cn.org.autumn.modules.sys.service.SysConfigService;
import cn.org.autumn.thread.TagRunnable;
import cn.org.autumn.thread.TagTaskExecutor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RpQrcPendingStoreTest {

    @Mock
    private SysConfigService sysConfigService;

    @Mock
    private TagTaskExecutor asyncTaskExecutor;

    @InjectMocks
    private RpQrcPendingStore rpQrcPendingStore;

    @Test
    void onThirtySecond_enqueuesThenAsyncDrainRemovesExpired() {
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
        RpQrcPendingSession expired = new RpQrcPendingSession();
        expired.setUuid("expired-2");
        expired.setStatus("COMPLETED");
        expired.setExpiredAt(System.currentTimeMillis() - 1_000L);
        rpQrcPendingStore.save(expired);

        rpQrcPendingStore.enqueueExpiredKeys();

        // 秒级只入队，尚未 drain 时内存仍可读
        assertNotNull(rpQrcPendingStore.get("expired-2"));

        rpQrcPendingStore.drainExpired();
        assertNull(rpQrcPendingStore.get("expired-2"));
    }
}
