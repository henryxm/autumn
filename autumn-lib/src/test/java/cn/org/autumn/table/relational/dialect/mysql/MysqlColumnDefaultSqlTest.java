package cn.org.autumn.table.relational.dialect.mysql;

import cn.org.autumn.table.annotation.Column;
import cn.org.autumn.table.data.ColumnInfo;
import cn.org.autumn.table.data.DataType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.math.BigDecimal;
import java.util.Date;

class MysqlColumnDefaultSqlTest {

    static class Sample {
        @Column(type = DataType.INT, defaultValue = "0")
        Integer score;

        @Column(type = DataType.TINYINT, length = 1, defaultValue = "0")
        Integer flag;

        @Column(type = DataType.VARCHAR, length = 32, defaultValue = "")
        String name;

        @Column(type = DataType.DATETIME, defaultValue = "CURRENT_TIMESTAMP")
        Date created;

        @Column(type = DataType.DECIMAL, length = 10, decimalLength = 2, defaultValue = "0.5")
        BigDecimal amount;

        @Column(type = DataType.TEXT, defaultValue = "(JSON_ARRAY())")
        String expr;
    }

    @Test
    void append_numericUnquoted_timestampBare_stringQuoted() throws Exception {
        assertEquals(" DEFAULT 0", emit("score"));
        assertEquals(" DEFAULT 0", emit("flag"));
        assertEquals(" DEFAULT ''", emit("name"));
        assertEquals(" DEFAULT CURRENT_TIMESTAMP", emit("created"));
        assertEquals(" DEFAULT 0.5", emit("amount"));
    }

    @Test
    void append_keepsParenthesizedExpression() throws Exception {
        String clause = emit("expr");
        assertTrue(clause.contains("DEFAULT (JSON_ARRAY())"), clause);
    }

    private static String emit(String field) throws Exception {
        ColumnInfo info = new ColumnInfo(Sample.class.getDeclaredField(field));
        StringBuilder sb = new StringBuilder();
        MysqlColumnDefaultSql.appendDefaultClause(info, sb);
        return sb.toString();
    }
}
