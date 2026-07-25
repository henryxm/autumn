package cn.org.autumn.job;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class JobDutyTest {

    @Test
    void of_blank_isAll() {
        assertEquals(JobDuty.ALL, JobDuty.of(null));
        assertEquals(JobDuty.ALL, JobDuty.of(""));
        assertEquals(JobDuty.ALL, JobDuty.of("  "));
        assertTrue(JobDuty.ALL.isDefault());
    }

    @Test
    void of_names() {
        assertEquals(JobDuty.SINGLETON, JobDuty.of("singleton"));
        assertEquals(JobDuty.SEQUENTIAL, JobDuty.of("SEQUENTIAL"));
        assertEquals(JobDuty.DISABLED, JobDuty.of("Disabled"));
        assertEquals(JobDuty.LOCAL, JobDuty.of("local"));
        assertEquals(JobDuty.ALL, JobDuty.of("unknown"));
        assertEquals(JobDuty.ALL, JobDuty.of("SINGLTON"));
    }

    @Test
    void mergeDuty_methodDefaultAll_keepsClassSingleton() {
        assertEquals(JobDuty.SINGLETON, JobDutySupport.mergeDuty(JobDuty.SINGLETON, JobDuty.ALL, true));
        assertEquals(JobDuty.SEQUENTIAL, JobDutySupport.mergeDuty(JobDuty.SINGLETON, JobDuty.SEQUENTIAL, true));
        assertEquals(JobDuty.LOCAL, JobDutySupport.mergeDuty(JobDuty.LOCAL, JobDuty.ALL, true));
        assertEquals(JobDuty.LOCAL, JobDutySupport.mergeDuty(JobDuty.ALL, JobDuty.LOCAL, true));
        assertEquals(JobDuty.ALL, JobDutySupport.mergeDuty(JobDuty.SINGLETON, JobDuty.ALL, false));
        assertEquals(JobDuty.ALL, JobDutySupport.mergeDuty(null, null, false));
    }

    @Test
    void mergeOncePerPeriod_methodDefaultFalse_keepsClassTrue() {
        assertTrue(JobDutySupport.mergeOncePerPeriod(true, false, true));
        assertTrue(JobDutySupport.mergeOncePerPeriod(false, true, true));
        assertFalse(JobDutySupport.mergeOncePerPeriod(false, false, true));
        assertTrue(JobDutySupport.mergeOncePerPeriod(false, true, false));
        assertFalse(JobDutySupport.mergeOncePerPeriod(true, false, false));
    }

    @Test
    void localScopedKey_usesUnknownWithoutProfile() {
        String key = JobDutySupport.localScopedKey("cache:cleanup");
        assertTrue(key.startsWith("local:"));
        assertTrue(key.endsWith(":cache:cleanup"));
    }

    @Test
    void localDuty_secondThreadSkipsWhileHeld() throws Exception {
        CountDownLatch held = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        AtomicInteger ran = new AtomicInteger();
        Thread t1 = new Thread(() -> {
            try {
                JobDutySupport.run(JobDuty.LOCAL, "FiveMinute|t", "ut:local-lock", () -> {
                    ran.incrementAndGet();
                    held.countDown();
                    try {
                        release.await(5, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        t1.start();
        assertTrue(held.await(5, TimeUnit.SECONDS));
        JobDutySupport.run(JobDuty.LOCAL, "FiveMinute|t", "ut:local-lock", ran::incrementAndGet);
        assertEquals(1, ran.get());
        release.countDown();
        t1.join(5000);
        assertEquals(1, ran.get());
    }
}
