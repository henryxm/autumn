package cn.org.autumn.table.data.index;

import cn.org.autumn.table.annotation.Column;
import cn.org.autumn.table.annotation.Index;
import cn.org.autumn.table.annotation.IndexField;
import cn.org.autumn.table.annotation.IndexOrder;
import cn.org.autumn.table.annotation.Indexes;
import cn.org.autumn.table.annotation.Table;
import cn.org.autumn.table.data.IndexInfo;
import cn.org.autumn.table.data.TableInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 索引限制预检：重复列、列数、名截断碰撞、键长粗估、isUnique+空串、forEmit skip。
 */
class IndexLimitGuardTest {

    @AfterEach
    void clearProps() {
        System.clearProperty(IndexTableSettings.PROP_SKIP_ON_LIMIT_VIOLATION);
        System.clearProperty(IndexTableSettings.PROP_MAX_COLUMNS_PER_INDEX);
        System.clearProperty(IndexTableSettings.PROP_MAX_KEY_BYTES);
        System.clearProperty(IndexTableSettings.PROP_MAX_PER_TABLE);
        System.clearProperty(IndexTableSettings.PROP_WARN_UNIQUE_BLANK_DEFAULT);
        System.clearProperty(IndexTableSettings.PROP_MAX_NAME_LENGTH);
    }

    @Test
    void dedupeDuplicateColumns() {
        IndexInfo idx = new IndexInfo();
        idx.setName("dup");
        idx.setIndexType("NORMAL");
        idx.setIndexMethod("BTREE");
        idx.appendColumn("revocation_time", 0, IndexOrder.ASC);
        idx.appendColumn("user", 0, IndexOrder.ASC);
        idx.appendColumn("revocation_time", 0, IndexOrder.ASC);
        IndexLimitGuard.dedupeColumns(idx, "t");
        assertEquals(2, idx.getColumnSpecs().size());
        assertEquals("revocation_time", idx.getColumnSpecs().get(0).getColumn());
        assertEquals("user", idx.getColumnSpecs().get(1).getColumn());
    }

    @Test
    void tooManyColumnsSetsSkipEmit() {
        IndexInfo idx = new IndexInfo();
        idx.setName("wide");
        idx.setIndexType("NORMAL");
        idx.setIndexMethod("BTREE");
        for (int i = 0; i < 17; i++) {
            idx.appendColumn("c" + i, 0, IndexOrder.ASC);
        }
        List<IndexInfo> list = new ArrayList<>();
        list.add(idx);
        IndexLimitGuard.guard(list, "t", null);
        assertTrue(idx.isSkipEmit());
        assertNull(IndexDialectCapability.forEmit(IndexDialectCapability.Family.MYSQL, idx));
    }

    @Test
    void maxColumnsZeroDisablesCheck() {
        System.setProperty(IndexTableSettings.PROP_MAX_COLUMNS_PER_INDEX, "0");
        IndexInfo idx = new IndexInfo();
        idx.setName("wide");
        idx.setIndexType("NORMAL");
        idx.setIndexMethod("BTREE");
        for (int i = 0; i < 17; i++) {
            idx.appendColumn("c" + i, 0, IndexOrder.ASC);
        }
        List<IndexInfo> list = new ArrayList<>();
        list.add(idx);
        IndexLimitGuard.guard(list, "t", null);
        assertFalse(idx.isSkipEmit());
    }

    @Test
    void unknownColumnSetsSkipEmit() {
        IndexInfo idx = new IndexInfo();
        idx.setName("badcol");
        idx.setIndexType("NORMAL");
        idx.setIndexMethod("BTREE");
        idx.appendColumn("not_a_real_column", 0, IndexOrder.ASC);
        List<IndexInfo> list = new ArrayList<>();
        list.add(idx);
        IndexLimitGuard.guard(list, "t_demo", DemoEntity.class);
        assertTrue(idx.isSkipEmit());
    }

