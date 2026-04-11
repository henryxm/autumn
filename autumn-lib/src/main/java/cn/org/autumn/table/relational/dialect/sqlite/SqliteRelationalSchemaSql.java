package cn.org.autumn.table.relational.dialect.sqlite;

import cn.org.autumn.table.relational.RelationalSchemaSql;
import cn.org.autumn.table.relational.support.AbstractNoopDdlRelationalSchemaSql;
import cn.org.autumn.table.relational.support.SchemaSqlNoops;
import cn.org.autumn.table.relational.support.ddl.AnsiDialect;
import cn.org.autumn.table.relational.support.ddl.AnsiDoubleQuotedDdlGenerator;
import cn.org.autumn.table.data.ColumnInfo;
import cn.org.autumn.table.data.IndexInfo;
import cn.org.autumn.table.data.TableInfo;
import org.apache.commons.lang.StringUtils;

import java.util.List;
import java.util.Map;

/**
 * SQLite：{@code sqlite_master} / {@code pragma_table_info}；DDL 由 {@link AnsiDoubleQuotedDdlGenerator} 生成。
 */
public final class SqliteRelationalSchemaSql extends AbstractNoopDdlRelationalSchemaSql {

    public static final SqliteRelationalSchemaSql INSTANCE = new SqliteRelationalSchemaSql();

    public SqliteRelationalSchemaSql() {
        super(SchemaSqlNoops.ANSI_FALSE);
    }

    @Override
    public String getColumnMetas() {
        return "SELECT CAST(NULL AS TEXT) AS tableCatalog, 'main' AS tableSchema, #{" + RelationalSchemaSql.paramName + "} AS tableName, "
                + "name AS columnName, CAST(cid + 1 AS TEXT) AS ordinalPosition, "
                + "CAST(dflt_value AS TEXT) AS columnDefault, "
                + "CASE WHEN \"notnull\" = 1 THEN 'NO' ELSE 'YES' END AS isNullable, "
                + "type AS dataType, CAST(NULL AS INTEGER) AS characterMaximumLength, "
                + "CAST(NULL AS INTEGER) AS characterOctetLength, CAST(NULL AS INTEGER) AS numericPrecision, "
                + "CAST(NULL AS INTEGER) AS numericScale, CAST(NULL AS INTEGER) AS datetimePrecision, "
                + "CAST(NULL AS TEXT) AS characterSetName, CAST(NULL AS TEXT) AS collationName, "
                + "type AS columnType, CASE WHEN pk = 1 THEN 'PRI' ELSE '' END AS columnKey, "
                + "CAST('' AS TEXT) AS columnComment, CAST('' AS TEXT) AS extra "
                + "FROM pragma_table_info(#{" + RelationalSchemaSql.paramName + "}) ORDER BY cid";
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
            where = " AND name = #{" + RelationalSchemaSql.paramName + "} ";
        }
        return "SELECT CAST(NULL AS TEXT) AS tableCatalog, 'main' AS tableSchema, name AS tableName, "
                + "'BASE TABLE' AS tableType, CAST('' AS TEXT) AS tableComment "
                + "FROM sqlite_master WHERE type = 'table' " + where
                + " ORDER BY name LIMIT " + rows + " OFFSET " + offset;
    }

    @Override
    public String getTableCount() {
        return "SELECT COUNT(*) FROM sqlite_master WHERE type = 'table'";
    }

    @Override
    public String hasTable() {
        return "SELECT COUNT(*) FROM sqlite_master WHERE type = 'table' AND name = #{" + RelationalSchemaSql.paramName + "}";
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
    public String dropTable(Map<String, String> map) {
        String t = map.get(RelationalSchemaSql.paramName);
        if (t == null) {
            return SchemaSqlNoops.ANSI_FALSE;
        }
        return "DROP TABLE IF EXISTS \"" + t.replace("\"", "\"\"") + "\"";
    }

    @Override
    public String createTable(Map<String, Map<TableInfo, List<ColumnInfo>>> map) {
        return AnsiDoubleQuotedDdlGenerator.createTable(AnsiDialect.SQLITE, map);
    }

    @Override
    public String addColumns(Map<String, Map<TableInfo, ColumnInfo>> map) {
        return AnsiDoubleQuotedDdlGenerator.addColumns(AnsiDialect.SQLITE, map);
    }

    @Override
    public String modifyColumn(Map<String, Map<TableInfo, ColumnInfo>> map) {
        return AnsiDoubleQuotedDdlGenerator.modifyColumn(AnsiDialect.SQLITE, map);
    }

    @Override
    public String dropColumn(Map<String, Map<TableInfo, String>> map) {
        return AnsiDoubleQuotedDdlGenerator.dropColumn(AnsiDialect.SQLITE, map);
    }

    @Override
    public String dropPrimaryKey(Map<String, Map<TableInfo, ColumnInfo>> map) {
        return AnsiDoubleQuotedDdlGenerator.dropPrimaryKey(AnsiDialect.SQLITE, map);
    }

    @Override
    public String dropIndex(Map<String, Map<TableInfo, Object>> map) throws NoSuchFieldException, IllegalAccessException {
        return AnsiDoubleQuotedDdlGenerator.dropIndex(AnsiDialect.SQLITE, map);
    }

    @Override
    public String addIndex(Map<String, Map<TableInfo, IndexInfo>> map) {
        return AnsiDoubleQuotedDdlGenerator.addIndex(AnsiDialect.SQLITE, map);
    }
}
