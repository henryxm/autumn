package cn.org.autumn.table.relational.dialect.mysql;

import cn.org.autumn.table.annotation.Column;
import cn.org.autumn.table.annotation.Table;
import cn.org.autumn.table.data.ColumnInfo;
import cn.org.autumn.table.data.DataType;
import cn.org.autumn.table.data.TableInfo;
import cn.org.autumn.table.relational.RelationalSchemaSql;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TEXT/BLOB 上 defaultValue（含空串）不得进入 MySQL DDL；isUnique 须带前缀；JSON isUnique 跳过。
 */
public class MysqlSchemaSqlLobDefaultTest {

    @Table(comment = "t's note")
    static class LobEntity {
        @Column(type = DataType.TEXT, defaultValue = "", isUnique = true, comment = "body")
        String body;

        @Column(type = DataType.VARCHAR, length = 32, defaultValue = "", comment = "name")
        String name;

        @Column(type = DataType.INT, defaultValue = "0", comment = "score")
        Integer score;

        @Column(type = "json", isUnique = true, comment = "payload")
        String payload;
    }

    @Test
    void createTable_skipsDefaultOnText_keepsOnVarchar_andPrefixesUnique() throws Exception {
        ColumnInfo text = new ColumnInfo(LobEntity.class.getDeclaredField("body"));
        text.setTypeLength(0);
        ColumnInfo varchar = new ColumnInfo(LobEntity.class.getDeclaredField("name"));
        varchar.setTypeLength(1);
        ColumnInfo score = new ColumnInfo(LobEntity.class.getDeclaredField("score"));
        score.setTypeLength(1);
        ColumnInfo json = new ColumnInfo(LobEntity.class.getDeclaredField("payload"));
        json.setTypeLength(0);

        TableInfo tableInfo = new TableInfo(LobEntity.class);
        Map<TableInfo, List<ColumnInfo>> inner = new HashMap<>();
        inner.put(tableInfo, List.of(text, varchar, score, json));
        Map<String, Map<TableInfo, List<ColumnInfo>>> map = new HashMap<>();
        map.put(RelationalSchemaSql.paramName, inner);

        String sql = MysqlSchemaSql.INSTANCE.createTable(map);
        assertFalse(sql.matches("(?is).*`body`\\s+text[^,]*DEFAULT.*"), sql);
        assertTrue(sql.matches("(?is).*`name`\\s+varchar\\(32\\)[^,]*DEFAULT\\s+''.*"), sql);
        assertTrue(sql.matches("(?is).*`score`\\s+int\\(11\\)[^,]*DEFAULT\\s+0(?:\\s|,|$).*")
                        || sql.matches("(?is).*`score`\\s+int\\(\\d+\\)[^,]*DEFAULT\\s+0.*"),
                sql);
        assertTrue(sql.contains("UNIQUE KEY (`body`(191))"), sql);
        assertFalse(sql.contains("UNIQUE KEY (`payload`"), sql);
        assertTrue(sql.contains("COMMENT='t''s note'"), sql);
    }
}
