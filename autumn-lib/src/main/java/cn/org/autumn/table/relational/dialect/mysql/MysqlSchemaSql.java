package cn.org.autumn.table.relational.dialect.mysql;

import cn.org.autumn.table.annotation.IndexTypeEnum;
import cn.org.autumn.table.relational.RelationalSchemaSql;
import cn.org.autumn.table.data.ColumnInfo;
import cn.org.autumn.table.data.IndexInfo;
import cn.org.autumn.table.data.TableInfo;
import cn.org.autumn.table.utils.HumpConvert;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * MySQL / MariaDB / TiDB / OceanBase MySQL 等主路径：不含内嵌 H2 兼容分支。
 */
public class MysqlSchemaSql implements RelationalSchemaSql {

    public static final MysqlSchemaSql INSTANCE = new MysqlSchemaSql();

    protected MysqlSchemaSql() {
    }

    @Override
    public String getColumnMetas() {
        return "select * from information_schema.columns where table_name = #{" + RelationalSchemaSql.paramName + "} and table_schema = (select database()) order by ordinal_position";
    }

    @Override
    public String getTableMetas(Map<String, Object> map) {
        String tableName = (String) map.get(RelationalSchemaSql.paramName);
        int offset = 0;
        if (map.containsKey("offset")) {
            offset = (int) map.get("offset");
        }
        if (offset < 0) {
            offset = 0;
        }

        int rows = 0;
        if (map.containsKey("rows")) {
            rows = (int) map.get("rows");
        }
        if (rows <= 0) {
            rows = Integer.MAX_VALUE;
        }
        String whereClause = "";
        if (!StringUtils.isEmpty(tableName)) {
            whereClause = " table_name = #{" + RelationalSchemaSql.paramName + "} and";
        }
        return "select * from information_schema.tables where" + whereClause + " table_schema = (select database()) order by create_time desc limit " + offset + ", " + rows;
    }

    @Override
    public String getTableCount() {
        return "select count( * ) from information_schema.tables where table_schema = (select database())";
    }

    @Override
    public String hasTable() {
        return "select count(1) from information_schema.tables"
                + " where table_name = #{" + RelationalSchemaSql.paramName + "} and table_schema = (select database())";
    }

    @Override
    public String getTableCharacterSetName() {
        return "SELECT C.CHARACTER_SET_NAME FROM information_schema.TABLES T "
                + "INNER JOIN information_schema.COLLATION_CHARACTER_SET_APPLICABILITY C "
                + "ON T.TABLE_COLLATION = C.COLLATION_NAME "
                + "WHERE T.TABLE_SCHEMA = (SELECT DATABASE()) AND T.TABLE_NAME = #{" + RelationalSchemaSql.paramName + "}";
    }

