package cn.org.autumn.database.runtime.query;

import cn.org.autumn.database.runtime.RuntimeSql;
import cn.org.autumn.table.annotation.Index;
import cn.org.autumn.table.annotation.IndexField;
import cn.org.autumn.table.annotation.Indexes;
import cn.org.autumn.table.data.IndexInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

    @Test
    void unmarkedMethod_notInvokedForIndex() {
        QueryIndexDiscovery.discover(Collections.singleton("cn.org.autumn.database.runtime.query"));
        assertEquals(0, QueryIndexRegistry.forTable("unmarked_only_table").size());
    }

    /** 无 @Index 的 Provider 不被发现调用。 */
    public static class UnmarkedDaoSql extends RuntimeSql {
        public String listAll() {
            return select("unmarked_only_table").eq("user").name("should_not").sql();
        }
    }

    /** 有 @Index 但未走 IndexedSelect → 不登记（可调用成功）。 */
    public static class MarkerOnlyStringDaoSql extends RuntimeSql {
        @Index
        public String plain() {
            return "SELECT 1";
        }
    }

    @Test
    void methodIndexPayload_ignored_indexedSelectWins() {
        QueryIndexDiscovery.discover(Collections.singleton("cn.org.autumn.database.runtime.query"));
        List<IndexInfo> list = QueryIndexRegistry.forTable("demo_payload_ignored");
        assertTrue(list.stream().anyMatch(i -> "from_select".equals(i.getName())));
        assertTrue(list.stream().noneMatch(i -> "from_annotation_payload".equals(i.getName())));
    }

    /**
     * 方法上即便写了 fields/name 载荷，发现也不解析；登记名来自 IndexedSelect。
     */
    public static class PayloadIgnoredDaoSql extends RuntimeSql {
        @Indexes({
                @Index(name = "from_annotation_payload", fields = {
                        @IndexField(field = "other")
                })
        })
        public String byUser() {
            return select("demo_payload_ignored").eq("user").name("from_select").sql();
        }
    }
}
