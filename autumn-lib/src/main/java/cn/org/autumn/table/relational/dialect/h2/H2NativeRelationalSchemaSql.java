package cn.org.autumn.table.relational.dialect.h2;

import cn.org.autumn.table.relational.RelationalSchemaSql;
import cn.org.autumn.table.relational.support.AbstractNoopDdlRelationalSchemaSql;
import cn.org.autumn.table.relational.support.SchemaSqlNoops;
import cn.org.autumn.table.relational.support.ddl.AnsiDialect;
import cn.org.autumn.table.relational.support.ddl.AnsiDoubleQuotedDdlGenerator;
import cn.org.autumn.table.data.ColumnInfo;
import cn.org.autumn.table.data.IndexInfo;
import cn.org.autumn.table.data.TableInfo;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;

/**
 * H2 原生模式（非 MySQL 兼容）：{@code INFORMATION_SCHEMA} + {@code SCHEMA()}。
 */
public final class H2NativeRelationalSchemaSql extends AbstractNoopDdlRelationalSchemaSql {

    public static final H2NativeRelationalSchemaSql INSTANCE = new H2NativeRelationalSchemaSql();

    public H2NativeRelationalSchemaSql() {
        super(SchemaSqlNoops.ANSI_FALSE);
    }

    @Override
    public String getColumnMetas() {
        return "SELECT * FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = #{" + RelationalSchemaSql.paramName + "} "
                + "AND TABLE_SCHEMA = SCHEMA() ORDER BY ORDINAL_POSITION";
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
        return "SELECT TABLE_CATALOG AS tableCatalog, TABLE_SCHEMA AS tableSchema, TABLE_NAME AS tableName, "
                + "TABLE_TYPE AS tableType, CAST(NULL AS VARCHAR) AS tableComment "
                + "FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = SCHEMA() AND TABLE_TYPE = 'TABLE' "
                + where + " ORDER BY TABLE_NAME LIMIT " + rows + " OFFSET " + offset;
    }

    @Override
    public String getTableCount() {
        return "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = SCHEMA() AND TABLE_TYPE = 'TABLE'";
    }

    @Override
    public String hasTable() {
        return "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = SCHEMA() "
                + "AND TABLE_NAME = #{" + RelationalSchemaSql.paramName + "}";
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
        return AnsiDoubleQuotedDdlGenerator.createTable(AnsiDialect.H2, map);
    }

    @Override
    public String addColumns(Map<String, Map<TableInfo, ColumnInfo>> map) {
        return AnsiDoubleQuotedDdlGenerator.addColumns(AnsiDialect.H2, map);
    }

    @Override
    public String modifyColumn(Map<String, Map<TableInfo, ColumnInfo>> map) {
        return AnsiDoubleQuotedDdlGenerator.modifyColumn(AnsiDialect.H2, map);
    }

    @Override
    public String dropColumn(Map<String, Map<TableInfo, String>> map) {
        return AnsiDoubleQuotedDdlGenerator.dropColumn(AnsiDialect.H2, map);
    }

    @Override
    public String dropPrimaryKey(Map<String, Map<TableInfo, ColumnInfo>> map) {
        return AnsiDoubleQuotedDdlGenerator.dropPrimaryKey(AnsiDialect.H2, map);
    }

    @Override
    public String dropIndex(Map<String, Map<TableInfo, Object>> map) throws NoSuchFieldException, IllegalAccessException {
        return AnsiDoubleQuotedDdlGenerator.dropIndex(AnsiDialect.H2, map);
    }

    @Override
    public String addIndex(Map<String, Map<TableInfo, IndexInfo>> map) {
        return AnsiDoubleQuotedDdlGenerator.addIndex(AnsiDialect.H2, map);
    }
}
