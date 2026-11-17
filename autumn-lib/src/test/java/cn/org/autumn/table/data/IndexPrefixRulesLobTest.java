package cn.org.autumn.table.data;

import cn.org.autumn.table.annotation.Column;
import cn.org.autumn.table.annotation.Index;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IndexPrefixRulesLobTest {

    static class Sample {
        @Index
        @Column(type = DataType.TEXT, length = 255)
        String body;

        @Column(type = DataType.INT)
        Integer score;
    }

    @Test
    void requiresMysqlKeyPrefix_forTextFamily() {
        assertTrue(IndexPrefixRules.requiresMysqlKeyPrefix(DataType.TEXT));
        assertTrue(IndexPrefixRules.requiresMysqlKeyPrefix("longblob"));
        assertEquals(0, IndexPrefixRules.ensureMysqlKeyPrefixLength(DataType.INT, 0));
        assertEquals(IndexPrefixRules.DEFAULT_LOB_KEY_PREFIX_LENGTH,
                IndexPrefixRules.ensureMysqlKeyPrefixLength(DataType.TEXT, 0));
        assertEquals(100, IndexPrefixRules.ensureMysqlKeyPrefixLength(DataType.TEXT, 100));
    }

    @Test
    void effectivePrefixLength_autoAssignsForTextWithoutLength() throws Exception {
        Field f = Sample.class.getDeclaredField("body");
        Column col = f.getAnnotation(Column.class);
        assertEquals(IndexPrefixRules.DEFAULT_LOB_KEY_PREFIX_LENGTH,
                IndexPrefixRules.effectivePrefixLength(f, col, 0));
    }

    @Test
    void applyPrefixLengthPolicy_fillsLobPrefix() {
        Map<String, Integer> fields = new LinkedHashMap<>();
        fields.put("body", 0);
        fields.put("score", 255);
        IndexPrefixRules.applyPrefixLengthPolicy(fields, Sample.class);
        assertEquals(IndexPrefixRules.DEFAULT_LOB_KEY_PREFIX_LENGTH, fields.get("body").intValue());
        assertEquals(0, fields.get("score").intValue());
    }

    @Test
    void effectivePrefixLength_treatsAnnotationDefault255AsUnsetForText() throws Exception {
        Field f = Sample.class.getDeclaredField("body");
        Column col = f.getAnnotation(Column.class);
        assertEquals(255, col.length());
        assertEquals(IndexPrefixRules.DEFAULT_LOB_KEY_PREFIX_LENGTH,
                IndexPrefixRules.effectivePrefixLength(f, col, col.length()));
    }
}
