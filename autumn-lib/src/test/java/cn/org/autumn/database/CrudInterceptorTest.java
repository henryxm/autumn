package cn.org.autumn.database;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.org.autumn.exception.AException;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.Invocation;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class CrudInterceptorTest {

    private CrudGuard guard;

    @Before
    public void setUp() {
        guard = new CrudGuard();
    }

    @After
    public void tearDown() {
        guard.clear();
    }

    @Test
    public void insertCommandIsBlockedWhenDatabaseWriteOff() throws Throwable {
        guard.apply(false, true, "read-only");
        CrudInterceptor interceptor = new CrudInterceptor(guard);
        Invocation invocation = invocation(SqlCommandType.INSERT);
        try {
            interceptor.intercept(invocation);
            Assert.fail("INSERT should be blocked");
        } catch (AException e) {
            Assert.assertEquals(834, e.getCode());
        }
        verify(invocation, never()).proceed();
    }

    @Test
    public void selectCommandIsNotChecked() throws Throwable {
        guard.apply(false, true, "read-only");
        CrudInterceptor interceptor = new CrudInterceptor(guard);
        Invocation invocation = invocation(SqlCommandType.SELECT);
        when(invocation.proceed()).thenReturn(0);
        interceptor.intercept(invocation);
        verify(invocation).proceed();
    }

    @Test
    public void updateCommandProceedsWhenWriteEnabled() throws Throwable {
        guard.apply(true, true, "");
        CrudInterceptor interceptor = new CrudInterceptor(guard);
        Invocation invocation = invocation(SqlCommandType.UPDATE);
        when(invocation.proceed()).thenReturn(1);
        Object result = interceptor.intercept(invocation);
        Assert.assertEquals(1, result);
        verify(invocation).proceed();
    }

    private static Invocation invocation(SqlCommandType type) {
        MappedStatement ms = mock(MappedStatement.class);
        when(ms.getSqlCommandType()).thenReturn(type);
        Invocation invocation = mock(Invocation.class);
        when(invocation.getArgs()).thenReturn(new Object[]{ms, new Object()});
        return invocation;
    }
}
