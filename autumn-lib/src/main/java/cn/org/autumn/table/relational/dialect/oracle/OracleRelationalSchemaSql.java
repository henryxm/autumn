package cn.org.autumn.table.relational.dialect.oracle;

import cn.org.autumn.table.relational.RelationalSchemaSql;
import cn.org.autumn.table.relational.support.SchemaSqlNoops;
import cn.org.autumn.table.relational.support.ddl.OracleJdbcDdlGenerator;
import cn.org.autumn.table.data.ColumnInfo;
import cn.org.autumn.table.data.IndexInfo;
import cn.org.autumn.table.data.TableInfo;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

/**
 * Oracle：USER_* 元数据；注解建表 DDL 由 {@link OracleJdbcDdlGenerator} 生成（12c+ 标识列）。
 */
public final class OracleRelationalSchemaSql implements RelationalSchemaSql {

    public static final OracleRelationalSchemaSql INSTANCE = new OracleRelationalSchemaSql();

    private static String qi(String name) {
        if (name == null) {
            return "\"\"";
        }
        return "\"" + name.replace("\"", "\"\"") + "\"";
    }

    @Override
    public String getColumnMetas() {
        return "SELECT CAST(NULL AS VARCHAR2(1)) AS tableCatalog, c.owner AS tableSchema, c.table_name AS tableName, "
                + "c.column_name AS columnName, TO_CHAR(c.column_id) AS ordinalPosition, "
                + "CAST(c.data_default AS VARCHAR2(4000)) AS columnDefault, c.nullable AS isNullable, c.data_type AS dataType, "
                + "c.data_length AS characterMaximumLength, c.data_length AS characterOctetLength, "
                + "c.data_precision AS numericPrecision, c.data_scale AS numericScale, "
                + "CAST(NULL AS NUMBER) AS datetimePrecision, CAST(NULL AS VARCHAR2(1)) AS characterSetName, "
                + "CAST(NULL AS VARCHAR2(1)) AS collationName, "
                + "(c.data_type || CASE WHEN c.data_length IS NOT NULL AND c.data_type LIKE '%CHAR%' "
                + "THEN '(' || c.data_length || ')' ELSE '' END) AS columnType, "
                + "CASE WHEN c.column_name IN (SELECT cc.column_name FROM user_cons_columns cc "
                + "JOIN user_constraints co ON cc.constraint_name = co.constraint_name AND cc.owner = co.owner "
                + "WHERE co.constraint_type = 'P' AND co.table_name = c.table_name) THEN 'PRI' ELSE '' END AS columnKey, "
                + "CAST(NULL AS VARCHAR2(1)) AS columnComment, CAST(NULL AS VARCHAR2(1)) AS extra "
                + "FROM user_tab_columns c WHERE c.table_name = UPPER(#{" + RelationalSchemaSql.paramName + "}) "
                + "ORDER BY c.column_id";
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
            where = " AND table_name = UPPER(#{" + RelationalSchemaSql.paramName + "}) ";
        }
        return "SELECT CAST(NULL AS VARCHAR2(1)) AS tableCatalog, owner AS tableSchema, table_name AS tableName, "
                + "'BASE TABLE' AS tableType, CAST(NULL AS VARCHAR2(1)) AS tableComment "
                + "FROM user_tables WHERE 1=1 " + where
                + " ORDER BY table_name OFFSET " + offset + " ROWS FETCH NEXT " + rows + " ROWS ONLY";
    }

    @Override
    public String getTableCount() {
        return "SELECT COUNT(*) FROM user_tables";
    }

    @Override
    public String hasTable() {
        return "SELECT COUNT(*) FROM user_tables WHERE table_name = UPPER(#{" + RelationalSchemaSql.paramName + "})";
    }

    @Override
    public String getTableCharacterSetName() {
        return SchemaSqlNoops.ORACLE_FAMILY_FALSE;
    }

    @Override
    public String convertTableCharset(Map<String, Object> map) {
        return SchemaSqlNoops.ORACLE_FAMILY_FALSE;
    }

    @Override
    public String dropTable(Map<String, String> map) {
        return "DROP TABLE " + qi(map.get(RelationalSchemaSql.paramName)) + " PURGE";
    }

    @Override
    public String showKeys(Map<String, String> map) {
        return SchemaSqlNoops.ORACLE_FAMILY_FALSE;
    }

    @Override
    public String showIndex(Map<String, String> map) {
        return SchemaSqlNoops.ORACLE_FAMILY_FALSE;
    }

    @Override
    public String addIndex(Map<String, Map<TableInfo, IndexInfo>> map) {
        return OracleJdbcDdlGenerator.addIndex(map);
    }

    @Override
    public String addColumns(Map<String, Map<TableInfo, ColumnInfo>> map) {
        return OracleJdbcDdlGenerator.addColumns(map);
    }

    @Override
    public String modifyColumn(Map<String, Map<TableInfo, ColumnInfo>> map) {
        return OracleJdbcDdlGenerator.modifyColumn(map);
    }

    @Override
    public String dropColumn(Map<String, Map<TableInfo, String>> map) {
        return OracleJdbcDdlGenerator.dropColumn(map);
    }

    @Override
    public String dropPrimaryKey(Map<String, Map<TableInfo, ColumnInfo>> map) {
        return OracleJdbcDdlGenerator.dropPrimaryKey(map);
    }

    @Override
    public String dropIndex(Map<String, Map<TableInfo, Object>> map) throws NoSuchFieldException, IllegalAccessException {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<TableInfo, Object> kv : map.get(RelationalSchemaSql.paramName).entrySet()) {
            Field f = kv.getValue().getClass().getDeclaredField("name");
            f.setAccessible(true);
            sb.append("DROP INDEX ").append(qi((String) f.get(kv.getValue()))).append(";");
        }
        return sb.toString();
    }

    @Override
    public String createTable(Map<String, Map<TableInfo, List<ColumnInfo>>> map) {
        return OracleJdbcDdlGenerator.createTable(map);
    }
}
