package cn.org.autumn.table.data;

import cn.org.autumn.table.annotation.Column;
import cn.org.autumn.table.annotation.Index;
import cn.org.autumn.table.annotation.IndexField;
import cn.org.autumn.table.annotation.IndexOrder;
import cn.org.autumn.table.annotation.Indexes;
import cn.org.autumn.table.annotation.Table;
import cn.org.autumn.table.data.index.IndexAnnotationValidator;
import cn.org.autumn.table.data.index.IndexColumnSpec;
import cn.org.autumn.table.data.index.IndexDdlFragments;
import cn.org.autumn.table.data.index.IndexDialectCapability;
import cn.org.autumn.table.data.index.IndexDiffEngine;
import cn.org.autumn.table.data.index.IndexTableSettings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IndexInfoOrderTest {

    @AfterEach
    void clearProp() {
        System.clearProperty(IndexTableSettings.PROP_REBUILD_ON_ORDER_MISMATCH);
    }

    @Table(value = "t_idx_order", comment = "测")
    @Indexes({
            @Index(name = "tagsenabled", fields = {
                    @IndexField(field = "tags"),
                    @IndexField(field = "enabled")
            }),
            @Index(name = "living_hot", fields = {
                    @IndexField(field = "status"),
                    @IndexField(field = "livingAt", order = IndexOrder.DESC),
                    @IndexField(field = "id", order = IndexOrder.DESC)
            })
    })
    static class SampleEntity {
        String tags;
        boolean enabled;
        String status;
        java.util.Date livingAt;
        Long id;
    }

    @Test
    void annotationFieldOrderPreserved() {
        TableInfo ti = new TableInfo(SampleEntity.class);
        IndexInfo tags = find(ti, "tagsenabled");
        List<IndexColumnSpec> specs = tags.getColumnSpecs();
        assertEquals(2, specs.size());
        assertEquals("tags", specs.get(0).getColumn());
        assertEquals("enabled", specs.get(1).getColumn());
        assertEquals(IndexOrder.ASC, specs.get(0).getOrder());
    }

    @Test
    void descOrderEmittedInMysqlDdl() {
        TableInfo ti = new TableInfo(SampleEntity.class);
        IndexInfo hot = find(ti, "living_hot");
        StringBuilder sb = new StringBuilder();
        IndexDdlFragments.appendMysqlColumns(sb, hot);
        assertEquals("`status` ASC,`living_at` DESC,`id` DESC", sb.toString());
    }

    @Test
    void orderMismatchDetectedByDefault() {
        System.setProperty(IndexTableSettings.PROP_REBUILD_ON_ORDER_MISMATCH, "true");
        IndexInfo a = new IndexInfo();
        a.setName("tagsenabled");
        a.setIndexType("NORMAL");
        a.setIndexMethod("BTREE");
        a.appendColumn("tags", 0, IndexOrder.ASC);
        a.appendColumn("enabled", 0, IndexOrder.ASC);

        IndexInfo b = new IndexInfo();
        b.setName("tagsenabled");
        b.setIndexType("NORMAL");
        b.setIndexMethod("BTREE");
        b.appendColumn("enabled", 0, IndexOrder.ASC);
        b.appendColumn("tags", 0, IndexOrder.ASC);

        assertFalse(a.sameIndexColumnsAndKind(b));
    }

    @Test
    void escapeHatchUsesSetEquality() {
        System.setProperty(IndexTableSettings.PROP_REBUILD_ON_ORDER_MISMATCH, "false");
        IndexInfo a = new IndexInfo();
        a.setName("tagsenabled");
        a.setIndexType("NORMAL");
        a.setIndexMethod("BTREE");
        a.appendColumn("tags", 0, IndexOrder.ASC);
        a.appendColumn("enabled", 0, IndexOrder.ASC);

        IndexInfo b = new IndexInfo();
        b.setName("tagsenabled");
        b.setIndexType("NORMAL");
        b.setIndexMethod("BTREE");
        b.appendColumn("enabled", 0, IndexOrder.ASC);
        b.appendColumn("tags", 0, IndexOrder.ASC);

        assertTrue(a.sameIndexColumnsAndKind(b));
    }

    @Test
    void fieldLevelIndexOrderShortcut() {
        TableInfo ti = new TableInfo(FieldLevelOrderEntity.class);
        IndexInfo idx = find(ti, "living_at");
        assertEquals(1, idx.getColumnSpecs().size());
        assertEquals("living_at", idx.getColumnSpecs().get(0).getColumn());
        assertEquals(IndexOrder.DESC, idx.getColumnSpecs().get(0).getOrder());
        StringBuilder sb = new StringBuilder();
        IndexDdlFragments.appendMysqlColumns(sb, idx);
        assertEquals("`living_at` DESC", sb.toString());
    }

    @Test
    void fieldLevelPlainIndexStillAsc() {
        TableInfo ti = new TableInfo(FieldLevelOrderEntity.class);
        IndexInfo idx = find(ti, "user");
        assertEquals(IndexOrder.ASC, idx.getColumnSpecs().get(0).getOrder());
        // 未写 length 时不要把 Column.length 当成索引前缀，避免与库内 Sub_part=null 反复重建
        assertEquals(0, idx.getColumnSpecs().get(0).getPrefixLength());
    }

    @Test
    void softPrefixZeroMatchesExplicitPrefix() {
        System.setProperty(IndexTableSettings.PROP_REBUILD_ON_ORDER_MISMATCH, "true");
        IndexInfo entity = new IndexInfo();
        entity.setName("authentication");
        entity.setIndexType("NORMAL");
        entity.setIndexMethod("BTREE");
        entity.appendColumn("authentication", 32, IndexOrder.ASC);

        IndexInfo db = new IndexInfo();
        db.setName("authentication");
        db.setIndexType("NORMAL");
        db.setIndexMethod("BTREE");
        db.appendColumn("authentication", 0, IndexOrder.ASC);

        assertTrue(entity.sameIndexColumnsAndKind(db));
        assertFalse(IndexDiffEngine.isMeaningfulRebuild(entity, db));
    }

    @Test
    void uniqueRebuildsWhenDbIsNonUnique() {
        System.setProperty(IndexTableSettings.PROP_REBUILD_ON_ORDER_MISMATCH, "true");
        IndexInfo entity = new IndexInfo();
        entity.setName("uuid");
        entity.setIndexType("UNIQUE");
        entity.setIndexMethod("BTREE");
        entity.appendColumn("uuid", 0, IndexOrder.ASC);

        IndexInfo db = new IndexInfo();
        db.setName("uuid");
        db.setNonUnique(1);
        db.setIndexType("BTREE");
        db.appendColumn("uuid", 0, IndexOrder.ASC);
        db.resolve();

        assertEquals("NORMAL", db.getIndexType());
        assertFalse(entity.sameIndexColumnsAndKind(db));
        assertTrue(IndexDiffEngine.isMeaningfulRebuild(entity, db));
    }

    @Test
    void forEmitPreservesUniqueAndDoesNotMutateSource() {
        IndexInfo entity = new IndexInfo();
        entity.setName("uuid");
        entity.setIndexType("UNIQUE");
        entity.setIndexMethod("BTREE");
        entity.appendColumn("uuid", 50, IndexOrder.ASC);

        IndexInfo emit = IndexDialectCapability.forEmit(IndexDialectCapability.Family.MYSQL, entity);
        assertEquals("UNIQUE", emit.getIndexType());
        assertEquals("UNIQUE", entity.getIndexType());
        entity.resolve();
        assertEquals("UNIQUE", entity.getIndexType());
    }

    @Test
    void resolveUniqueFromShowIndexNonUniqueVariants() {
        IndexInfo a = new IndexInfo();
        a.setNonUnique(0);
        a.setIndexType("BTREE");
        a.resolve();
        assertEquals("UNIQUE", a.getIndexType());
        assertEquals("BTREE", a.getIndexMethod());

        IndexInfo b = new IndexInfo();
        b.setNonUnique(Boolean.FALSE);
        b.setIndexType("BTREE");
        b.resolve();
        assertEquals("UNIQUE", b.getIndexType());

        IndexInfo entity = new IndexInfo();
        entity.setName("auth_upper");
        entity.setIndexType("UNIQUE");
        entity.setIndexMethod("BTREE");
        entity.appendColumn("authentication", 32, IndexOrder.ASC);
        entity.appendColumn("upper", 32, IndexOrder.ASC);

        IndexInfo db = new IndexInfo();
        db.setName("auth_upper");
        db.setNonUnique(0);
        db.setIndexType("BTREE");
        db.appendColumn("authentication", 0, IndexOrder.ASC);
        db.appendColumn("upper", 0, IndexOrder.ASC);
        db.resolve();

        assertTrue(entity.sameIndexColumnsAndKind(db));
    }

    @Test
    void legacyAnnotationOnlyStillCollects() {
        TableInfo ti = new TableInfo(LegacyOnlyEntity.class);
        IndexInfo idx = find(ti, "user_side");
        assertEquals("user", idx.getColumnSpecs().get(0).getColumn());
        assertEquals("side", idx.getColumnSpecs().get(1).getColumn());
        assertTrue(idx.getFields() instanceof java.util.LinkedHashMap);
        List<String> keys = new java.util.ArrayList<>(idx.getFields().keySet());
        assertEquals(java.util.Arrays.asList("user", "side"), keys);
    }

    @Test
    void unsafeWhereRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> IndexAnnotationValidator.rejectUnsafe("where", "deleted = 0; drop table t"));
    }

    @Test
    void mysqlIgnoresWherePredicateWithoutMutatingSource() {
        IndexInfo idx = new IndexInfo();
        idx.setName("active_user");
        idx.setIndexType("NORMAL");
        idx.setIndexMethod("BTREE");
        idx.appendColumn("user", 0, IndexOrder.ASC);
        idx.setWherePredicate("deleted = 0");
        IndexInfo emit = IndexDialectCapability.forEmit(IndexDialectCapability.Family.MYSQL, idx);
        assertTrue(emit != null);
        assertEquals("", emit.getWherePredicate());
        // 实体侧保留 where，避免后续 diff 读到被清空的状态
        assertEquals("deleted = 0", idx.getWherePredicate());
    }

    @Test
    void softWhereEqualWhenDbSideEmpty() {
        IndexInfo entity = new IndexInfo();
        entity.setName("active_user");
        entity.setIndexType("NORMAL");
        entity.setIndexMethod("BTREE");
        entity.appendColumn("user", 0, IndexOrder.ASC);
        entity.setWherePredicate("deleted = 0");

        IndexInfo db = new IndexInfo();
        db.setName("active_user");
        db.setIndexType("NORMAL");
        db.setIndexMethod("BTREE");
        db.appendColumn("user", 0, IndexOrder.ASC);

        // SHOW INDEX 读不回 where 时，不应每启强制 DROP+ADD
        assertTrue(entity.sameIndexColumnsAndKind(db));
    }

    @Test
    void classifyRebuildOrderVsDefinition() {
        IndexInfo entity = new IndexInfo();
        entity.setName("tagsenabled");
        entity.setIndexType("NORMAL");
        entity.setIndexMethod("BTREE");
        entity.appendColumn("tags", 0, IndexOrder.ASC);
        entity.appendColumn("enabled", 0, IndexOrder.ASC);

        IndexInfo dbOrder = new IndexInfo();
        dbOrder.setName("tagsenabled");
        dbOrder.setIndexType("NORMAL");
        dbOrder.setIndexMethod("BTREE");
        dbOrder.appendColumn("enabled", 0, IndexOrder.ASC);
        dbOrder.appendColumn("tags", 0, IndexOrder.ASC);

        assertEquals(cn.org.autumn.table.data.index.IndexRebuildTask.Reason.ORDER,
                IndexDiffEngine.classifyRebuild(entity, dbOrder));
        assertEquals("enabled ASC,tags ASC", IndexDiffEngine.formatColumns(dbOrder));
        assertEquals("tags ASC,enabled ASC", IndexDiffEngine.formatColumns(entity));

        IndexInfo dbDef = new IndexInfo();
        dbDef.setName("tagsenabled");
        dbDef.setIndexType("UNIQUE");
        dbDef.setIndexMethod("BTREE");
        dbDef.appendColumn("other", 0, IndexOrder.ASC);
        assertEquals(cn.org.autumn.table.data.index.IndexRebuildTask.Reason.DEFINITION,
                IndexDiffEngine.classifyRebuild(entity, dbDef));
    }

    @Test
    void indexMethodCaseInsensitive() {
        IndexInfo a = new IndexInfo();
        a.setName("ix");
        a.setIndexType("NORMAL");
        a.setIndexMethod("BTREE");
        a.appendColumn("user", 0, IndexOrder.ASC);
        IndexInfo b = new IndexInfo();
        b.setName("ix");
        b.setIndexType("normal");
        b.setIndexMethod("btree");
        b.appendColumn("user", 0, IndexOrder.ASC);
        assertTrue(a.sameIndexColumnsAndKind(b));
    }

    @Test
    void blankIndexMethodTreatedAsBtree() {
        IndexInfo a = new IndexInfo();
        a.setName("ix");
        a.setIndexType("NORMAL");
        a.setIndexMethod("BTREE");
        a.appendColumn("user", 0, IndexOrder.ASC);
        IndexInfo b = new IndexInfo();
        b.setName("ix");
        b.setIndexType("NORMAL");
        b.setIndexMethod(null);
        b.appendColumn("user", 0, IndexOrder.ASC);
        assertTrue(a.sameIndexColumnsAndKind(b));
    }

    @Test
    void groupUnionTagsEnabledOrderAcceptance() {
        TableInfo ti = new TableInfo(GroupUnionLikeEntity.class);
        IndexInfo tags = find(ti, "tagsenabled");
        assertEquals("tags", tags.getColumnSpecs().get(0).getColumn());
        assertEquals("enabled", tags.getColumnSpecs().get(1).getColumn());
        StringBuilder sb = new StringBuilder();
        IndexDdlFragments.appendMysqlColumns(sb, tags);
        assertEquals("`tags` ASC,`enabled` ASC", sb.toString());
        IndexInfo del = find(ti, "deletedenableduser");
        assertEquals(java.util.Arrays.asList("deleted", "enabled", "user"),
                del.getColumnSpecs().stream().map(IndexColumnSpec::getColumn).toList());
    }

    @Test
    void diffEngineDetectsOrderMismatch() {
        IndexInfo a = new IndexInfo();
        a.setName("tagsenabled");
        a.setIndexType("NORMAL");
        a.setIndexMethod("BTREE");
        a.appendColumn("tags", 0, IndexOrder.ASC);
        a.appendColumn("enabled", 0, IndexOrder.ASC);
        IndexInfo b = new IndexInfo();
        b.setName("tagsenabled");
        b.setIndexType("NORMAL");
        b.setIndexMethod("BTREE");
        b.appendColumn("enabled", 0, IndexOrder.ASC);
        b.appendColumn("tags", 0, IndexOrder.ASC);
        System.setProperty(IndexTableSettings.PROP_REBUILD_ON_ORDER_MISMATCH, "true");
        assertFalse(IndexDiffEngine.sameDefinition(a, b));
        assertTrue(IndexDiffEngine.matches("t", a, a));
    }

    @Table(value = "t_legacy", comment = "旧")
    @Indexes({
            @Index(name = "user_side", fields = {
                    @IndexField(field = "user"),
                    @IndexField(field = "side")
            })
    })
    static class LegacyOnlyEntity {
        String user;
        String side;
    }

    /** 对齐 im MessageGroupUnionEntity 组合索引列序验收 */
    @Table(value = "im_message_group_union", comment = "群联")
    @Indexes({
            @Index(name = "tagsenabled", fields = {
                    @IndexField(field = "tags"),
                    @IndexField(field = "enabled")
            }),
            @Index(name = "deletedenableduser", fields = {
                    @IndexField(field = "deleted"),
                    @IndexField(field = "enabled"),
                    @IndexField(field = "user")
            })
    })
    static class GroupUnionLikeEntity {
        String tags;
        boolean enabled;
        boolean deleted;
        String user;
    }

    @Table(value = "t_field_idx", comment = "字段级")
    static class FieldLevelOrderEntity {
        @Column(length = 32, comment = "用户")
        @Index
        String user;

        @Column(comment = "开播时间")
        @Index(order = IndexOrder.DESC)
        java.util.Date livingAt;
    }

    private static IndexInfo find(TableInfo ti, String name) {
        for (IndexInfo i : ti.getIndexInfos()) {
            if (name.equals(i.getName())) {
                return i;
            }
        }
        throw new AssertionError("missing index " + name);
    }
}
