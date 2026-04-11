package cn.org.autumn.table.relational.support.ddl;

import cn.org.autumn.table.data.ColumnInfo;
import org.apache.commons.lang3.StringUtils;

/**
 * 将实体侧 {@link ColumnInfo}（MySQL 风格逻辑类型）映射为各 JDBC 方言物理类型片段。
 */
public final class JdbcDdlColumnTypes {

    private JdbcDdlColumnTypes() {
    }

    public static String oracle(ColumnInfo c) {
        if (c == null) {
            return "VARCHAR2(1)";
        }
        String t = c.getType().toLowerCase();
        int len = Math.max(c.getLength(), 1);
        int dec = c.getDecimalLength();
        switch (t) {
            case "varchar":
                return "VARCHAR2(" + len + ")";
            case "char":
                return "CHAR(" + len + ")";
            case "int":
            case "integer":
                return "NUMBER(10)";
            case "tinyint":
                if (c.getLength() == 1) {
                    return "NUMBER(1)";
                }
                return "NUMBER(3)";
            case "smallint":
                return "NUMBER(5)";
            case "bigint":
                return "NUMBER(19)";
            case "text":
            case "tinytext":
            case "mediumtext":
            case "longtext":
                return "CLOB";
            case "datetime":
            case "timestamp":
                return "TIMESTAMP(0)";
            case "date":
                return "DATE";
            case "time":
                return "INTERVAL DAY TO SECOND";
            case "decimal":
                return "NUMBER(" + Math.max(len, 1) + "," + dec + ")";
            case "double":
                return "BINARY_DOUBLE";
            case "float":
                return "BINARY_FLOAT";
            case "blob":
            case "tinyblob":
            case "mediumblob":
            case "longblob":
                return "BLOB";
            case "year":
                return "NUMBER(4)";
            case "enum":
            case "set":
                return "VARCHAR2(255)";
            default:
                return t.toUpperCase();
        }
    }

    public static String sqlServer(ColumnInfo c) {
        if (c == null) {
            return "NVARCHAR(1)";
        }
        String t = c.getType().toLowerCase();
        int len = Math.max(c.getLength(), 1);
        int dec = c.getDecimalLength();
        switch (t) {
            case "varchar":
                return "NVARCHAR(" + Math.min(len, 4000) + ")";
            case "char":
                return "NCHAR(" + len + ")";
            case "int":
            case "integer":
                return "INT";
            case "tinyint":
                if (c.getLength() == 1) {
                    return "BIT";
                }
                return "TINYINT";
            case "smallint":
                return "SMALLINT";
            case "bigint":
                return "BIGINT";
            case "text":
            case "tinytext":
            case "mediumtext":
            case "longtext":
                return "NVARCHAR(MAX)";
            case "datetime":
            case "timestamp":
                return "DATETIME2(0)";
            case "date":
                return "DATE";
            case "time":
                return "TIME(0)";
            case "decimal":
                return "DECIMAL(" + Math.max(len, 1) + "," + dec + ")";
            case "double":
                return "FLOAT(53)";
            case "float":
                return "REAL";
            case "blob":
            case "tinyblob":
            case "mediumblob":
            case "longblob":
                return "VARBINARY(MAX)";
            case "year":
                return "SMALLINT";
            case "enum":
            case "set":
                return "NVARCHAR(255)";
            default:
                return t;
        }
    }

    /**
     * 双引号标识符系方言的通用类型（以 H2/DB2 系为基准，SQLite/Informix 等在调用处微调）。
     */
    public static String ansiDoubleQuoted(ColumnInfo c, AnsiDialect d) {
        if (c == null) {
            return "VARCHAR(1)";
        }
        String t = c.getType().toLowerCase();
        int len = Math.max(c.getLength(), 1);
        int dec = c.getDecimalLength();
        if (d == AnsiDialect.INFORMIX) {
            return informix(c, t, len, dec);
        }
        if (d == AnsiDialect.FIREBIRD) {
            return firebird(c, t, len, dec);
        }
        if (d == AnsiDialect.SQLITE) {
            return sqlite(c, t, len, dec);
        }
        // H2, HSQLDB, DB2, DERBY
        switch (t) {
            case "varchar":
                return "VARCHAR(" + len + ")";
            case "char":
                return "CHAR(" + len + ")";
            case "int":
            case "integer":
                return "INTEGER";
            case "tinyint":
                if (c.getLength() == 1) {
                    return d == AnsiDialect.HSQLDB ? "BOOLEAN" : "BOOLEAN";
                }
                return "SMALLINT";
            case "smallint":
                return "SMALLINT";
            case "bigint":
                return "BIGINT";
            case "text":
            case "tinytext":
            case "mediumtext":
            case "longtext":
                return d == AnsiDialect.DB2 || d == AnsiDialect.DERBY ? "CLOB" : "CLOB";
            case "datetime":
            case "timestamp":
                return "TIMESTAMP";
            case "date":
                return "DATE";
            case "time":
                return "TIME";
            case "decimal":
                return "DECIMAL(" + Math.max(len, 1) + "," + dec + ")";
            case "double":
                return "DOUBLE";
            case "float":
                return "REAL";
            case "blob":
            case "tinyblob":
            case "mediumblob":
            case "longblob":
                return "BLOB";
            case "year":
                return "SMALLINT";
            case "enum":
            case "set":
                return "VARCHAR(255)";
            default:
                return t;
        }
    }

