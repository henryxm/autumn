package cn.org.autumn.database.runtime.query;

import cn.org.autumn.table.data.IndexInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QueryIndexDiscoveryTest {

    @BeforeEach
    void setUp() {
        QueryIndexDiscovery.resetForTests();
    }

    @AfterEach
    void tearDown() {
        QueryIndexDiscovery.resetForTests();
    }

    @Test
    void discoverRegistersDemoDaoSql() {
        QueryIndexDiscovery.discover(Collections.singleton("cn.org.autumn.database.runtime.query"));
        List<IndexInfo> list = QueryIndexRegistry.forTable("demo_query_bind");
        assertFalse(list.isEmpty());
        assertTrue(list.stream().anyMatch(i -> "auth_upper".equals(i.getName())));
        assertTrue(list.stream().anyMatch(i -> "auth_user".equals(i.getName())));
        assertTrue(QueryIndexRegistry.isFrozen());
    }

    @Test
    void discoverEmptyPack_freezesWithoutIndexes() {
        QueryIndexDiscovery.discover(Collections.singleton("cn.org.autumn.database.runtime.query.nonexistent"));
        assertTrue(QueryIndexRegistry.isEmpty());
        assertTrue(QueryIndexRegistry.isFrozen());
    }
}
