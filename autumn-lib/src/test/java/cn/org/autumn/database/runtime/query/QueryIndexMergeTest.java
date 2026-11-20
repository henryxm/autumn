package cn.org.autumn.database.runtime.query;

import cn.org.autumn.table.annotation.Column;
import cn.org.autumn.table.annotation.Index;
import cn.org.autumn.table.annotation.IndexField;
import cn.org.autumn.table.annotation.IndexMethodEnum;
import cn.org.autumn.table.annotation.IndexOrder;
import cn.org.autumn.table.annotation.IndexTypeEnum;
import cn.org.autumn.table.annotation.Indexes;
import cn.org.autumn.table.annotation.Table;
import cn.org.autumn.table.data.IndexInfo;
import cn.org.autumn.table.data.TableInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QueryIndexMergeTest {

    @BeforeEach
    void setUp() {
        QueryIndexDiscovery.resetForTests();
    }

    @AfterEach
    void tearDown() {
        QueryIndexDiscovery.resetForTests();
    }

    @Test
    void emptyRegistry_leavesEntityIndexesUntouched() {
        TableInfo ti = new TableInfo(DemoBindEntity.class);
        List<String> namesBefore = indexNames(ti);
        QueryIndexMerger.applyTo(ti, DemoBindEntity.class);
        assertEquals(namesBefore, indexNames(ti));
    }

    @Test
    void sameFingerprint_builderWins_entityIgnored() {
        TableInfo ti = new TableInfo(DemoBindEntity.class);
        assertTrue(ti.getIndexInfos().stream().anyMatch(i -> "auth_upper".equals(i.getName())));

        IndexInfo fromBuilder = new cn.org.autumn.database.runtime.RuntimeSql()
                .select("demo_query_bind")
                .eq("authentication", 32).eq("upper", 32)
                .unique().name("auth_upper_builder")
                .toIndexInfo();
        QueryIndexRegistry.register("demo_query_bind", fromBuilder);

        QueryIndexMerger.applyTo(ti, DemoBindEntity.class);

        assertTrue(ti.getIndexInfos().stream().anyMatch(i -> "auth_upper_builder".equals(i.getName())));
        assertFalse(ti.getIndexInfos().stream().anyMatch(i -> "auth_upper".equals(i.getName())));
        assertTrue(ti.getIndexInfos().stream().anyMatch(i -> "auth_user".equals(i.getName())));
    }

    @Test
    void builderParity_nameTypeMethodWhereIncludeParserInvisibleExpressionCollation() {
        IndexInfo info = new cn.org.autumn.database.runtime.RuntimeSql()
                .select("demo_query_bind")
                .eq("authentication", 32).collation("utf8mb4_bin")
                .eq("upper", 32)
                .unique()
                .method(IndexMethodEnum.BTREE)
                .name("auth_upper")
                .comment("oauth bind")
                .indexWhere("upper is not null")
                .include("user")
                .parser("")
                .invisible(false)
                .toIndexInfo();
        assertEquals("auth_upper", info.getName());
        assertEquals("UNIQUE", info.getIndexType());
        assertEquals("BTREE", info.getIndexMethod());
        assertEquals("oauth bind", info.getComment());
        assertEquals("upper is not null", info.getWherePredicate());
        assertTrue(info.getIncludeColumns().contains("user"));
        assertEquals("utf8mb4_bin", info.getColumnSpecs().get(0).getCollation());
        assertEquals(32, info.getColumnSpecs().get(0).getPrefixLength());

        IndexInfo expr = new cn.org.autumn.database.runtime.RuntimeSql()
                .select("demo_query_bind")
                .expression("lower(authentication)", IndexOrder.ASC)
                .name("auth_lower")
                .toIndexInfo();
        assertTrue(expr.getColumnSpecs().get(0).isExpression());
        assertEquals("lower(authentication)", expr.getColumnSpecs().get(0).getExpression());
    }

    @Test
    void prefixCover_keepsLonger() {
        IndexInfo shortIdx = new cn.org.autumn.database.runtime.RuntimeSql()
                .select("t").eq("user").eq("status").name("us").toIndexInfo();
        IndexInfo longIdx = new cn.org.autumn.database.runtime.RuntimeSql()
                .select("t").eq("user").eq("status").orderByDesc("id").name("usi").toIndexInfo();
        assertTrue(QueryIndexMerger.isPrefixCoveredBy(shortIdx, longIdx));
        assertFalse(QueryIndexMerger.isPrefixCoveredBy(longIdx, shortIdx));
        assertEquals(1, QueryIndexMerger.collapsePrefixCovered(java.util.Arrays.asList(shortIdx, longIdx)).size());
    }

    @Test
    void indexedSelect_sqlAndIndexParity() {
        String sql = new cn.org.autumn.database.runtime.RuntimeSql()
                .select("demo_query_bind")
                .eq("authentication", 32).eq("upper", 32)
                .unique().name("auth_upper")
                .limitOne()
                .registerOnSql(false)
                .sql();
        assertTrue(sql.contains("FROM"));
        assertTrue(sql.contains("authentication"));
        assertTrue(sql.contains("upper"));
        assertTrue(sql.contains("#{authentication}"));
        IndexInfo info = new cn.org.autumn.database.runtime.RuntimeSql()
                .select("demo_query_bind")
                .eq("authentication", 32).eq("upper", 32)
                .unique().name("auth_upper")
                .toIndexInfo();
        assertEquals("UNIQUE", info.getIndexType());
        assertEquals(2, info.getColumnSpecs().size());
        assertEquals(32, info.getColumnSpecs().get(0).getPrefixLength());
    }

    @Test
    void discoveryThenMerge_endToEnd() {
        QueryIndexDiscovery.discover(java.util.Collections.singleton("cn.org.autumn.database.runtime.query"));
        assertFalse(QueryIndexRegistry.forTable("demo_query_bind").isEmpty());
        assertTrue(QueryIndexRegistry.isFrozen());

        TableInfo ti = new TableInfo(DemoBindEntity.class);
        QueryIndexMerger.applyTo(ti, DemoBindEntity.class);
        assertTrue(ti.getIndexInfos().stream().anyMatch(i -> "auth_upper".equals(i.getName())));
        assertTrue(ti.getIndexInfos().stream().anyMatch(i -> "auth_user".equals(i.getName())));

        int tables = QueryIndexRegistry.tableCount();
        new cn.org.autumn.database.runtime.RuntimeSql()
                .select("demo_query_bind").eq("authentication").eq("newcol").name("should_not_register").sql();
        assertEquals(tables, QueryIndexRegistry.tableCount());
        assertTrue(QueryIndexRegistry.forTable("demo_query_bind").stream()
                .noneMatch(i -> "should_not_register".equals(i.getName())));
    }

    private static List<String> indexNames(TableInfo ti) {
        if (ti.getIndexInfos() == null) {
            return new ArrayList<>();
        }
        return ti.getIndexInfos().stream()
                .map(IndexInfo::getName)
                .collect(Collectors.toList());
    }

    @Table(value = "demo_query_bind", comment = "QueryIndex 试点实体")
    @Indexes({
            @Index(name = "auth_upper", indexType = IndexTypeEnum.UNIQUE, indexMethod = IndexMethodEnum.BTREE, fields = {
                    @IndexField(field = "authentication", length = 32),
                    @IndexField(field = "upper", length = 32)
            }),
            @Index(name = "auth_user", indexType = IndexTypeEnum.UNIQUE, indexMethod = IndexMethodEnum.BTREE, fields = {
                    @IndexField(field = "authentication", length = 32),
                    @IndexField(field = "user", length = 32)
            })
    })
    public static class DemoBindEntity {
        @Column(length = 32, comment = "用户")
        private String user;
        @Column(length = 32, comment = "接入")
        private String authentication;
        @Column(length = 32, comment = "上游")
        private String upper;
    }
}