    @Override
    public String convertTableCharset(Map<String, Object> map) {
        String tableName = (String) map.get(RelationalSchemaSql.paramName);
        String charset = (String) map.get("charset");
        String collation = (String) map.get("collation");
        if (tableName == null || charset == null || charset.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("ALTER TABLE `").append(tableName).append("` CONVERT TO CHARACTER SET ");
        sb.append(charset);
        if (StringUtils.isNotBlank(collation)) {
            sb.append(" COLLATE ").append(collation);
        }
        sb.append(";");
        return sb.toString();
    }

    @Override
    public String dropTable(Map<String, String> map) {
        String tableName = map.get(RelationalSchemaSql.paramName);
        return "DROP TABLE IF EXISTS " + tableName;
    }

    @Override
    public String showKeys(Map<String, String> map) {
        String tableName = map.get(RelationalSchemaSql.paramName);
        return "SHOW KEYS FROM " + tableName;
    }

    @Override
    public String showIndex(Map<String, String> map) {
        String tableName = map.get(RelationalSchemaSql.paramName);
        return "SHOW INDEX FROM " + tableName;
    }

    protected static String stripIndexUsingClauses(String fragment) {
        if (StringUtils.isBlank(fragment)) {
            return fragment;
        }
        return fragment.replaceAll(" USING [A-Za-z0-9_]+", "");
    }

    protected static String stripMysqlIndexPrefixLengths(String fragment) {
        if (StringUtils.isBlank(fragment)) {
            return fragment;
        }
        return fragment.replaceAll("`([^`]+)`\\(\\d+\\)", "`$1`");
    }

    protected static String h2IndexTablePrefix(String tableName) {
        if (StringUtils.isBlank(tableName)) {
            return "t";
        }
        return tableName.replaceAll("[^a-zA-Z0-9_]", "_");
    }

    protected static String flattenH2SecondaryIndexNames(String fragment, String tableName) {
        if (StringUtils.isBlank(fragment)) {
            return fragment;
        }
        String tab = h2IndexTablePrefix(tableName);
        String s = fragment.replaceAll("INDEX `([^`]+)` \\(", "INDEX `ix_" + tab + "_$1` (");
        return s.replaceAll("UNIQUE KEY `([^`]+)` \\(", "UNIQUE KEY `uk_" + tab + "_$1` (");
    }

    protected void appendColumnCharsetCollation(ColumnInfo columnInfo, StringBuilder sb) {
        if (!columnInfo.supportsSqlCharsetClause()) {
            return;
        }
        String cs = columnInfo.getExplicitCharset();
        String co = columnInfo.getExplicitCollation();
        if (StringUtils.isNotEmpty(cs)) {
            sb.append(" CHARACTER SET ").append(cs);
        }
        if (StringUtils.isNotEmpty(co)) {
            sb.append(" COLLATE ").append(co);
        }
    }

    protected void appendCommon(ColumnInfo columnInfo, StringBuilder sb) {
        if (columnInfo.getTypeLength() == 0) {
            sb.append("`" + columnInfo.getName() + "` " + columnInfo.getType());
        }
        if (columnInfo.getTypeLength() == 1) {
            sb.append("`" + columnInfo.getName() + "` " + columnInfo.getType() + "(" + columnInfo.getLength() + ")");
        }
        if (columnInfo.getTypeLength() == 2) {
            sb.append("`" + columnInfo.getName() + "` ");
            sb.append(columnInfo.getType() + "(" + columnInfo.getLength() + "," + columnInfo.getDecimalLength() + ")");
        }
        appendColumnCharsetCollation(columnInfo, sb);
        if (columnInfo.isNull()) {
            sb.append(" NULL");
        } else {
            sb.append(" NOT NULL");
        }
        if (columnInfo.isAutoIncrement()) {
            sb.append(" AUTO_INCREMENT");
        }
        if (!"NULL".equals(columnInfo.getDefaultValue())) {
            sb.append(" DEFAULT '" + columnInfo.getDefaultValue() + "'");
        }
        if (!StringUtils.isEmpty(columnInfo.getComment())) {
            sb.append(" COMMENT '" + columnInfo.getComment() + "'");
        }
    }

    protected void appendCommonEmbeddedH2(ColumnInfo columnInfo, StringBuilder sb) {
        if (columnInfo.getTypeLength() == 0) {
            sb.append("`").append(columnInfo.getName()).append("` ").append(columnInfo.getType());
        }
        if (columnInfo.getTypeLength() == 1) {
            sb.append("`").append(columnInfo.getName()).append("` ").append(columnInfo.getType())
                    .append("(").append(columnInfo.getLength()).append(")");
        }
        if (columnInfo.getTypeLength() == 2) {
            sb.append("`").append(columnInfo.getName()).append("` ");
            sb.append(columnInfo.getType()).append("(").append(columnInfo.getLength()).append(",")
                    .append(columnInfo.getDecimalLength()).append(")");
        }
        if (columnInfo.isNull()) {
            sb.append(" NULL");
        } else {
            sb.append(" NOT NULL");
        }
        if (columnInfo.isAutoIncrement()) {
            sb.append(" AUTO_INCREMENT");
        }
        if (!"NULL".equals(columnInfo.getDefaultValue())) {
            sb.append(" DEFAULT '").append(columnInfo.getDefaultValue()).append("'");
        }
    }

    private String addColumnsInternal(final Map<String, Map<TableInfo, ColumnInfo>> map, String action) {
        StringBuilder sb = new StringBuilder();
        Map<TableInfo, ColumnInfo> parameter = map.get(RelationalSchemaSql.paramName);
        for (Map.Entry<TableInfo, ColumnInfo> kv : parameter.entrySet()) {
            TableInfo tableInfo = kv.getKey();
            sb.append("ALTER TABLE `" + tableInfo.getName() + "` " + action + " ");
            ColumnInfo columnInfo = kv.getValue();
            appendCommon(columnInfo, sb);
            if (columnInfo.isKey()) {
                sb.append(" PRIMARY KEY");
            }
            sb.append(";");
        }
        return sb.toString();
    }

    @Override
    public String dropColumn(final Map<String, Map<TableInfo, String>> map) {
        StringBuilder stringBuilder = new StringBuilder();
        Map<TableInfo, String> parameter = map.get(RelationalSchemaSql.paramName);
        for (Map.Entry<TableInfo, String> kv : parameter.entrySet()) {
            TableInfo tableInfo = kv.getKey();
            stringBuilder.append("ALTER TABLE `" + tableInfo.getName() + "`");
            String columnInfo = kv.getValue();
            stringBuilder.append(" DROP `" + columnInfo + "`");
            stringBuilder.append(";");
        }
        return stringBuilder.toString();
    }

    @Override
    public String dropPrimaryKey(final Map<String, Map<TableInfo, ColumnInfo>> map) {
        StringBuilder stringBuilder = new StringBuilder();
        Map<TableInfo, ColumnInfo> parameter = map.get(RelationalSchemaSql.paramName);
        for (Map.Entry<TableInfo, ColumnInfo> kv : parameter.entrySet()) {
            TableInfo tableInfo = kv.getKey();
            stringBuilder.append("ALTER TABLE `" + tableInfo.getName() + "` MODIFY");
            ColumnInfo columnInfo = kv.getValue();
            appendCommon(columnInfo, stringBuilder);
            stringBuilder.append(", DROP PRIMARY KEY");
            stringBuilder.append(";");
        }
        return stringBuilder.toString();
    }

    @Override
    public String dropIndex(final Map<String, Map<TableInfo, Object>> map) throws NoSuchFieldException, IllegalAccessException {
        StringBuilder stringBuilder = new StringBuilder();
        Map<TableInfo, Object> parameter = map.get(RelationalSchemaSql.paramName);
        for (Map.Entry<TableInfo, Object> kv : parameter.entrySet()) {
            TableInfo tableInfo = kv.getKey();
            stringBuilder.append("ALTER TABLE `" + tableInfo.getName() + "` DROP INDEX");
            Object indexInfo = kv.getValue();
            Field field = indexInfo.getClass().getDeclaredField("name");
            field.setAccessible(true);
            String name = (String) field.get(indexInfo);
            stringBuilder.append(" `" + name + "`");
            stringBuilder.append(";");
        }
        return stringBuilder.toString();
    }

    @Override
    public String addIndex(final Map<String, Map<TableInfo, IndexInfo>> map) {
        StringBuilder stringBuilder = new StringBuilder();
        Map<TableInfo, IndexInfo> parameter = map.get(RelationalSchemaSql.paramName);
        for (Map.Entry<TableInfo, IndexInfo> ii : parameter.entrySet()) {
            stringBuilder.append("ALTER TABLE `" + ii.getKey().getName() + "` ADD ");
            IndexInfo indexInfo = ii.getValue();
            if (!IndexTypeEnum.NORMAL.toString().equals(indexInfo.getIndexType())) {
                stringBuilder.append(indexInfo.getIndexType() + " ");
            }
            stringBuilder.append("INDEX ");
            stringBuilder.append("`").append(indexInfo.getName()).append("` (");
            Iterator<Map.Entry<String, Integer>> iterator = indexInfo.getFields().entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, Integer> kv = iterator.next();
                if (kv.getValue() > 0) {
                    stringBuilder.append("`").append(kv.getKey()).append("`(").append(kv.getValue()).append(")");
                } else {
                    stringBuilder.append("`").append(kv.getKey()).append("`");
                }
                if (iterator.hasNext()) {
                    stringBuilder.append(",");
                }
            }
            stringBuilder.append(")");
            stringBuilder.append(" USING ").append(indexInfo.getIndexMethod().toUpperCase());
            stringBuilder.append(";");
        }
        return stringBuilder.toString();
    }

