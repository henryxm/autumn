package cn.org.autumn.datasources;

import cn.org.autumn.database.DatabaseType;
import org.junit.After;
import org.junit.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.Assert.assertEquals;

public class DataSourceDialectRegistryTest {

    @After
    public void tearDown() {
        DynamicDataSource.clearDataSource();
    }

    @Test
    public void firstMysqlSecondPostgresql() {
        MockEnvironment env = new MockEnvironment()
                .withProperty("spring.datasource.druid.first.url", "jdbc:mysql://localhost:3306/a")
                .withProperty("spring.datasource.druid.second.url", "jdbc:postgresql://localhost:5432/b");
        DataSourceDialectRegistry r = new DataSourceDialectRegistry(env);
        assertEquals(DatabaseType.MYSQL, r.getFirstType());
        assertEquals(DatabaseType.POSTGRESQL, r.getSecondType());
        assertEquals(DatabaseType.MYSQL, r.resolveForLookupKey(null));
        assertEquals(DatabaseType.MYSQL, r.resolveForLookupKey(""));
        assertEquals(DatabaseType.MYSQL, r.resolveForLookupKey(DataSourceNames.FIRST));
        assertEquals(DatabaseType.POSTGRESQL, r.resolveForLookupKey(DataSourceNames.SECOND));
    }

    @Test
    public void blankSecondUrlUsesFirstType() {
        MockEnvironment env = new MockEnvironment()
                .withProperty("spring.datasource.druid.first.url", "jdbc:sqlite:./x.db");
        DataSourceDialectRegistry r = new DataSourceDialectRegistry(env);
        assertEquals(DatabaseType.SQLITE, r.getFirstType());
        assertEquals(DatabaseType.SQLITE, r.getSecondType());
    }

    @Test
    public void unknownLookupKeyFallsBackToFirst() {
        MockEnvironment env = new MockEnvironment()
                .withProperty("spring.datasource.druid.first.url", "jdbc:h2:mem:x;MODE=MySQL");
        DataSourceDialectRegistry r = new DataSourceDialectRegistry(env);
        assertEquals(DatabaseType.MYSQL, r.resolveForLookupKey("unknown"));
    }

    @Test
    public void resolveJdbcUrlForLookupKeyTracksFirstAndSecond() {
        MockEnvironment env = new MockEnvironment()
                .withProperty("spring.datasource.druid.first.url", "jdbc:mysql://localhost/a")
                .withProperty("spring.datasource.druid.second.url", "jdbc:h2:mem:z;MODE=MySQL");
        DataSourceDialectRegistry r = new DataSourceDialectRegistry(env);
        assertEquals("jdbc:mysql://localhost/a", r.resolveJdbcUrlForLookupKey(null));
        assertEquals("jdbc:mysql://localhost/a", r.resolveJdbcUrlForLookupKey(""));
        assertEquals("jdbc:mysql://localhost/a", r.resolveJdbcUrlForLookupKey(DataSourceNames.FIRST));
        assertEquals("jdbc:h2:mem:z;MODE=MySQL", r.resolveJdbcUrlForLookupKey(DataSourceNames.SECOND));
        assertEquals("jdbc:mysql://localhost/a", r.resolveJdbcUrlForLookupKey("unknown"));
    }
}
