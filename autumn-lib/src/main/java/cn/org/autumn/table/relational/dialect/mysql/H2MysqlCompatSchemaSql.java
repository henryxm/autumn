package cn.org.autumn.table.relational.dialect.mysql;

import cn.org.autumn.table.annotation.IndexTypeEnum;
import cn.org.autumn.table.relational.RelationalSchemaSql;
import cn.org.autumn.table.data.ColumnInfo;
import cn.org.autumn.table.data.IndexInfo;
import cn.org.autumn.table.data.TableInfo;
import org.apache.commons.lang.StringUtils;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * 内嵌 H2 + {@code MODE=MySQL}：剥离 MySQL 表级 DDL、索引 {@code USING}、前缀长度等 H2 不支持的语法。
 */
public final class H2MysqlCompatSchemaSql extends MysqlSchemaSql {

    public static final H2MysqlCompatSchemaSql INSTANCE = new H2MysqlCompatSchemaSql();

    @Override
    protected void appendColumnDefinition(final ColumnInfo columnInfo, final StringBuilder sb) {
        appendCommonEmbeddedH2(columnInfo, sb);
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
            String idxName = "ix_" + h2IndexTablePrefix(ii.getKey().getName()) + "_" + indexInfo.getName();
            stringBuilder.append("`").append(idxName).append("` (");
            Iterator<Map.Entry<String, Integer>> iterator = indexInfo.getFields().entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, Integer> kv = iterator.next();
                stringBuilder.append("`").append(kv.getKey()).append("`");
                if (iterator.hasNext()) {
                    stringBuilder.append(",");
                }
            }
            stringBuilder.append(")");
            stringBuilder.append(";");
        }
        return stringBuilder.toString();
    }

    @Override
    public String createTable(final Map<String, Map<TableInfo, List<ColumnInfo>>> map) {
        StringBuilder stringBuilder = new StringBuilder();
        Map<TableInfo, List<ColumnInfo>> parameter = map.get(RelationalSchemaSql.paramName);
        for (Map.Entry<TableInfo, List<ColumnInfo>> kv : parameter.entrySet()) {
            TableInfo tableInfo = kv.getKey();
            stringBuilder.append("CREATE TABLE IF NOT EXISTS `").append(tableInfo.getName()).append("`(");
            List<ColumnInfo> list = kv.getValue();
            String primaryKey = "";
            for (ColumnInfo columnInfo : list) {
                appendColumnDefinition(columnInfo, stringBuilder);
                if (columnInfo.isKey()) {
                    String split = ",";
                    if (StringUtils.isBlank(primaryKey)) {
                        split = "";
                    }
                    primaryKey = primaryKey + split + "`" + columnInfo.getName() + "`";
                }
                if (columnInfo.isUnique()) {
                    String tab = h2IndexTablePrefix(tableInfo.getName());
                    stringBuilder.append(", UNIQUE KEY `uk_").append(tab).append("_").append(columnInfo.getName())
                            .append("` (`").append(columnInfo.getName()).append("`)");
                }
                if (list.indexOf(columnInfo) != list.size() - 1) {
                    stringBuilder.append(", ");
                }
            }

            if (!StringUtils.isBlank(primaryKey)) {
                stringBuilder.append(", PRIMARY KEY (").append(primaryKey).append(")");
            }
            if (tableInfo.getUniqueKeyInfos().size() > 0) {
                String uniq = tableInfo.buildUniqueSql();
                uniq = flattenH2SecondaryIndexNames(stripMysqlIndexPrefixLengths(stripIndexUsingClauses(uniq)),
                        tableInfo.getName());
                stringBuilder.append(", ").append(uniq);
            }
            if (tableInfo.getIndexInfos().size() > 0) {
                String idx = tableInfo.buildIndexSql();
                idx = flattenH2SecondaryIndexNames(stripMysqlIndexPrefixLengths(stripIndexUsingClauses(idx)),
                        tableInfo.getName());
                stringBuilder.append(", ").append(idx);
            }

            stringBuilder.append(");");
        }
        return stringBuilder.toString();
    }
}
