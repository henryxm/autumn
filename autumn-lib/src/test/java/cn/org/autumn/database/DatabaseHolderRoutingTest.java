package cn.org.autumn.database;

import cn.org.autumn.datasources.DataSourceDialectRegistry;
import cn.org.autumn.datasources.DataSourceNames;
import cn.org.autumn.datasources.DynamicDataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.Assert.assertEquals;

public class DatabaseHolderRoutingTest {

    private DatabaseHolder holder;
    private MockEnvironment env;

    @Before
    public void setUp() {
        DynamicDataSource.clearDataSource();
        env = new MockEnvironment()
                .withProperty("spring.datasource.druid.first.url", "jdbc:mysql://localhost:3306/a")
                .withProperty("spring.datasource.druid.second.url", "jdbc:postgresql://localhost:5432/b");
        DataSourceDialectRegistry registry = new DataSourceDialectRegistry(env);
        holder = new DatabaseHolder();
        ReflectionTestUtils.setField(holder, "environment", env);
        ReflectionTestUtils.setField(holder, "databaseRaw", "");
        ReflectionTestUtils.setField(holder, "dataSourceDialectRegistry", registry);
    }

    @After
    public void tearDown() {
        DynamicDataSource.clearDataSource();
    }

    @Test
    public void typeFollowsThreadLocalLookupKey() {
        assertEquals(DatabaseType.MYSQL, holder.getType());
        DynamicDataSource.setDataSource(DataSourceNames.SECOND);
        assertEquals(DatabaseType.POSTGRESQL, holder.getType());
        DynamicDataSource.clearDataSource();
        assertEquals(DatabaseType.MYSQL, holder.getType());
    }

    @Test
    public void withoutRegistryFallsBackToFirstUrlOnly() {
        DatabaseHolder plain = new DatabaseHolder();
        ReflectionTestUtils.setField(plain, "environment", env);
        ReflectionTestUtils.setField(plain, "databaseRaw", "");
        ReflectionTestUtils.setField(plain, "dataSourceDialectRegistry", null);
        DynamicDataSource.setDataSource(DataSourceNames.SECOND);
        assertEquals(DatabaseType.MYSQL, plain.getType());
        DynamicDataSource.clearDataSource();
    }
}
