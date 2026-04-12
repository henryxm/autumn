package cn.org.autumn.config;

import cn.org.autumn.database.DatabaseHolder;
import cn.org.autumn.database.DatabaseType;
import org.apache.ibatis.plugin.Invocation;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Field;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ThreadLocalPaginationInterceptorTest {

    @Test
    public void interceptSetsDialectFromDatabaseHolderBeforeDelegate() throws Throwable {
        DatabaseHolder holder = mock(DatabaseHolder.class);
        when(holder.getType()).thenReturn(DatabaseType.POSTGRESQL);
        ThreadLocalPaginationInterceptor interceptor = new ThreadLocalPaginationInterceptor(holder);
        Invocation invocation = mock(Invocation.class);
        try {
            interceptor.intercept(invocation);
        } catch (Throwable ignored) {
            // MyBatis-Plus 需要真实 MappedStatement；此处只断言在委托前已按 DatabaseHolder 设置方言
        }
        assertEquals("postgresql", ReflectionTestUtils.getField(interceptor, "dialectType"));
    }

    /**
     * 验证 {@code intercept} 与私有字段 {@code dialectGuard} 使用同一把监视器：外线程持锁时，分页拦截无法跑完。
     */
    @Test
    public void interceptUsesDialectGuardMonitor() throws Exception {
        DatabaseHolder holder = mock(DatabaseHolder.class);
        when(holder.getType()).thenReturn(DatabaseType.MYSQL);
        ThreadLocalPaginationInterceptor interceptor = new ThreadLocalPaginationInterceptor(holder);
        Invocation inv = mock(Invocation.class);

        Field guardField = ThreadLocalPaginationInterceptor.class.getDeclaredField("dialectGuard");
        guardField.setAccessible(true);
        Object dialectGuard = guardField.get(interceptor);

        CountDownLatch blockerInside = new CountDownLatch(1);
        CountDownLatch releaseBlocker = new CountDownLatch(1);
        Thread blocker = new Thread(() -> {
            synchronized (dialectGuard) {
                blockerInside.countDown();
                try {
                    releaseBlocker.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
        blocker.start();
        assertTrue(blockerInside.await(5, TimeUnit.SECONDS));

        AtomicBoolean interceptFinished = new AtomicBoolean(false);
        Thread worker = new Thread(() -> {
            try {
                interceptor.intercept(inv);
            } catch (Throwable ignored) {
            }
            interceptFinished.set(true);
        });
        worker.start();
        assertFalse(interceptFinished.get());
        releaseBlocker.countDown();
        worker.join(5000);
        assertTrue(interceptFinished.get());
        blocker.join(2000);
    }
}
