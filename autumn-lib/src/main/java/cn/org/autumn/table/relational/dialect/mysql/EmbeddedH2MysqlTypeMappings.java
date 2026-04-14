package cn.org.autumn.table.relational.dialect.mysql;

import cn.org.autumn.table.data.ColumnInfo;
import cn.org.autumn.table.data.DataType;

import java.util.Locale;

/**
 * 将实体侧 MySQL 语义 {@link ColumnInfo} 映射为 H2 {@code MODE=MySQL} 下可稳定解析的列类型片段（仅类型关键字及括号部分，不含列名）。
 * <p>
 * 覆盖：近似数精度、整型显示宽度、JSON/枚举/集合、文本子类型、YEAR、BIT、MEDIUMINT、NUMERIC 等常见隐患。
 */
public final class EmbeddedH2MysqlTypeMappings {

    private EmbeddedH2MysqlTypeMappings() {
    }

    public static void appendPhysicalType(ColumnInfo c, StringBuilder sb) {
        if (c == null || c.getType() == null) {
            sb.append("varchar(255)");
            return;
        }
        int tl = c.getTypeLength();
        String base = c.getType();
        String t = base.toLowerCase(Locale.ROOT);
        if (DataType.ENUM.equals(t) || DataType.SET.equals(t)) {
            int len = Math.max(1, Math.min(Math.max(c.getLength(), 1), 65535));
            sb.append("varchar(").append(len).append(")");
            return;
        }
        if (tl == 0) {
            appendTypeLengthZero(t, base, sb);
            return;
        }
        if (tl == 1) {
            appendTypeLengthOne(t, base, c, sb);
            return;
        }
        if (tl == 2) {
            appendTypeLengthTwo(t, base, c, sb);
            return;
        }
        sb.append(base);
    }

    private static void appendTypeLengthZero(String t, String base, StringBuilder sb) {
        switch (t) {
            case "json":
            case "jsonb":
                sb.append("longtext");
                break;
            case "tinytext":
            case "mediumtext":
            case "text":
            case "longtext":
                sb.append("longtext");
                break;
            case "year":
                sb.append("smallint");
                break;
            case "bit":
                sb.append("tinyint(1)");
                break;
            case "mediumint":
                sb.append("int");
                break;
            case "geometry":
            case "point":
            case "linestring":
            case "polygon":
            case "multipoint":
            case "multilinestring":
            case "multipolygon":
            case "geometrycollection":
                sb.append("longblob");
                break;
            default:
                sb.append(base);
                break;
        }
    }

    private static void appendTypeLengthOne(String t, String base, ColumnInfo c, StringBuilder sb) {
        // MySQL 整型 (n) 多为显示宽度，H2 易与类型精度混淆，直接省略括号
        switch (t) {
            case "int":
            case "integer":
                sb.append("int");
                return;
            case "bigint":
                sb.append("bigint");
                return;
            case "smallint":
                sb.append("smallint");
                return;
            case "mediumint":
                sb.append("int");
                return;
            case "tinyint":
                sb.append("tinyint(").append(c.getLength()).append(")");
                return;
            case "float":
            case "double":
            case "real":
                sb.append(approximateNumericKeyword(t));
                return;
            default:
                break;
        }
        sb.append(base).append("(").append(c.getLength()).append(")");
    }

    private static void appendTypeLengthTwo(String t, String base, ColumnInfo c, StringBuilder sb) {
        if (DataType.DOUBLE.equals(t) || DataType.FLOAT.equals(t) || "real".equals(t)) {
            sb.append(approximateNumericKeyword(t));
            return;
        }
        if (DataType.DECIMAL.equals(t) || "numeric".equals(t)) {
            sb.append("decimal(").append(c.getLength()).append(",").append(c.getDecimalLength()).append(")");
            return;
        }
        sb.append(base).append("(").append(c.getLength()).append(",").append(c.getDecimalLength()).append(")");
    }

    private static String approximateNumericKeyword(String t) {
        if (DataType.FLOAT.equals(t) || "real".equals(t)) {
            return "float";
        }
        return "double";
    }
}
