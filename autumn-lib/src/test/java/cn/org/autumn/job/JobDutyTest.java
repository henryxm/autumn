package cn.org.autumn.job;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        assertEquals(JobDuty.ALL, JobDuty.of("unknown"));
        assertEquals(JobDuty.ALL, JobDuty.of("SINGLTON"));
    }

    @Test
    void mergeDuty_methodDefaultAll_keepsClassSingleton() {
        assertEquals(JobDuty.SINGLETON, JobDutySupport.mergeDuty(JobDuty.SINGLETON, JobDuty.ALL, true));
        assertEquals(JobDuty.SEQUENTIAL, JobDutySupport.mergeDuty(JobDuty.SINGLETON, JobDuty.SEQUENTIAL, true));
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
}
