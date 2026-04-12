package cn.org.autumn.datasources.aspect;

import cn.org.autumn.datasources.DataSourceNames;
import cn.org.autumn.datasources.DynamicDataSource;
import cn.org.autumn.datasources.annotation.DataSource;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DataSourceAspectTest {

    interface WithBlankName {
        @DataSource
        void m();
    }

    interface WithSecond {
        @DataSource(name = DataSourceNames.SECOND)
        void m();
    }

    @After
    public void tearDown() {
        DynamicDataSource.clearDataSource();
    }

    @Test
    public void blankAnnotationNameUsesFirstDuringProceed() throws Throwable {
        ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);
        MethodSignature sig = mock(MethodSignature.class);
        when(pjp.getSignature()).thenReturn(sig);
        when(sig.getMethod()).thenReturn(WithBlankName.class.getMethod("m"));
        when(pjp.proceed()).thenAnswer(invocation -> {
            assertEquals(DataSourceNames.FIRST, DynamicDataSource.getDataSource());
            return "ok";
        });
        assertNull(DynamicDataSource.getDataSource());
        Object r = new DataSourceAspect().around(pjp);
        assertEquals("ok", r);
        assertNull(DynamicDataSource.getDataSource());
    }

    @Test
    public void explicitSecondDuringProceed() throws Throwable {
        ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);
        MethodSignature sig = mock(MethodSignature.class);
        when(pjp.getSignature()).thenReturn(sig);
        when(sig.getMethod()).thenReturn(WithSecond.class.getMethod("m"));
        when(pjp.proceed()).thenAnswer(invocation -> {
            assertEquals(DataSourceNames.SECOND, DynamicDataSource.getDataSource());
            return 1;
        });
        Object r = new DataSourceAspect().around(pjp);
        assertEquals(1, r);
        assertNull(DynamicDataSource.getDataSource());
    }
}
