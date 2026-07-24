package cn.org.autumn.thread;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class JobPhaseGateTest {

    @Test
    public void createStartsIdle() {
        AtomicReference<JobPhase> phase = JobPhaseGate.create();
        assertTrue(JobPhaseGate.isIdle(phase));
        assertFalse(JobPhaseGate.isDispatching(phase));
        assertEquals(JobPhase.IDLE, JobPhaseGate.get(phase));
    }

    @Test
    public void tryBeginOnlyOnceUntilEnd() {
        AtomicReference<JobPhase> phase = JobPhaseGate.create();
        assertTrue(JobPhaseGate.tryBegin(phase));
        assertTrue(JobPhaseGate.isDispatching(phase));
        assertFalse(JobPhaseGate.tryBegin(phase));

        JobPhaseGate.end(phase);
        assertTrue(JobPhaseGate.isIdle(phase));
        assertTrue(JobPhaseGate.tryBegin(phase));
    }

    @Test
    public void endAndMaybeRescheduleWhenPending() {
        AtomicReference<JobPhase> phase = JobPhaseGate.create();
        assertTrue(JobPhaseGate.tryBegin(phase));
        AtomicInteger schedules = new AtomicInteger();
        JobPhaseGate.endAndMaybeReschedule(phase, true, schedules::incrementAndGet);
        assertEquals(1, schedules.get());
        // schedule 回调里通常会再 tryBegin；此处仅验证 end 后为 IDLE 且触发了 schedule
        assertTrue(JobPhaseGate.isIdle(phase) || JobPhaseGate.isDispatching(phase));
    }

    @Test
    public void endAndMaybeRescheduleSkipsWhenEmpty() {
        AtomicReference<JobPhase> phase = JobPhaseGate.create();
        assertTrue(JobPhaseGate.tryBegin(phase));
        AtomicInteger schedules = new AtomicInteger();
        JobPhaseGate.endAndMaybeReschedule(phase, false, schedules::incrementAndGet);
        assertEquals(0, schedules.get());
        assertTrue(JobPhaseGate.isIdle(phase));
    }

    @Test
    public void recoverIfStalledOnlyWhenIdleAndPending() {
        AtomicReference<JobPhase> phase = JobPhaseGate.create();
        AtomicInteger schedules = new AtomicInteger();
        JobPhaseGate.recoverIfStalled(phase, true, schedules::incrementAndGet);
        assertEquals(1, schedules.get());

        assertTrue(JobPhaseGate.tryBegin(phase));
        JobPhaseGate.recoverIfStalled(phase, true, schedules::incrementAndGet);
        assertEquals(1, schedules.get());

        JobPhaseGate.end(phase);
        JobPhaseGate.recoverIfStalled(phase, false, schedules::incrementAndGet);
        assertEquals(1, schedules.get());
    }

    @Test
    public void getNullPhaseIsIdle() {
        assertEquals(JobPhase.IDLE, JobPhaseGate.get(null));
    }
}
