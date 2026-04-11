package cn.org.autumn.table.relational.dialect.sqlserver;

import cn.org.autumn.table.relational.RelationalSchemaSql;
import cn.org.autumn.table.relational.support.SchemaSqlNoops;
import cn.org.autumn.table.relational.support.ddl.SqlServerJdbcDdlGenerator;
import cn.org.autumn.table.data.ColumnInfo;
import cn.org.autumn.table.data.IndexInfo;
import cn.org.autumn.table.data.TableInfo;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;

/**
 * Microsoft SQL Server：{@code INFORMATION_SCHEMA} + 当前架构；DDL 由 {@link SqlServerJdbcDdlGenerator} 生成。
 */
public final class SqlServerRelationalSchemaSql implements RelationalSchemaSql {

    public static final SqlServerRelationalSchemaSql INSTANCE = new SqlServerRelationalSchemaSql();

    @Override
    public String getColumnMetas() {
        return "SELECT CAST(NULL AS VARCHAR(1)) AS tableCatalog, c.TABLE_SCHEMA AS tableSchema, c.TABLE_NAME AS tableName, "
                + "c.COLUMN_NAME AS columnName, CAST(c.ORDINAL_POSITION AS VARCHAR(20)) AS ordinalPosition, "
                + "c.COLUMN_DEFAULT AS columnDefault, c.IS_NULLABLE AS isNullable, c.DATA_TYPE AS dataType, "
                + "c.CHARACTER_MAXIMUM_LENGTH AS characterMaximumLength, c.CHARACTER_OCTET_LENGTH AS characterOctetLength, "
                + "c.NUMERIC_PRECISION AS numericPrecision, c.NUMERIC_SCALE AS numericScale, "
                + "c.DATETIME_PRECISION AS datetimePrecision, c.CHARACTER_SET_NAME AS characterSetName, "
                + "c.COLLATION_NAME AS collationName, "
                + "(c.DATA_TYPE + COALESCE('(' + CAST(c.CHARACTER_MAXIMUM_LENGTH AS VARCHAR(20)) + ')', '')) AS columnType, "
                + "CASE WHEN pk.COLUMN_NAME IS NOT NULL THEN 'PRI' ELSE '' END AS columnKey, "
                + "CAST(ep.value AS NVARCHAR(4000)) AS columnComment, CAST('' AS VARCHAR(1)) AS extra "
                + "FROM INFORMATION_SCHEMA.COLUMNS c "
                + "LEFT JOIN ( "
                + "  SELECT ku.TABLE_SCHEMA, ku.TABLE_NAME, ku.COLUMN_NAME "
                + "  FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS tc "
                + "  JOIN INFORMATION_SCHEMA.KEY_COLUMN_USAGE ku ON tc.CONSTRAINT_SCHEMA = ku.CONSTRAINT_SCHEMA "
                + "    AND tc.CONSTRAINT_NAME = ku.CONSTRAINT_NAME "
                + "  WHERE tc.CONSTRAINT_TYPE = 'PRIMARY KEY' "
                + ") pk ON pk.TABLE_SCHEMA = c.TABLE_SCHEMA AND pk.TABLE_NAME = c.TABLE_NAME AND pk.COLUMN_NAME = c.COLUMN_NAME "
                + "LEFT JOIN sys.extended_properties ep ON ep.major_id = OBJECT_ID(QUOTENAME(c.TABLE_SCHEMA) + '.' + QUOTENAME(c.TABLE_NAME)) "
                + "  AND ep.minor_id = COLUMNPROPERTY(OBJECT_ID(QUOTENAME(c.TABLE_SCHEMA) + '.' + QUOTENAME(c.TABLE_NAME)), c.COLUMN_NAME, 'ColumnId') "
                + "  AND ep.name = 'MS_Description' AND ep.class = 1 "
                + "WHERE c.TABLE_SCHEMA = SCHEMA_NAME() AND c.TABLE_NAME = #{" + RelationalSchemaSql.paramName + "} "
                + "ORDER BY c.ORDINAL_POSITION";
    }

    @Override
    public String getTableMetas(Map<String, Object> map) {
        String tableName = (String) map.get(RelationalSchemaSql.paramName);
        int offset = map.containsKey("offset") ? Math.max(0, (int) map.get("offset")) : 0;
        int rows = map.containsKey("rows") ? (int) map.get("rows") : Integer.MAX_VALUE;
        if (rows <= 0) {
            rows = Integer.MAX_VALUE;
        }
        String where = "";
        if (StringUtils.isNotBlank(tableName)) {
            where = " AND TABLE_NAME = #{" + RelationalSchemaSql.paramName + "} ";
        }
        return "SELECT TABLE_CATALOG AS tableCatalog, TABLE_SCHEMA AS tableSchema, TABLE_NAME AS tableName, TABLE_TYPE AS tableType, "
                + "CAST(NULL AS NVARCHAR(4000)) AS tableComment "
                + "FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = SCHEMA_NAME() AND TABLE_TYPE = 'BASE TABLE' "
                + where + " ORDER BY TABLE_NAME OFFSET " + offset + " ROWS FETCH NEXT " + rows + " ROWS ONLY";
    }

    @Override
    public String getTableCount() {
        return "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = SCHEMA_NAME() AND TABLE_TYPE = 'BASE TABLE'";
    }

    @Override
    public String hasTable() {
        return "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = SCHEMA_NAME() "
                + "AND TABLE_NAME = #{" + RelationalSchemaSql.paramName + "}";
    }

    @Override
    public String getTableCharacterSetName() {
        return SchemaSqlNoops.ANSI_FALSE;
    }

    @Override
    public String convertTableCharset(Map<String, Object> map) {
        return SchemaSqlNoops.ANSI_FALSE;
    }

    @Override
    public String dropTable(Map<String, String> map) {
        String t = map.get(RelationalSchemaSql.paramName);
        if (t == null) {
            return SchemaSqlNoops.ANSI_FALSE;
        }
        return "DROP TABLE IF EXISTS dbo." + bracket(t);
    }

    private static String bracket(String s) {
        return "[" + s.replace("]", "]]") + "]";
    }

    @Override
    public String showKeys(Map<String, String> map) {
        return SchemaSqlNoops.ANSI_FALSE;
    }

    @Override
    public String showIndex(Map<String, String> map) {
        return SchemaSqlNoops.ANSI_FALSE;
    }

    @Override
    public String addIndex(Map<String, Map<TableInfo, IndexInfo>> map) {
        return SqlServerJdbcDdlGenerator.addIndex(map);
    }

    @Override
    public String addColumns(Map<String, Map<TableInfo, ColumnInfo>> map) {
        return SqlServerJdbcDdlGenerator.addColumns(map);
    }

    @Override
    public String modifyColumn(Map<String, Map<TableInfo, ColumnInfo>> map) {
        return SqlServerJdbcDdlGenerator.modifyColumn(map);
    }

    @Override
    public String dropColumn(Map<String, Map<TableInfo, String>> map) {
        return SqlServerJdbcDdlGenerator.dropColumn(map);
    }

    @Override
    public String dropPrimaryKey(Map<String, Map<TableInfo, ColumnInfo>> map) {
        return "";
    }

    @Override
    public String dropIndex(Map<String, Map<TableInfo, Object>> map) throws NoSuchFieldException, IllegalAccessException {
        return SqlServerJdbcDdlGenerator.dropIndex(map);
    }

    @Override
    public String createTable(Map<String, Map<TableInfo, List<ColumnInfo>>> map) {
        return SqlServerJdbcDdlGenerator.createTable(map);
    }
}
