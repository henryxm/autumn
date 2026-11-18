package cn.org.autumn.table.data;

import cn.org.autumn.table.annotation.Index;
import cn.org.autumn.table.annotation.IndexField;
import cn.org.autumn.table.annotation.IndexOrder;
import cn.org.autumn.table.annotation.Indexes;
import cn.org.autumn.table.annotation.Table;
import cn.org.autumn.table.data.index.IndexAnnotationValidator;
import cn.org.autumn.table.data.index.IndexDiffEngine;
import cn.org.autumn.table.data.index.IndexRebuildTask;
import cn.org.autumn.table.relational.RelationalSchemaSql;
import cn.org.autumn.table.relational.dialect.mysql.MysqlSchemaSql;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IndexIdentityAndNamingTest {

    @Table(value = "t_idx_naming", comment = "测")
    @Indexes({
            @Index(name = "usertagplace", fields = {
                    @IndexField(field = "user"),
                    @IndexField(field = "tag"),
                    @IndexField(field = "place"),
                    @IndexField(field = "createAt")
            }),
            @Index(name = "", fields = {
                    @IndexField(field = "status"),
                    @IndexField(field = "enabled")
            })
    })
    static class NamingEntity {
        String user;
        String tag;
        String place;
        java.util.Date createAt;
        String status;
        boolean enabled;
    }

    @Test
    void camelAndSnakeFieldEquivalent() {
        IndexInfo viaCamel = new IndexInfo(NamingEntity.class.getAnnotation(Indexes.class).value()[0]);
        assertEquals("create_at", viaCamel.getColumnSpecs().get(3).getColumn());

        IndexInfo viaSnake = new IndexInfo();
        viaSnake.setName("usertagplace");
        viaSnake.setIndexType("NORMAL");
        viaSnake.setIndexMethod("BTREE");
        viaSnake.appendColumn("user", 0, IndexOrder.ASC);
        viaSnake.appendColumn("tag", 0, IndexOrder.ASC);
        viaSnake.appendColumn("place", 0, IndexOrder.ASC);
        viaSnake.appendColumn("create_at", 0, IndexOrder.ASC);

        assertTrue(viaCamel.sameIndexColumnsAndKind(viaSnake));
        assertEquals("createat", viaCamel.getColumnSpecs().get(3).identityKey());
    }

    @Test
    void nameMatchIgnoresUnderscore() {
        assertTrue(IndexInfo.relationalSchemaScopedIndexNamesMatch("t", "living_hot", "livinghot"));
        assertTrue(IndexInfo.relationalSchemaScopedIndexNamesMatch("t_user", "t_user_statuslivingat", "statuslivingat"));
    }

    @Test
    void fingerprintPairDetectsRenameOnly() {
        IndexInfo entity = new IndexInfo();
        entity.setName("usertagplace");
        entity.setIndexType("NORMAL");
        entity.setIndexMethod("BTREE");
        entity.appendColumn("user", 0, IndexOrder.ASC);
        entity.appendColumn("tag", 0, IndexOrder.ASC);

        IndexInfo db = new IndexInfo();
        db.setName("old_user_tag");
        db.setIndexType("NORMAL");
        db.setIndexMethod("BTREE");
        db.appendColumn("user", 0, IndexOrder.ASC);
        db.appendColumn("tag", 0, IndexOrder.ASC);

        List<IndexInfo> remaining = new ArrayList<>();
        remaining.add(db);
        IndexDiffEngine.PairOutcome out = IndexDiffEngine.pair("t", entity, remaining);
        assertEquals(IndexDiffEngine.PairOutcome.Kind.BY_DEFINITION, out.kind);
        assertSame(db, out.dbIndex);
        assertEquals(IndexRebuildTask.Reason.NAME,
                IndexDiffEngine.classifyAfterPair("t", entity, db));
    }

    @Test
    void ambiguousFingerprintSkipsAutoRename() {
        IndexInfo entity = new IndexInfo();
        entity.setName("a");
        entity.setIndexType("NORMAL");
        entity.setIndexMethod("BTREE");
        entity.appendColumn("user", 0, IndexOrder.ASC);

        IndexInfo db1 = new IndexInfo();
        db1.setName("x");
        db1.setIndexType("NORMAL");
        db1.setIndexMethod("BTREE");
        db1.appendColumn("user", 0, IndexOrder.ASC);

        IndexInfo db2 = new IndexInfo();
        db2.setName("y");
        db2.setIndexType("NORMAL");
        db2.setIndexMethod("BTREE");
        db2.appendColumn("user", 0, IndexOrder.ASC);

        List<IndexInfo> remaining = new ArrayList<>();
        remaining.add(db1);
        remaining.add(db2);
        IndexDiffEngine.PairOutcome out = IndexDiffEngine.pair("t", entity, remaining);
        assertEquals(IndexDiffEngine.PairOutcome.Kind.AMBIGUOUS, out.kind);
        assertNull(out.dbIndex);
    }

    @Test
    void emptyNameDerivedAndLongNameTruncated() {
        TableInfo ti = new TableInfo(NamingEntity.class);
        IndexInfo blank = null;
        for (IndexInfo i : ti.getIndexInfos()) {
            if (i.getColumnSpecs().size() == 2
                    && "status".equals(i.getColumnSpecs().get(0).getColumn())) {
                blank = i;
            }
        }
        assertNotNull(blank);
        assertEquals("statusenabled", blank.getName());

        IndexInfo longName = new IndexInfo();
        String original = "a".repeat(80);
        longName.setName(original);
        longName.setIndexType("NORMAL");
        longName.setIndexMethod("BTREE");
        longName.appendColumn("user", 0, IndexOrder.ASC);
        assertTrue(longName.ensurePhysicalName(IndexInfo.DEFAULT_MAX_INDEX_NAME_LENGTH));
        assertEquals(IndexInfo.DEFAULT_MAX_INDEX_NAME_LENGTH, longName.getName().length());
        assertEquals(IndexInfo.truncateIndexName(original, 64), longName.getName());

        List<IndexInfo> list = new ArrayList<>();
        list.add(longName);
        IndexAnnotationValidator.normalizeAndValidateAll(list, "t");
        assertEquals(64, longName.getName().length());
    }

    @Test
    void mysqlRenameIndexSql() {
        IndexInfo from = new IndexInfo();
        from.setName("oldname");
        IndexInfo to = new IndexInfo();
        to.setName("newname");
        TableInfo table = new TableInfo(NamingEntity.class);
        table.setName("t_demo");
        IndexRebuildTask task = new IndexRebuildTask(table, from, to, IndexRebuildTask.Reason.NAME);
        Map<TableInfo, Object> inner = new HashMap<>();
        inner.put(table, task);
        Map<String, Map<TableInfo, Object>> wrap = new HashMap<>();
        wrap.put(RelationalSchemaSql.paramName, inner);
        String sql = MysqlSchemaSql.INSTANCE.renameIndex(wrap);
        assertEquals("ALTER TABLE `t_demo` RENAME INDEX `oldname` TO `newname`;", sql);
    }
}
