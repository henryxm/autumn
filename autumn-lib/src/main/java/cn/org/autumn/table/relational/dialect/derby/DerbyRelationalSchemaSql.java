package cn.org.autumn.table.relational.dialect.derby;

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
 * Apache Derby：{@code INFORMATION_SCHEMA} + {@code CURRENT SCHEMA}；DDL 由 {@link AnsiDoubleQuotedDdlGenerator} 生成。
 * Boot 2.7 默认 10.14 无 {@code IF NOT EXISTS} DDL，生成器对 {@link AnsiDialect#DERBY} 已按该版本输出。
 */
public final class DerbyRelationalSchemaSql extends AbstractNoopDdlRelationalSchemaSql {

    public static final DerbyRelationalSchemaSql INSTANCE = new DerbyRelationalSchemaSql();

    /**
     * Derby 用户表在 {@code INFORMATION_SCHEMA.TABLES} 中多为 {@code BASE TABLE}，与 {@code TABLE} 一并识别，避免漏列。
     */
    private static final String USER_TABLE_FILTER =
            "(TABLE_TYPE = 'TABLE' OR TABLE_TYPE = 'BASE TABLE')";

    public DerbyRelationalSchemaSql() {
        super(SchemaSqlNoops.ANSI_FALSE);
    }

    @Override
    public String getColumnMetas() {
        return "SELECT * FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = #{"
                + RelationalSchemaSql.paramName + "} "
                + "AND TABLE_SCHEMA = CURRENT SCHEMA ORDER BY ORDINAL_POSITION";
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
        String paging = derbyPaging(offset, rows);
        return "SELECT TABLE_CATALOG AS tableCatalog, TABLE_SCHEMA AS tableSchema, TABLE_NAME AS tableName, "
                + "TABLE_TYPE AS tableType, CAST(NULL AS VARCHAR(32672)) AS tableComment "
                + "FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = CURRENT SCHEMA AND " + USER_TABLE_FILTER + " "
                + where + " ORDER BY TABLE_NAME " + paging;
    }

    /**
     * Derby 使用 {@code FETCH FIRST … ROWS ONLY} / {@code OFFSET … ROWS FETCH NEXT …}，与 MySQL {@code LIMIT} 隔离，避免误伤其它方言。
     */
    private static String derbyPaging(int offset, int rows) {
        int cap = (rows == Integer.MAX_VALUE) ? 1_000_000 : Math.min(Math.max(rows, 1), 1_000_000);
        if (offset <= 0) {
            return "FETCH FIRST " + cap + " ROWS ONLY";
        }
        return "OFFSET " + offset + " ROWS FETCH NEXT " + cap + " ROWS ONLY";
    }

    @Override
    public String getTableCount() {
        return "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = CURRENT SCHEMA AND "
                + USER_TABLE_FILTER;
    }

    @Override
    public String hasTable() {
        return "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = CURRENT SCHEMA "
                + "AND TABLE_NAME = #{" + RelationalSchemaSql.paramName + "} AND " + USER_TABLE_FILTER;
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
        return AnsiDoubleQuotedDdlGenerator.createTable(AnsiDialect.DERBY, map);
    }

    @Override
    public String addColumns(Map<String, Map<TableInfo, ColumnInfo>> map) {
        return AnsiDoubleQuotedDdlGenerator.addColumns(AnsiDialect.DERBY, map);
    }

    @Override
    public String modifyColumn(Map<String, Map<TableInfo, ColumnInfo>> map) {
        return AnsiDoubleQuotedDdlGenerator.modifyColumn(AnsiDialect.DERBY, map);
    }

    @Override
    public String dropColumn(Map<String, Map<TableInfo, String>> map) {
        return AnsiDoubleQuotedDdlGenerator.dropColumn(AnsiDialect.DERBY, map);
    }

    @Override
    public String dropPrimaryKey(Map<String, Map<TableInfo, ColumnInfo>> map) {
        return AnsiDoubleQuotedDdlGenerator.dropPrimaryKey(AnsiDialect.DERBY, map);
    }

    @Override
    public String dropIndex(Map<String, Map<TableInfo, Object>> map) throws NoSuchFieldException, IllegalAccessException {
        return AnsiDoubleQuotedDdlGenerator.dropIndex(AnsiDialect.DERBY, map);
    }

    @Override
    public String addIndex(Map<String, Map<TableInfo, IndexInfo>> map) {
        return AnsiDoubleQuotedDdlGenerator.addIndex(AnsiDialect.DERBY, map);
    }
}