    @Override
    public String addColumns(final Map<String, Map<TableInfo, ColumnInfo>> map) {
        return addColumnsInternal(map, "ADD");
    }

    @Override
    public String modifyColumn(final Map<String, Map<TableInfo, ColumnInfo>> map) {
        return addColumnsInternal(map, "MODIFY");
    }

    @Override
    public String createTable(final Map<String, Map<TableInfo, List<ColumnInfo>>> map) {
        StringBuilder stringBuilder = new StringBuilder();
        Map<TableInfo, List<ColumnInfo>> parameter = map.get(RelationalSchemaSql.paramName);
        for (Map.Entry<TableInfo, List<ColumnInfo>> kv : parameter.entrySet()) {
            TableInfo tableInfo = kv.getKey();
            stringBuilder.append("CREATE TABLE `").append(tableInfo.getName()).append("`(");
            List<ColumnInfo> list = kv.getValue();
            String primaryKey = "";
            for (ColumnInfo columnInfo : list) {
                appendCommon(columnInfo, stringBuilder);
                if (columnInfo.isKey()) {
                    String split = ",";
                    if (StringUtils.isBlank(primaryKey)) {
                        split = "";
                    }
                    primaryKey = primaryKey + split + "`" + columnInfo.getName() + "`";
                }
                if (columnInfo.isUnique()) {
                    stringBuilder.append(", UNIQUE KEY (`").append(columnInfo.getName()).append("`)");
                }
                if (list.indexOf(columnInfo) != list.size() - 1) {
                    stringBuilder.append(", ");
                }
            }

            if (!StringUtils.isBlank(primaryKey)) {
                stringBuilder.append(", PRIMARY KEY (").append(primaryKey).append(")");
                stringBuilder.append(" USING BTREE");
            }
            if (tableInfo.getUniqueKeyInfos().size() > 0) {
                stringBuilder.append(", ").append(tableInfo.buildUniqueSql());
            }
            if (tableInfo.getIndexInfos().size() > 0) {
                stringBuilder.append(", ").append(tableInfo.buildIndexSql());
            }

            stringBuilder.append(")");
            stringBuilder.append(" ENGINE=").append(tableInfo.getEngine());
            stringBuilder.append(" DEFAULT CHARACTER SET ").append(tableInfo.getCharset());
            if (StringUtils.isNotBlank(tableInfo.getCollation())) {
                stringBuilder.append(" COLLATE ").append(tableInfo.getCollation());
            }
            if (!StringUtils.isEmpty(tableInfo.getComment())) {
                stringBuilder.append(" COMMENT='").append(tableInfo.getComment()).append("'");
            }
            stringBuilder.append(";");
        }
        return stringBuilder.toString();
    }
}