    @Test
    void parentClassColumnIsKnown() {
        IndexInfo idx = new IndexInfo();
        idx.setName("parentcol");
        idx.setIndexType("NORMAL");
        idx.setIndexMethod("BTREE");
        idx.appendColumn("base_uuid", 0, IndexOrder.ASC);
        idx.appendColumn("tag", 0, IndexOrder.ASC);
        List<IndexInfo> list = new ArrayList<>();
        list.add(idx);
        IndexLimitGuard.guard(list, "t_child", ChildEntity.class);
        assertFalse(idx.isSkipEmit(), "父类字段不得被误判为未知列");
        assertTrue(IndexDialectCapability.canEmit(IndexDialectCapability.Family.MYSQL, idx));
    }

    @Test
    void canEmitRespectsSkipEmit() {
        IndexInfo idx = new IndexInfo();
        idx.setName("x");
        idx.setIndexType("NORMAL");
        idx.setIndexMethod("BTREE");
        idx.appendColumn("user", 0, IndexOrder.ASC);
        idx.setSkipEmit(true);
        assertFalse(IndexDialectCapability.canEmit(IndexDialectCapability.Family.MYSQL, idx));
        assertNull(IndexDialectCapability.forEmit(IndexDialectCapability.Family.MYSQL, idx));
    }

    @Test
    void truncateNameCollisionWarns() {
        IndexInfo a = new IndexInfo();
        a.setName("x".repeat(70) + "_a");
        a.setIndexType("NORMAL");
        a.setIndexMethod("BTREE");
        a.appendColumn("user", 0, IndexOrder.ASC);
        IndexInfo b = new IndexInfo();
        b.setName("x".repeat(70) + "_b");
        b.setIndexType("NORMAL");
        b.setIndexMethod("BTREE");
        b.appendColumn("tag", 0, IndexOrder.ASC);
        List<IndexInfo> list = new ArrayList<>();
        list.add(a);
        list.add(b);
        IndexAnnotationValidator.normalizeAndValidateAll(list, "t", DemoEntity.class, 0);
        assertEquals(a.getName(), b.getName());
        assertEquals(64, a.getName().length());
    }

    @Test
    void keyBytesEstimateOverLimitWarnsAndStrictSkip() {
        System.setProperty(IndexTableSettings.PROP_SKIP_ON_LIMIT_VIOLATION, "true");
        IndexInfo idx = new IndexInfo();
        idx.setName("widevarchar");
        idx.setIndexType("NORMAL");
        idx.setIndexMethod("BTREE");
        // 4 * 255 * 4 = 4080 > 3072
        idx.appendColumn("a", 0, IndexOrder.ASC);
        idx.appendColumn("b", 0, IndexOrder.ASC);
        idx.appendColumn("c", 0, IndexOrder.ASC);
        idx.appendColumn("d", 0, IndexOrder.ASC);
        List<IndexInfo> list = new ArrayList<>();
        list.add(idx);
        IndexLimitGuard.guard(list, "t", WideEntity.class);
        assertTrue(idx.isSkipEmit());
    }

    @Test
    void keyBytesEstimateIgnoresDefaultVarcharOnBooleanInt() {
        // 对齐 shop_goods_basic.deletedstatusapplevisible：boolean/int 注解默认 type=VARCHAR
        IndexInfo idx = new IndexInfo();
        idx.setName("deletedstatusapplevisible");
        idx.setIndexType("NORMAL");
        idx.setIndexMethod("BTREE");
        idx.appendColumn("deleted", 0, IndexOrder.ASC);
        idx.appendColumn("status", 0, IndexOrder.ASC);
        idx.appendColumn("apple", 0, IndexOrder.ASC);
        idx.appendColumn("visible", 0, IndexOrder.ASC);
        int bytes = IndexLimitGuard.estimateKeyBytes(idx.getColumnSpecs(), FlagIndexEntity.class);
        // 1 + 4 + 1 + 4 = 10，远低于 3072；旧逻辑会误算 4*255*4=4080
        assertEquals(10, bytes);
        assertTrue(bytes < 3072);

        System.setProperty(IndexTableSettings.PROP_SKIP_ON_LIMIT_VIOLATION, "true");
        List<IndexInfo> list = new ArrayList<>();
        list.add(idx);
        IndexLimitGuard.guard(list, "shop_goods_basic", FlagIndexEntity.class);
        assertFalse(idx.isSkipEmit());
    }

