package cn.org.autumn.table.relational.support.ddl;

import cn.org.autumn.table.data.ColumnInfo;
import cn.org.autumn.table.relational.model.ColumnMeta;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class JdbcDdlColumnTypesAnsiTest {

    @Test
    public void h2UnknownLogicalTypeUsesVarchar255() {
        ColumnMeta m = new ColumnMeta();
        m.setColumnName("x");
        m.setDataType("some_mysql_only_type");
        m.setColumnType("some_mysql_only_type");
        m.setNullable("YES");
        ColumnInfo c = new ColumnInfo(m);
        assertEquals("VARCHAR(255)", JdbcDdlColumnTypes.ansiDoubleQuoted(c, AnsiDialect.H2));
    }

    @Test
    public void h2JsonMapsToClob() {
        ColumnMeta m = new ColumnMeta();
        m.setColumnName("j");
        m.setDataType("json");
        m.setColumnType("json");
        m.setNullable("YES");
        ColumnInfo c = new ColumnInfo(m);
        assertEquals("CLOB", JdbcDdlColumnTypes.ansiDoubleQuoted(c, AnsiDialect.H2));
    }

    @Test
    public void h2EnumUsesColumnLength() {
        ColumnMeta m = new ColumnMeta();
        m.setColumnName("e");
        m.setDataType("enum");
        m.setColumnType("enum");
        m.setNullable("YES");
        ColumnInfo c = new ColumnInfo(m);
        c.setLength(64);
        assertEquals("VARCHAR(64)", JdbcDdlColumnTypes.ansiDoubleQuoted(c, AnsiDialect.H2));
    }
}
