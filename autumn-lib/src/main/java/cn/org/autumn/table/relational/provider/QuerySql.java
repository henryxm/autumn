package cn.org.autumn.table.relational.provider;

import cn.org.autumn.table.relational.RelationalSchemaSql;
import cn.org.autumn.table.relational.RelationalSchemaSqlRegistry;
import cn.org.autumn.table.data.ColumnInfo;
import cn.org.autumn.table.data.IndexInfo;
import cn.org.autumn.table.data.TableInfo;

import java.util.List;
import java.util.Map;

/**
 * MyBatis {@code @SelectProvider} 入口（原 {@code cn.org.autumn.table.mysql.QuerySql}）：委托 {@link RelationalSchemaSqlRegistry}，
 * 仅路由 MySQL 族与内嵌 H2-MySQL 兼容实现，避免包名 {@code mysql} 与「仅 MySQL」语义混淆。
 */
public class QuerySql {

    public static final String paramName = RelationalSchemaSql.paramName;
    public static final String createTable = "createTable";
    public static final String hasTable = "hasTable";
    public static final String getColumnMetas = "getColumnMetas";
    public static final String addColumns = "addColumns";
    public static final String modifyColumn = "modifyColumn";
    public static final String dropColumn = "dropColumn";
    public static final String dropPrimaryKey = "dropPrimaryKey";
    public static final String dropIndex = "dropIndex";
    public static final String dropTable = "dropTable";
    public static final String addIndex = "addIndex";
    public static final String getTableMetas = "getTableMetas";
    public static final String getTableCharacterSetName = "getTableCharacterSetName";
    public static final String convertTableCharset = "convertTableCharset";
    public static final String getTableCount = "getTableCount";
    public static final String showKeys = "showKeys";
    public static final String showIndex = "showIndex";
    public static final String getTableMetasWithMap = "getTableMetasWithMap";

    private static RelationalSchemaSql sql() {
        return RelationalSchemaSqlRegistry.get();
    }

    /**
     * @deprecated 使用 {@link EmbeddedH2MysqlMode#active()}。
     */
    @Deprecated
    public static boolean embeddedH2MysqlCompatDdl() {
        return EmbeddedH2MysqlMode.active();
    }

    public String getColumnMetas() {
        return sql().getColumnMetas();
    }

    public String getTableMetas(Map<String, Object> map) {
        return sql().getTableMetas(map);
    }

    public String getTableCount() {
        return sql().getTableCount();
    }

    public String hasTable() {
        return sql().hasTable();
    }

    public String getTableCharacterSetName() {
        return sql().getTableCharacterSetName();
    }

    public String convertTableCharset(Map<String, Object> map) {
        return sql().convertTableCharset(map);
    }

    public String dropTable(Map<String, String> map) {
        return sql().dropTable(map);
    }

    public String showKeys(Map<String, String> map) {
        return sql().showKeys(map);
    }

    public String showIndex(Map<String, String> map) {
        return sql().showIndex(map);
    }

    public String addIndex(Map<String, Map<TableInfo, IndexInfo>> map) {
        return sql().addIndex(map);
    }

    public String addColumns(Map<String, Map<TableInfo, ColumnInfo>> map) {
        return sql().addColumns(map);
    }

    public String modifyColumn(Map<String, Map<TableInfo, ColumnInfo>> map) {
        return sql().modifyColumn(map);
    }

    public String dropColumn(Map<String, Map<TableInfo, String>> map) {
        return sql().dropColumn(map);
    }

    public String dropPrimaryKey(Map<String, Map<TableInfo, ColumnInfo>> map) {
        return sql().dropPrimaryKey(map);
    }

    public String dropIndex(Map<String, Map<TableInfo, Object>> map) throws NoSuchFieldException, IllegalAccessException {
        return sql().dropIndex(map);
    }

    public String createTable(Map<String, Map<TableInfo, List<ColumnInfo>>> map) {
        return sql().createTable(map);
    }
}
