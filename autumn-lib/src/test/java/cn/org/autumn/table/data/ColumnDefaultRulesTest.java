package cn.org.autumn.table.data;

import cn.org.autumn.table.annotation.Column;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ColumnDefaultRulesTest {

    static class Sample {
        @Column(type = DataType.TEXT, defaultValue = "")
        String body;

        @Column(type = DataType.BLOB, defaultValue = "x")
        byte[] bin;

        @Column(type = DataType.VARCHAR, length = 64, defaultValue = "")
        String name;

        @Column(isKey = true, isAutoIncrement = true, defaultValue = "0")
        Long id;

        @Column(type = "json", defaultValue = "{}")
        String payload;

        @Column(type = DataType.INT, defaultValue = "")
        Integer score;

        @Column(type = DataType.TEXT, defaultValue = "(JSON_ARRAY())")
        String exprBody;

        @Column(type = " JSON ", defaultValue = "x")
        String spacedJson;
    }

    @Test
    void willEmitMysqlDefault_treatsEmptyStringAsEmit() {
        assertTrue(ColumnDefaultRules.willEmitMysqlDefault(""));
        assertTrue(ColumnDefaultRules.willEmitMysqlDefault("0"));
        assertFalse(ColumnDefaultRules.willEmitMysqlDefault("NULL"));
        assertFalse(ColumnDefaultRules.willEmitMysqlDefault(null));
    }

    @Test
    void forbidsExplicitMysqlDefault_coversLobAndJson_normalized() {
        assertTrue(ColumnDefaultRules.forbidsExplicitMysqlDefault(DataType.TEXT));
        assertTrue(ColumnDefaultRules.forbidsExplicitMysqlDefault("MEDIUMBLOB"));
        assertTrue(ColumnDefaultRules.forbidsExplicitMysqlDefault("json"));
        assertTrue(ColumnDefaultRules.forbidsExplicitMysqlDefault(" JSON "));
        assertFalse(ColumnDefaultRules.forbidsExplicitMysqlDefault(DataType.VARCHAR));
    }

    @Test
    void forbidsMysqlBtreeKey_jsonAndGeometry() {
        assertTrue(ColumnDefaultRules.forbidsMysqlBtreeKey("json"));
        assertTrue(ColumnDefaultRules.forbidsMysqlBtreeKey("geometry"));
        assertFalse(ColumnDefaultRules.forbidsMysqlBtreeKey(DataType.TEXT));
        assertFalse(ColumnDefaultRules.forbidsMysqlBtreeKey(DataType.VARCHAR));
    }

    @Test
    void sanitize_clearsEmptyDefaultOnText() throws Exception {
        ColumnInfo info = new ColumnInfo(Sample.class.getDeclaredField("body"));
        assertEquals("", info.getDefaultValue());
        assertTrue(ColumnDefaultRules.sanitizeMysqlColumnDefault(info, Sample.class.getName()));
        assertEquals("NULL", info.getDefaultValue());
        assertFalse(ColumnDefaultRules.willEmitMysqlDefault(info.getDefaultValue()));
    }

    @Test
    void sanitize_clearsDefaultOnBlobAndJson() throws Exception {
        ColumnInfo blob = new ColumnInfo(Sample.class.getDeclaredField("bin"));
        assertTrue(ColumnDefaultRules.sanitizeMysqlColumnDefault(blob, "t"));
        assertEquals("NULL", blob.getDefaultValue());

        ColumnInfo json = new ColumnInfo(Sample.class.getDeclaredField("payload"));
        assertTrue(ColumnDefaultRules.sanitizeMysqlColumnDefault(json, "t"));
        assertEquals("NULL", json.getDefaultValue());
    }

    @Test
    void sanitize_keepsEmptyDefaultOnVarchar() throws Exception {
        ColumnInfo info = new ColumnInfo(Sample.class.getDeclaredField("name"));
        assertFalse(ColumnDefaultRules.sanitizeMysqlColumnDefault(info, Sample.class.getName()));
        assertEquals("", info.getDefaultValue());
    }

    @Test
    void sanitize_clearsEmptyDefaultOnInt() throws Exception {
        ColumnInfo info = new ColumnInfo(Sample.class.getDeclaredField("score"));
        assertTrue(ColumnDefaultRules.sanitizeMysqlColumnDefault(info, Sample.class.getName()));
        assertEquals("NULL", info.getDefaultValue());
    }

    @Test
    void sanitize_keepsParenthesizedExpressionOnText() throws Exception {
        ColumnInfo info = new ColumnInfo(Sample.class.getDeclaredField("exprBody"));
        assertFalse(ColumnDefaultRules.sanitizeMysqlColumnDefault(info, Sample.class.getName()));
        assertEquals("(JSON_ARRAY())", info.getDefaultValue());
    }

    @Test
    void sanitize_clearsDefaultOnAutoIncrement() throws Exception {
        Field f = Sample.class.getDeclaredField("id");
        ColumnInfo info = new ColumnInfo(f);
        assertTrue(info.isAutoIncrement());
        assertTrue(ColumnDefaultRules.sanitizeMysqlColumnDefault(info, Sample.class.getName()));
        assertEquals("NULL", info.getDefaultValue());
    }

    @Test
    void sanitize_normalizesSpacedJsonType() throws Exception {
        ColumnInfo info = new ColumnInfo(Sample.class.getDeclaredField("spacedJson"));
        assertTrue(ColumnDefaultRules.sanitizeMysqlColumnDefault(info, "t"));
        assertEquals("NULL", info.getDefaultValue());
    }
}
