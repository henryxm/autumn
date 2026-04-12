package cn.org.autumn.table.relational.dialect.postgresql;

import cn.org.autumn.table.annotation.IndexTypeEnum;
import cn.org.autumn.table.relational.RelationalSchemaSql;
import cn.org.autumn.table.data.ColumnInfo;
import cn.org.autumn.table.data.IndexInfo;
import cn.org.autumn.table.data.TableInfo;
import cn.org.autumn.table.data.UniqueKeyInfo;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * PostgreSQL 方言：供 {@link PostgresTableDao} 的 {@code @SelectProvider} 使用。
 * 实现 {@link RelationalSchemaSql}，与 MySQL 族、H2-MySQL 兼容实现平级。
 */
public class PostgresRelationalSchemaSql implements RelationalSchemaSql {

    public static final PostgresRelationalSchemaSql INSTANCE = new PostgresRelationalSchemaSql();

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

    private static String qi(String name) {
        if (name == null) {
            return "\"\"";
        }
        return "\"" + name.replace("\"", "\"\"") + "\"";
    }

    private static String pgDataType(ColumnInfo columnInfo) {
        String t = columnInfo.getType().toLowerCase();
        int len = columnInfo.getLength();
        int dec = columnInfo.getDecimalLength();
        switch (t) {
            case "varchar":
                return "varchar(" + Math.max(len, 1) + ")";
            case "char":
                return "char(" + Math.max(len, 1) + ")";
            case "int":
                return "integer";
            case "integer":
                return "integer";
            case "tinyint":
                // Java boolean 在 ColumnInfo 中固定为 tinyint + length=1；PG 用 boolean 与 JDBC boolean 参数一致
                if (len == 1) {
                    return "boolean";
                }
                return "smallint";
            case "smallint":
                return "smallint";
            case "bigint":
                return "bigint";
            case "text":
            case "tinytext":
            case "mediumtext":
            case "longtext":
                return "text";
            case "datetime":
            case "timestamp":
                return "timestamp(0) without time zone";
            case "date":
                return "date";
            case "time":
                return "time";
            case "decimal":
                return "numeric(" + Math.max(len, 1) + "," + dec + ")";
            case "double":
                return "double precision";
            case "float":
                return "real";
            case "blob":
            case "tinyblob":
            case "mediumblob":
            case "longblob":
                return "bytea";
            case "year":
                return "smallint";
            case "enum":
            case "set":
                return "varchar(255)";
            default:
                return t;
        }
    }

    private static void appendColumnBody(ColumnInfo columnInfo, StringBuilder sb, boolean forPkAutoSerial) {
        if (forPkAutoSerial) {
            if ("bigint".equalsIgnoreCase(columnInfo.getType())) {
                sb.append("BIGSERIAL");
            } else {
                sb.append("SERIAL");
            }
            return;
        }
        sb.append(pgDataType(columnInfo));
        if (!columnInfo.isNull()) {
            sb.append(" NOT NULL");
        }
        if (!"NULL".equals(columnInfo.getDefaultValue())) {
            String dv = columnInfo.getDefaultValue();
            if (dv != null && !dv.isEmpty()) {
                if ("NULL".equalsIgnoreCase(dv)) {
                    sb.append(" DEFAULT NULL");
                } else if (isTinyintBooleanFlag(columnInfo) && dv.matches("[01]")) {
                    sb.append(" DEFAULT ").append("0".equals(dv) ? "false" : "true");
                } else if (dv.matches("-?\\d+") && isNumericType(columnInfo.getType())) {
                    sb.append(" DEFAULT ").append(dv);
                } else if ("true".equalsIgnoreCase(dv) || "false".equalsIgnoreCase(dv)) {
                    sb.append(" DEFAULT ").append(dv.toLowerCase());
                } else {
                    sb.append(" DEFAULT '").append(dv.replace("'", "''")).append("'");
                }
            }
        }
    }