    private static String sqlite(ColumnInfo c, String t, int len, int dec) {
        switch (t) {
            case "varchar":
            case "char":
            case "text":
            case "tinytext":
            case "mediumtext":
            case "longtext":
            case "enum":
            case "set":
                return "TEXT";
            case "int":
            case "integer":
            case "smallint":
            case "year":
                return "INTEGER";
            case "tinyint":
                return "INTEGER";
            case "bigint":
                return "INTEGER";
            case "datetime":
            case "timestamp":
            case "date":
            case "time":
                return "TEXT";
            case "decimal":
                return "NUMERIC(" + Math.max(len, 1) + "," + dec + ")";
            case "double":
            case "float":
                return "REAL";
            case "blob":
            case "tinyblob":
            case "mediumblob":
            case "longblob":
                return "BLOB";
            default:
                return "TEXT";
        }
    }

    private static String firebird(ColumnInfo c, String t, int len, int dec) {
        switch (t) {
            case "varchar":
                return "VARCHAR(" + len + ")";
            case "char":
                return "CHAR(" + len + ")";
            case "int":
            case "integer":
                return "INTEGER";
            case "tinyint":
            case "smallint":
                return "SMALLINT";
            case "bigint":
                return "BIGINT";
            case "text":
            case "tinytext":
            case "mediumtext":
            case "longtext":
                return "BLOB SUB_TYPE TEXT";
            case "datetime":
            case "timestamp":
                return "TIMESTAMP";
            case "date":
                return "DATE";
            case "time":
                return "TIME";
            case "decimal":
                return "DECIMAL(" + Math.max(len, 1) + "," + dec + ")";
            case "double":
                return "DOUBLE PRECISION";
            case "float":
                return "FLOAT";
            case "blob":
                return "BLOB";
            default:
                return "VARCHAR(255)";
        }
    }

    private static String informix(ColumnInfo c, String t, int len, int dec) {
        switch (t) {
            case "varchar":
            case "text":
            case "tinytext":
            case "mediumtext":
            case "longtext":
                return "LVARCHAR(" + Math.min(Math.max(len, 255), 32739) + ")";
            case "char":
                return "CHAR(" + len + ")";
            case "int":
            case "integer":
                return "INTEGER";
            case "bigint":
                return "BIGINT";
            case "smallint":
            case "tinyint":
                return "SMALLINT";
            case "datetime":
            case "timestamp":
                return "DATETIME YEAR TO FRACTION(3)";
            case "date":
                return "DATE";
            case "decimal":
                return "DECIMAL(" + Math.max(len, 1) + "," + dec + ")";
            case "double":
                return "FLOAT";
            case "float":
                return "SMALLFLOAT";
            case "blob":
                return "BYTE";
            default:
                return "LVARCHAR(255)";
        }
    }

    public static void appendDefaultClause(ColumnInfo c, StringBuilder sb, SqlLiteralQuote quote) {
        if (c == null || "NULL".equals(c.getDefaultValue())) {
            return;
        }
        String dv = c.getDefaultValue();
        if (StringUtils.isBlank(dv) || "NULL".equalsIgnoreCase(dv)) {
            return;
        }
        sb.append(" DEFAULT ");
        if (isTinyintBoolean(c) && dv.matches("[01]")) {
            sb.append(dialectBooleanLiteral(dv, quote));
            return;
        }
        if (dv.matches("-?\\d+") && isNumericType(c.getType())) {
            sb.append(dv);
            return;
        }
        if ("true".equalsIgnoreCase(dv) || "false".equalsIgnoreCase(dv)) {
            sb.append(dialectBooleanLiteral("true".equalsIgnoreCase(dv) ? "1" : "0", quote));
            return;
        }
        char q = '\'';
        sb.append(q).append(dv.replace(String.valueOf(q), String.valueOf(q) + q)).append(q);
    }

    private static String dialectBooleanLiteral(String bit, SqlLiteralQuote quote) {
        switch (quote) {
            case ORACLE_STRING:
                return "0".equals(bit) ? "'N'" : "'Y'";
            case SQLSERVER_BIT:
                return "0".equals(bit) ? "0" : "1";
            default:
                return "0".equals(bit) ? "FALSE" : "TRUE";
        }
    }

    public enum SqlLiteralQuote {
        ANSI,
        SQLSERVER_BIT,
        ORACLE_STRING
    }

    private static boolean isTinyintBoolean(ColumnInfo columnInfo) {
        return columnInfo != null && "tinyint".equalsIgnoreCase(columnInfo.getType())
                && columnInfo.getLength() == 1;
    }

    private static boolean isNumericType(String type) {
        if (type == null) {
            return false;
        }
        String t = type.toLowerCase();
        return t.contains("int") || t.equals("decimal") || t.equals("float") || t.equals("double")
                || t.equals("real") || t.equals("numeric");
    }
}
