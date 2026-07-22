package cn.org.autumn.thread;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class FunctionQueueTest {

    private FunctionQueue queue;

    @Before
    public void setUp() {
        queue = new FunctionQueue();
        queue.start();
        assertTrue(queue.isRunning());
    }

    @After
    public void tearDown() {
        if (queue != null)
            queue.stop();
    }

    /** 忽略 interrupt 的阻塞，避免自愈 interrupt 误伤测试闸门 */
    private static void parkUntil(AtomicBoolean release) {
        while (!release.get()) {
            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(10));
        }
    }

    @Test
    public void offerRunsInOrderWithArg() throws Exception {
        int n = 20;
        CountDownLatch done = new CountDownLatch(n);
        List<Integer> order = Collections.synchronizedList(new ArrayList<>());
        for (int i = 0; i < n; i++) {
            final int v = i;
            assertTrue(queue.offer("ord-" + v, () -> {
                order.add(v);
                done.countDown();
            }));
        }
        assertTrue(done.await(5, TimeUnit.SECONDS));
        assertEquals(n, order.size());
        for (int i = 0; i < n; i++)
            assertEquals(Integer.valueOf(i), order.get(i));
        assertEquals(n, queue.getExecuted());
    }

    @Test
    public void namedOfferDedupesPending() throws Exception {
        AtomicBoolean release = new AtomicBoolean(false);
        CountDownLatch started = new CountDownLatch(1);
        AtomicInteger runs = new AtomicInteger();
        assertTrue(queue.offer("blocker", () -> {
            started.countDown();
            parkUntil(release);
        }));
        assertTrue(started.await(3, TimeUnit.SECONDS));
        assertTrue(queue.offer("same", runs::incrementAndGet));
        assertTrue(queue.offer("same", runs::incrementAndGet));
        assertEquals(1, queue.size());
        release.set(true);
        Thread.sleep(400);
        assertEquals(1, runs.get());
    }

    @Test
    public void workerStaysAliveAcrossIdleAndNextOffer() throws Exception {
        CountDownLatch first = new CountDownLatch(1);
        assertTrue(queue.offer("first", first::countDown));
        assertTrue(first.await(3, TimeUnit.SECONDS));
        Thread.sleep(200);
        CountDownLatch second = new CountDownLatch(1);
        assertTrue(queue.offer("second", second::countDown));
        assertTrue(second.await(3, TimeUnit.SECONDS));
        assertTrue(queue.isRunning());
        assertEquals(2, queue.getExecuted());
    }

    @Test
    public void failedTaskDoesNotStopSubsequent() throws Exception {
        CountDownLatch done = new CountDownLatch(1);
        AtomicInteger ran = new AtomicInteger();
        assertTrue(queue.offer("boom", () -> {
            ran.incrementAndGet();
            throw new RuntimeException("boom");
        }));
        assertTrue(queue.offer("ok", () -> {
            ran.incrementAndGet();
            done.countDown();
        }));
        assertTrue(done.await(5, TimeUnit.SECONDS));
        assertEquals(2, ran.get());
        assertEquals(1, queue.getFailed());
        assertEquals(1, queue.getExecuted());
    }

    @Test
    public void stopRejectsFurtherOffers() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        assertTrue(queue.offer(started::countDown));
        assertTrue(started.await(3, TimeUnit.SECONDS));
        queue.stop();
        assertFalse(queue.isRunning());
        assertFalse(queue.offer("after-stop", () -> {
        }));
        assertTrue(queue.isEmpty());
    }

    @Test
    public void rejectWhenCapacityFull() throws Exception {
        queue.updateConfig(2, OverflowPolicy.REJECT, null, null);
        AtomicBoolean release = new AtomicBoolean(false);
        CountDownLatch started = new CountDownLatch(1);
        assertTrue(queue.offer("blocker", () -> {
            started.countDown();
            parkUntil(release);
        }));
        assertTrue(started.await(3, TimeUnit.SECONDS));
        assertTrue(queue.offer("p1", () -> {
        }));
        assertTrue(queue.offer("p2", () -> {
        }));
        assertFalse(queue.offer("p3", () -> {
        }));
        assertTrue(queue.getRejected() >= 1);
        release.set(true);
        Thread.sleep(200);
        queue.clear();
    }

    @Test
    public void dropOldestWhenFull() throws Exception {
        queue.updateConfig(1, OverflowPolicy.DROP_OLDEST, null, null);
        AtomicBoolean release = new AtomicBoolean(false);
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(1);
        AtomicInteger ranOldest = new AtomicInteger();
        assertTrue(queue.offer("blocker", () -> {
            started.countDown();
            parkUntil(release);
        }));
        assertTrue(started.await(3, TimeUnit.SECONDS));
        assertTrue(queue.offer("old", ranOldest::incrementAndGet));
        assertTrue(queue.offer("new", done::countDown));
        assertTrue(queue.getDropped() >= 1);
        release.set(true);
        assertTrue(done.await(5, TimeUnit.SECONDS));
        assertEquals(0, ranOldest.get());
    }

    @Test
    public void softCapacityChangeDoesNotStall() throws Exception {
        Thread.sleep(100);
        queue.updateConfig(32, OverflowPolicy.REJECT, null, null);
        assertEquals(32, queue.getCapacity());
        CountDownLatch done = new CountDownLatch(1);
        assertTrue(queue.offer("after-resize", done::countDown));
        assertTrue(done.await(5, TimeUnit.SECONDS));
        assertTrue(queue.getExecuted() + queue.getFailed() >= 1);
    }

    @Test
    public void recoverIfStalledWakesWhenPending() throws Exception {
        CountDownLatch done = new CountDownLatch(1);
        assertTrue(queue.offer("wake-me", done::countDown));
        String r = queue.recoverIfStalled();
        assertTrue(r != null);
        assertTrue(done.await(5, TimeUnit.SECONDS));
    }

    @Test
    public void interruptibleTaskTimesOutAndContinues() throws Exception {
        queue.updateConfig(null, null, null, null, 1000L, 500L);
        CountDownLatch hungStarted = new CountDownLatch(1);
        CountDownLatch nextDone = new CountDownLatch(1);
        assertTrue(queue.offer("hang-sleep", () -> {
            hungStarted.countDown();
            try {
                Thread.sleep(60000L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("interrupted", e);
            }
        }));
        assertTrue(hungStarted.await(3, TimeUnit.SECONDS));
        assertTrue(queue.offer("after-timeout", nextDone::countDown));
        assertTrue("next task should run after timeout", nextDone.await(8, TimeUnit.SECONDS));
        assertTrue(queue.getTimedOut() >= 1 || queue.getAbandoned() >= 1);
        assertTrue(queue.getExecuted() >= 1);
    }

    @Test
    public void uninterruptibleTaskIsAbandonedAndContinues() throws Exception {
        queue.updateConfig(null, null, null, null, 1000L, 500L);
        AtomicBoolean release = new AtomicBoolean(false);
        CountDownLatch hungStarted = new CountDownLatch(1);
        CountDownLatch nextDone = new CountDownLatch(1);
        assertTrue(queue.offer("hang-spin", () -> {
            hungStarted.countDown();
            parkUntil(release);
        }));
        assertTrue(hungStarted.await(3, TimeUnit.SECONDS));
        assertTrue(queue.offer("after-abandon", nextDone::countDown));
        assertTrue("next task should run after hard abandon", nextDone.await(10, TimeUnit.SECONDS));
        assertTrue(queue.getAbandoned() >= 1);
        release.set(true);
        Thread.sleep(200);
        assertTrue(queue.isRunning());
    }

    @Test
    public void manualRecoverAbandonsBusyTask() throws Exception {
        AtomicBoolean release = new AtomicBoolean(false);
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch nextDone = new CountDownLatch(1);
        assertTrue(queue.offer("manual-hang", () -> {
            started.countDown();
            parkUntil(release);
        }));
        assertTrue(started.await(3, TimeUnit.SECONDS));
        assertTrue(queue.offer("after-manual", nextDone::countDown));
        String r = queue.recoverIfStalled(true);
        assertTrue(r != null && r.contains("abandoned"));
        assertTrue(nextDone.await(5, TimeUnit.SECONDS));
        release.set(true);
    }
}
