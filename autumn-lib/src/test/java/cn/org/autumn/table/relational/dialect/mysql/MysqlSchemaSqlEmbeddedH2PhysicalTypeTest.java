package cn.org.autumn.table.relational.dialect.mysql;

import cn.org.autumn.table.annotation.Column;
import cn.org.autumn.table.data.ColumnInfo;
import cn.org.autumn.table.data.DataType;
import cn.org.autumn.table.relational.model.ColumnMeta;
import org.junit.Test;

import java.lang.reflect.Field;

import static org.junit.Assert.assertEquals;

public class MysqlSchemaSqlEmbeddedH2PhysicalTypeTest {

    @Test
    public void doubleWithPrecisionOmitsScaleForH2() {
        ColumnMeta m = new ColumnMeta();
        m.setColumnName("x");
        m.setDataType("double");
        m.setColumnType("double(11,2)");
        m.setNullable("YES");
        ColumnInfo c = new ColumnInfo(m);
        c.setTypeLength(2);
        assertEquals("double", physical(c));
    }

    @Test
    public void floatWithPrecisionOmitsScaleForH2() {
        ColumnMeta m = new ColumnMeta();
        m.setColumnName("x");
        m.setDataType("float");
        m.setColumnType("float(7,4)");
        m.setNullable("YES");
        ColumnInfo c = new ColumnInfo(m);
        c.setTypeLength(2);
        assertEquals("float", physical(c));
    }

    @Test
    public void decimalKeepsPrecision() {
        ColumnMeta m = new ColumnMeta();
        m.setColumnName("x");
        m.setDataType("decimal");
        m.setColumnType("decimal(10,2)");
        m.setNullable("YES");
        ColumnInfo c = new ColumnInfo(m);
        c.setTypeLength(2);
        assertEquals("decimal(10,2)", physical(c));
    }

    @Test
    public void jsonMapsToLongtext() {
        ColumnMeta m = new ColumnMeta();
        m.setColumnName("j");
        m.setDataType("json");
        m.setColumnType("json");
        m.setNullable("YES");
        ColumnInfo c = new ColumnInfo(m);
        c.setTypeLength(0);
        assertEquals("longtext", physical(c));
    }

    @Test
    public void mediumtextMapsToLongtext() throws Exception {
        class T {
            @Column(type = DataType.MEDIUMTEXT)
            String m;
        }
        Field f = T.class.getDeclaredField("m");
        ColumnInfo c = new ColumnInfo(f, null, null);
        c.setTypeLength(0);
        assertEquals("longtext", physical(c));
    }

    @Test
    public void enumMapsToVarchar() throws Exception {
        class T {
            @Column(type = DataType.ENUM, length = 40)
            String e;
        }
        Field f = T.class.getDeclaredField("e");
        ColumnInfo c = new ColumnInfo(f, null, null);
        c.setTypeLength(0);
        assertEquals("varchar(40)", physical(c));
    }

    @Test
    public void textMapsToLongtext() throws Exception {
        class T {
            @Column(type = DataType.TEXT)
            String x;
        }
        Field f = T.class.getDeclaredField("x");
        ColumnInfo c = new ColumnInfo(f, null, null);
        c.setTypeLength(0);
        assertEquals("longtext", physical(c));
    }

    @Test
    public void bigintDisplayWidthStripped() {
        ColumnMeta m = new ColumnMeta();
        m.setColumnName("id");
        m.setDataType("bigint");
        m.setColumnType("bigint(20)");
        m.setNullable("YES");
        ColumnInfo c = new ColumnInfo(m);
        c.setTypeLength(1);
        assertEquals("bigint", physical(c));
    }

    @Test
    public void realWithPrecisionOmitsScale() {
        ColumnMeta m = new ColumnMeta();
        m.setColumnName("r");
        m.setDataType("real");
        m.setColumnType("real(10,5)");
        m.setNullable("YES");
        ColumnInfo c = new ColumnInfo(m);
        c.setTypeLength(2);
        assertEquals("float", physical(c));
    }

    @Test
    public void geometryMapsToLongblob() {
        ColumnMeta m = new ColumnMeta();
        m.setColumnName("g");
        m.setDataType("geometry");
        m.setColumnType("geometry");
        m.setNullable("YES");
        ColumnInfo c = new ColumnInfo(m);
        c.setTypeLength(0);
        assertEquals("longblob", physical(c));
    }

    @Test
    public void numericMapsToDecimalSyntax() {
        ColumnMeta m = new ColumnMeta();
        m.setColumnName("n");
        m.setDataType("numeric");
        m.setColumnType("numeric(12,4)");
        m.setNullable("YES");
        ColumnInfo c = new ColumnInfo(m);
        c.setTypeLength(2);
        assertEquals("decimal(12,4)", physical(c));
    }

    private static String physical(ColumnInfo c) {
        StringBuilder sb = new StringBuilder();
        EmbeddedH2MysqlTypeMappings.appendPhysicalType(c, sb);
        return sb.toString();
    }
}
