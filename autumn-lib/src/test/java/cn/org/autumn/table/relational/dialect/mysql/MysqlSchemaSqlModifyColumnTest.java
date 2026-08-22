package cn.org.autumn.table.relational.dialect.mysql;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cn.org.autumn.table.annotation.Column;
import cn.org.autumn.table.data.ColumnInfo;
import cn.org.autumn.table.data.TableInfo;
import cn.org.autumn.table.relational.RelationalSchemaSql;
import cn.org.autumn.table.relational.model.TableMeta;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class MysqlSchemaSqlModifyColumnTest {

    static class Sensitive {
        @Column(isKey = true, type = "varchar", length = 16, isNull = false, comment = "固定值 'default'，单行表")
        String id;
    }

    @Test
    public void modifyExistingPrimaryKeyOmitsPrimaryKeyClause() throws Exception {
        ColumnInfo col = column();
        col.setExistingPrimaryKey(true);
        String sql = MysqlSchemaSql.INSTANCE.modifyColumn(wrap(col));
        assertTrue(sql.contains("MODIFY"));
        assertFalse(sql.contains("PRIMARY KEY"));
        assertTrue(sql.contains("COMMENT '固定值 ''default''，单行表'"));
    }

    @Test
    public void modifyPromotingToPrimaryKeyKeepsPrimaryKeyClause() throws Exception {
        String sql = MysqlSchemaSql.INSTANCE.modifyColumn(wrap(column()));
        assertTrue(sql.contains("PRIMARY KEY"));
    }

    @Test
    public void addPrimaryKeyColumnKeepsPrimaryKeyClause() throws Exception {
        String sql = MysqlSchemaSql.INSTANCE.addColumns(wrap(column()));
        assertTrue(sql.contains("ADD"));
        assertTrue(sql.contains("PRIMARY KEY"));
    }

    private static ColumnInfo column() throws Exception {
        ColumnInfo col = new ColumnInfo(Sensitive.class.getDeclaredField("id"));
        col.setTypeLength(1);
        return col;
    }

    private static Map<String, Map<TableInfo, ColumnInfo>> wrap(ColumnInfo col) {
        TableMeta meta = new TableMeta();
        meta.setTableName("im_message_sensitive_config");
        TableInfo table = new TableInfo(meta);
        Map<TableInfo, ColumnInfo> inner = new HashMap<>();
        inner.put(table, col);
        Map<String, Map<TableInfo, ColumnInfo>> map = new HashMap<>();
        map.put(RelationalSchemaSql.paramName, inner);
        return map;
    }
}