    /** 与 {@link cn.org.autumn.table.data.ColumnInfo#initFrom} 中 boolean → tinyint、length=1 一致 */
    private static boolean isTinyintBooleanFlag(ColumnInfo columnInfo) {
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

    public String getColumnMetas() {
        return "SELECT "
                + "c.column_name AS columnName, "
                + "c.table_schema AS tableSchema, "
                + "c.table_name AS tableName, "
                + "CAST(c.ordinal_position AS VARCHAR) AS ordinalPosition, "
                + "c.column_default AS columnDefault, "
                + "c.is_nullable AS isNullable, "
                + "c.data_type AS dataType, "
                + "c.character_maximum_length AS characterMaximumLength, "
                + "c.character_octet_length AS characterOctetLength, "
                + "c.numeric_precision AS numericPrecision, "
                + "c.numeric_scale AS numericScale, "
                + "c.datetime_precision AS datetimePrecision, "
                + "NULL AS characterSetName, "
                + "NULL AS collationName, "
                + "pg_catalog.format_type(a.atttypid, a.atttypmod) AS columnType, "
                + "CASE WHEN pk.col IS NOT NULL THEN 'PRI' ELSE '' END AS columnKey, "
                + "d.description AS columnComment, "
                + "CASE WHEN c.is_identity = 'YES' THEN 'auto_increment' "
                + "WHEN c.column_default IS NOT NULL AND c.column_default LIKE 'nextval%' THEN 'auto_increment' "
                + "ELSE COALESCE(pg_catalog.pg_get_expr(ad.adbin, ad.adrelid), '') END AS extra "
                + "FROM information_schema.columns c "
                + "JOIN pg_catalog.pg_namespace ns ON ns.nspname = c.table_schema "
                + "JOIN pg_catalog.pg_class cls ON cls.relnamespace = ns.oid AND cls.relname = c.table_name AND cls.relkind = 'r' "
                + "JOIN pg_catalog.pg_attribute a ON a.attrelid = cls.oid AND a.attname = c.column_name AND a.attnum > 0 AND NOT a.attisdropped "
                + "LEFT JOIN pg_catalog.pg_attrdef ad ON ad.adrelid = cls.oid AND ad.adnum = a.attnum "
                + "LEFT JOIN pg_catalog.pg_description d ON d.objoid = cls.oid AND d.objsubid = a.attnum "
                + "LEFT JOIN ( "
                + "  SELECT kcu.column_name AS col "
                + "  FROM information_schema.table_constraints tc "
                + "  JOIN information_schema.key_column_usage kcu ON tc.constraint_schema = kcu.constraint_schema "
                + "    AND tc.constraint_name = kcu.constraint_name "
                + "  WHERE tc.table_schema = current_schema() AND tc.table_name = #{" + paramName + "} "
                + "    AND tc.constraint_type = 'PRIMARY KEY' "
                + ") pk ON pk.col = c.column_name "
                + "WHERE c.table_schema = current_schema() AND c.table_name = #{" + paramName + "} "
                + "ORDER BY c.ordinal_position";
    }

    public String hasTable() {
        return "SELECT COUNT(1) FROM information_schema.tables "
                + "WHERE table_schema = current_schema() AND table_name = #{" + paramName + "}";
    }

    public String getTableCharacterSetName() {
        return "SELECT CAST(NULL AS VARCHAR) AS charset WHERE FALSE";
    }

    public String convertTableCharset(Map<String, Object> map) {
        return "SELECT 1 WHERE FALSE";
    }

    public String getTableMetas(Map<String, Object> map) {
        String tableName = (String) map.get(RelationalSchemaSql.paramName);
        int offset = 0;
        if (map.containsKey("offset")) {
            offset = (int) map.get("offset");
        }
        if (offset < 0) {
            offset = 0;
        }
        int rows = Integer.MAX_VALUE;
        if (map.containsKey("rows")) {
            rows = (int) map.get("rows");
        }
        if (rows <= 0) {
            rows = Integer.MAX_VALUE;
        }
        String where = "";
        if (StringUtils.isNotBlank(tableName)) {
            where = " AND table_name = #{" + paramName + "} ";
        }
        return "SELECT table_name AS tableName, table_schema AS tableSchema, "
                + "obj_description((quote_ident(table_schema) || '.' || quote_ident(table_name))::regclass, 'pg_class') AS tableComment "
                + "FROM information_schema.tables "
                + "WHERE table_schema = current_schema() AND table_type = 'BASE TABLE' "
                + where
                + "ORDER BY table_name LIMIT " + rows + " OFFSET " + offset;
    }

    public String getTableCount() {
        return "SELECT COUNT(*) FROM information_schema.tables "
                + "WHERE table_schema = current_schema() AND table_type = 'BASE TABLE'";
    }

    public String showKeys(Map<String, String> map) {
        return "SELECT CAST(NULL AS VARCHAR) AS keyName, CAST(NULL AS VARCHAR) AS columnName, CAST(NULL AS VARCHAR) AS nonUnique WHERE FALSE";
    }

    public String showIndex(Map<String, String> map) {
        return "SELECT i.relname AS keyName, "
                + "a.attname AS columnName, "
                + "CASE WHEN ix.indisunique THEN '0' ELSE '1' END AS nonUnique, "
                + "CAST(u.ordinality AS VARCHAR) AS seqInIndex, "
                + "'BTREE' AS indexType, "
                + "'BTREE' AS indexMethod "
                + "FROM pg_catalog.pg_class t "
                + "JOIN pg_catalog.pg_namespace ns ON ns.oid = t.relnamespace AND ns.nspname = current_schema() "
                + "JOIN pg_catalog.pg_index ix ON t.oid = ix.indrelid "
                + "JOIN pg_catalog.pg_class i ON i.oid = ix.indexrelid "
                + "JOIN LATERAL unnest(ix.indkey) WITH ORDINALITY AS u(attnum, ordinality) ON TRUE "
                + "JOIN pg_catalog.pg_attribute a ON a.attrelid = t.oid AND a.attnum = u.attnum AND NOT a.attisdropped "
                + "WHERE t.relname = #{" + paramName + "} AND t.relkind = 'r' AND NOT ix.indisprimary "
                + "ORDER BY i.relname, u.ordinality";
    }

    public String dropTable(Map<String, String> map) {
        String tableName = map.get(RelationalSchemaSql.paramName);
        return "DROP TABLE IF EXISTS " + qi(tableName) + " CASCADE";
    }

    public String createTable(final Map<String, Map<TableInfo, List<ColumnInfo>>> map) {
        StringBuilder all = new StringBuilder();
        Map<TableInfo, List<ColumnInfo>> parameter = map.get(RelationalSchemaSql.paramName);
        for (Entry<TableInfo, List<ColumnInfo>> kv : parameter.entrySet()) {
            TableInfo tableInfo = kv.getKey();
            List<ColumnInfo> list = kv.getValue();
            String tname = tableInfo.getName();
            StringBuilder sb = new StringBuilder();
            sb.append("CREATE TABLE IF NOT EXISTS ").append(qi(tname)).append(" (");
            StringBuilder pkCols = new StringBuilder();
            for (int i = 0; i < list.size(); i++) {
                ColumnInfo columnInfo = list.get(i);
                boolean serialPk = columnInfo.isKey() && columnInfo.isAutoIncrement();
                sb.append(qi(columnInfo.getName())).append(" ");
                appendColumnBody(columnInfo, sb, serialPk);
                if (serialPk) {
                    sb.append(" PRIMARY KEY");
                } else if (columnInfo.isKey()) {
                    String sp = pkCols.length() == 0 ? "" : ",";
                    pkCols.append(sp).append(qi(columnInfo.getName()));
                }
                if (columnInfo.isUnique() && !columnInfo.isKey()) {
                    sb.append(" UNIQUE");
                }
                if (i < list.size() - 1) {
                    sb.append(", ");
                }
            }
            if (pkCols.length() > 0) {
                sb.append(", PRIMARY KEY (").append(pkCols).append(")");
            }
            if (tableInfo.getUniqueKeyInfos() != null && !tableInfo.getUniqueKeyInfos().isEmpty()) {
                for (UniqueKeyInfo uk : tableInfo.getUniqueKeyInfos()) {
                    if (StringUtils.isBlank(uk.getName())) {
                        continue;
                    }
                    sb.append(", CONSTRAINT ").append(qi(uk.getName())).append(" UNIQUE (");
                    appendIndexFields(sb, uk.getFields());
                    sb.append(")");
                }
            }
            sb.append(")");
            if (StringUtils.isNotBlank(tableInfo.getComment())) {
                sb.append("; COMMENT ON TABLE ").append(qi(tname)).append(" IS '")
                        .append(tableInfo.getComment().replace("'", "''")).append("'");
            }
            for (ColumnInfo c : list) {
                if (StringUtils.isNotBlank(c.getComment())) {
                    sb.append("; COMMENT ON COLUMN ").append(qi(tname)).append(".").append(qi(c.getName()))
                            .append(" IS '").append(c.getComment().replace("'", "''")).append("'");
                }
            }
            if (tableInfo.getIndexInfos() != null) {
                for (IndexInfo idx : tableInfo.getIndexInfos()) {
                    idx.resolve();
                    if (IndexTypeEnum.FULLTEXT.name().equals(idx.getIndexType())) {
                        continue;
                    }
                    if (StringUtils.isBlank(idx.getName())) {
                        continue;
                    }
                    sb.append("; CREATE ");
                    if (IndexTypeEnum.UNIQUE.name().equals(idx.getIndexType())) {
                        sb.append("UNIQUE ");
                    }
                    sb.append("INDEX IF NOT EXISTS ").append(qi(idx.getName()))
                            .append(" ON ").append(qi(tname)).append(" USING btree (");
                    appendIndexFields(sb, idx.getFields());
                    sb.append(")");
                }
            }
            all.append(sb).append(";");
        }
        return all.toString();
    }

    /**
     * 仅输出列名。MySQL 风格索引前缀 {@code "col"(n)} 在 PostgreSQL 中会被解析为函数调用（如 {@code user_uuid(50)}），
     * 导致 {@code function user_uuid(integer) does not exist}；前缀长度仅适用于 MySQL 路径，见 {@link cn.org.autumn.table.data.IndexPrefixRules}。
     */
    private void appendIndexFields(StringBuilder sb, Map<String, Integer> fields) {
        if (fields == null) {
            return;
        }
        Iterator<Entry<String, Integer>> it = fields.entrySet().iterator();
        while (it.hasNext()) {
            Entry<String, Integer> e = it.next();
            sb.append(qi(e.getKey()));
            if (it.hasNext()) {
                sb.append(", ");
            }
        }
    }

    private void appendAlterColumn(ColumnInfo columnInfo, StringBuilder sb) {
        sb.append(pgDataType(columnInfo));
        if (!columnInfo.isNull()) {
            sb.append(" NOT NULL");
        } else {
            sb.append(" NULL");
        }
    }

    public String addColumns(final Map<String, Map<TableInfo, ColumnInfo>> map) {
        return alterColumns(map, "ADD COLUMN");
    }

    public String modifyColumn(final Map<String, Map<TableInfo, ColumnInfo>> map) {
        return alterColumns(map, "ALTER COLUMN");
    }

    private String alterColumns(final Map<String, Map<TableInfo, ColumnInfo>> map, String action) {
        StringBuilder sb = new StringBuilder();
        Map<TableInfo, ColumnInfo> parameter = map.get(RelationalSchemaSql.paramName);
        for (Entry<TableInfo, ColumnInfo> kv : parameter.entrySet()) {
            TableInfo tableInfo = kv.getKey();
            ColumnInfo c = kv.getValue();
            sb.append("ALTER TABLE ").append(qi(tableInfo.getName()));
            if ("ADD COLUMN".equals(action)) {
                sb.append(" ADD COLUMN ").append(qi(c.getName())).append(" ");
                appendAlterColumn(c, sb);
            } else {
                sb.append(" ALTER COLUMN ").append(qi(c.getName())).append(" TYPE ")
                        .append(pgDataType(c)).append(", ALTER COLUMN ").append(qi(c.getName()));
                if (!c.isNull()) {
                    sb.append(" SET NOT NULL");
                } else {
                    sb.append(" DROP NOT NULL");
                }
            }
            sb.append(";");
        }
        return sb.toString();
    }

    public String dropColumn(final Map<String, Map<TableInfo, String>> map) {
        StringBuilder sb = new StringBuilder();
        Map<TableInfo, String> parameter = map.get(RelationalSchemaSql.paramName);
        for (Entry<TableInfo, String> kv : parameter.entrySet()) {
            sb.append("ALTER TABLE ").append(qi(kv.getKey().getName()))
                    .append(" DROP COLUMN IF EXISTS ").append(qi(kv.getValue())).append(";");
        }
        return sb.toString();
    }

    public String dropPrimaryKey(final Map<String, Map<TableInfo, ColumnInfo>> map) {
        return "SELECT 1 WHERE FALSE";
    }

    public String dropIndex(final Map<String, Map<TableInfo, Object>> map) throws NoSuchFieldException, IllegalAccessException {
        StringBuilder sb = new StringBuilder();
        Map<TableInfo, Object> parameter = map.get(RelationalSchemaSql.paramName);
        for (Entry<TableInfo, Object> kv : parameter.entrySet()) {
            Object indexInfo = kv.getValue();
            Field field = indexInfo.getClass().getDeclaredField("name");
            field.setAccessible(true);
            String name = (String) field.get(indexInfo);
            sb.append("DROP INDEX IF EXISTS ").append(qi(name)).append(";");
        }
        return sb.toString();
    }

    public String addIndex(final Map<String, Map<TableInfo, IndexInfo>> map) {
        StringBuilder sb = new StringBuilder();
        Map<TableInfo, IndexInfo> parameter = map.get(RelationalSchemaSql.paramName);
        for (Entry<TableInfo, IndexInfo> ii : parameter.entrySet()) {
            TableInfo table = ii.getKey();
            IndexInfo indexInfo = ii.getValue();
            indexInfo.resolve();
            if (IndexTypeEnum.FULLTEXT.name().equals(indexInfo.getIndexType())) {
                continue;
            }
            if (StringUtils.isBlank(indexInfo.getName())) {
                continue;
            }
            sb.append("CREATE ");
            if (IndexTypeEnum.UNIQUE.name().equals(indexInfo.getIndexType())) {
                sb.append("UNIQUE ");
            }
            sb.append("INDEX IF NOT EXISTS ").append(qi(indexInfo.getName()))
                    .append(" ON ").append(qi(table.getName())).append(" USING btree (");
            appendIndexFields(sb, indexInfo.getFields());
            sb.append(");");
        }
        return sb.toString();
    }
}
