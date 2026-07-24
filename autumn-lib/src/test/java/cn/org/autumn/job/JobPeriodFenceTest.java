package cn.org.autumn.job;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class JobPeriodFenceTest {

    @Test
    void periodBucket_dividesByInterval() {
        assertEquals(0L, JobPeriodFence.periodBucket(0, 60_000));
        assertEquals(0L, JobPeriodFence.periodBucket(59_999, 60_000));
        assertEquals(1L, JobPeriodFence.periodBucket(60_000, 60_000));
        assertEquals(2L, JobPeriodFence.periodBucket(120_000, 60_000));
    }

    @Test
    void periodBucket_nonPositiveInterval_isZero() {
        assertEquals(0L, JobPeriodFence.periodBucket(12345, 0));
        assertEquals(0L, JobPeriodFence.periodBucket(12345, -1));
    }

    @Test
    void ttlMs_addsGrace() {
        assertEquals(60_000L + JobPeriodFence.TTL_GRACE_MS, JobPeriodFence.ttlMs(60_000));
        assertEquals(60_000L + JobPeriodFence.TTL_GRACE_MS, JobPeriodFence.ttlMs(0));
    }

    @Test
    void fenceKey_includesBaseAndBucket() {
        assertEquals("autumn:job:once:my-lock:42", JobPeriodFence.fenceKey("my-lock", 42));
        assertEquals("autumn:job:once:my-lock:42", JobPeriodFence.fenceKey("  my-lock  ", 42));
    }
}