    @Test
    void classifyAddFailureHumanHints() {
        assertTrue(IndexLimitGuard.classifyAddFailure("ERROR 1070 (42000): Too many key parts").contains("列数"));
        assertTrue(IndexLimitGuard.classifyAddFailure("Specified key was too long for this key").contains("键长"));
        assertTrue(IndexLimitGuard.classifyAddFailure("Duplicate entry '' for key 'uk'").contains("空串"));
        assertTrue(IndexLimitGuard.classifyAddFailure("Duplicate column name 'x'").contains("重复列"));
        assertEquals("", IndexLimitGuard.classifyAddFailure("some other error"));
        // 裸数字 / 时间戳不得误伤
        assertEquals("", IndexLimitGuard.classifyAddFailure("retry at 20241107107012"));
        assertEquals("", IndexLimitGuard.classifyAddFailure("line 1062 of script"));
    }

    @Test
    void blankStringDefaultDetection() {
        assertTrue(IndexLimitGuard.isBlankStringDefault(""));
        assertTrue(IndexLimitGuard.isBlankStringDefault("''"));
        assertTrue(IndexLimitGuard.isBlankStringDefault("\"\""));
        assertFalse(IndexLimitGuard.isBlankStringDefault("NULL"));
        assertFalse(IndexLimitGuard.isBlankStringDefault("null"));
        assertFalse(IndexLimitGuard.isBlankStringDefault("abc"));
    }

    @Test
    void dialectMaxNameLengthBranches() {
        assertEquals(64, IndexDialectCapability.maxIndexNameLength(IndexDialectCapability.Family.MYSQL));
        assertEquals(63, IndexDialectCapability.maxIndexNameLength(IndexDialectCapability.Family.POSTGRES));
        assertEquals(30, IndexDialectCapability.maxIndexNameLength(IndexDialectCapability.Family.ORACLE));
        System.setProperty(IndexTableSettings.PROP_MAX_NAME_LENGTH, "40");
        assertEquals(40, IndexDialectCapability.maxIndexNameLength(IndexDialectCapability.Family.MYSQL));
    }

    @Test
    void nullIndexesWithExtraCountDoesNotNpe() {
        IndexLimitGuard.guard(null, "t", null, 70);
    }

    @Test
    void tableInfoCollectsWithoutThrowingOnUniqueBlank() {
        TableInfo ti = new TableInfo(UniqueBlankEntity.class);
        assertNotNull(ti.getName());
        assertEquals("t_unique_blank", ti.getName());
        assertFalse(ti.getIndexInfos().isEmpty());
    }

    @Table(value = "t_demo")
    static class DemoEntity {
        @Column
        private String user;
        @Column
        private String tag;
    }

    static class ParentEntity {
        @Column
        private String baseUuid;
    }

    @Table(value = "t_child")
    static class ChildEntity extends ParentEntity {
        @Column
        private String tag;
    }

    @Table(value = "t_wide")
    static class WideEntity {
        @Column(type = "varchar", length = 255)
        private String a;
        @Column(type = "varchar", length = 255)
        private String b;
        @Column(type = "varchar", length = 255)
        private String c;
        @Column(type = "varchar", length = 255)
        private String d;
    }

    /** 复现 GoodsBasicEntity 标志位索引：@Column 未写 type，默认 VARCHAR。 */
    @Table(value = "t_flag_idx")
    static class FlagIndexEntity {
        @Column(defaultValue = "0")
        private boolean deleted;
        @Column(defaultValue = "0")
        private int status;
        @Column(defaultValue = "0")
        private boolean apple;
        @Column(defaultValue = "0")
        private int visible;
    }

    @Table(value = "t_unique_blank")
    @Indexes({
            @Index(name = "usertag", fields = {
                    @IndexField(field = "user"),
                    @IndexField(field = "tag")
            })
    })
    static class UniqueBlankEntity {
        @Column(isUnique = true, defaultValue = "", isNull = true)
        private String code;
        @Column
        private String user;
        @Column
        private String tag;
    }
}
