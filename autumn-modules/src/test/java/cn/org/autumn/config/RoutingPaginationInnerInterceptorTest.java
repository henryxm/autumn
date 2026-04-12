package cn.org.autumn.config;

import cn.org.autumn.database.DatabaseHolder;
import cn.org.autumn.database.DatabaseType;
import com.baomidou.mybatisplus.annotation.DbType;
import org.junit.Test;
import org.springframework.core.env.Environment;

import java.lang.reflect.Field;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RoutingPaginationInnerInterceptorTest {

    @Test
    public void initialDbTypeH2WhenPrimaryUrlIsEmbeddedMysqlH2() {
        DatabaseHolder holder = mock(DatabaseHolder.class);
        when(holder.getType()).thenReturn(DatabaseType.MYSQL);
        Environment env = mock(Environment.class);
        when(env.getProperty("spring.datasource.druid.first.url")).thenReturn("jdbc:h2:mem:x;MODE=MySQL");
        when(env.getProperty("spring.datasource.url")).thenReturn(null);
        when(holder.getEnvironment()).thenReturn(env);

        RoutingPaginationInnerInterceptor interceptor = new RoutingPaginationInnerInterceptor(holder, env);
        assertEquals(DbType.H2, interceptor.getDbType());
    }

    @Test
    public void initialDbTypeFromHolderWhenPrimaryUrlNotH2EmbeddedMysql() {
        DatabaseHolder holder = mock(DatabaseHolder.class);
        when(holder.getType()).thenReturn(DatabaseType.POSTGRESQL);
        Environment env = mock(Environment.class);
        when(env.getProperty("spring.datasource.druid.first.url")).thenReturn("jdbc:postgresql://localhost/db");
        when(env.getProperty("spring.datasource.url")).thenReturn(null);
        when(holder.getEnvironment()).thenReturn(env);

        RoutingPaginationInnerInterceptor interceptor = new RoutingPaginationInnerInterceptor(holder, env);
        assertEquals(DbType.POSTGRE_SQL, interceptor.getDbType());
    }

    @Test
    public void dialectGuardFieldExistsForConcurrencyDocumentation() throws Exception {
        DatabaseHolder holder = mock(DatabaseHolder.class);
        when(holder.getType()).thenReturn(DatabaseType.MYSQL);
        Environment env = mock(Environment.class);
        when(env.getProperty("spring.datasource.druid.first.url")).thenReturn("jdbc:mysql://localhost/db");
        when(env.getProperty("spring.datasource.url")).thenReturn(null);
        when(holder.getEnvironment()).thenReturn(env);

        RoutingPaginationInnerInterceptor interceptor = new RoutingPaginationInnerInterceptor(holder, env);
        Field f = RoutingPaginationInnerInterceptor.class.getDeclaredField("dialectGuard");
        f.setAccessible(true);
        assertNotNull(f.get(interceptor));
    }
}
