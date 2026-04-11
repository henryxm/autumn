package cn.org.autumn.table.relational.support.ddl;

import cn.org.autumn.table.annotation.IndexTypeEnum;
import cn.org.autumn.table.relational.RelationalSchemaSql;
import cn.org.autumn.table.data.ColumnInfo;
import cn.org.autumn.table.data.IndexInfo;
import cn.org.autumn.table.data.TableInfo;
import cn.org.autumn.table.data.UniqueKeyInfo;
import org.apache.commons.lang3.StringUtils;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Microsoft SQL Server 注解建表 DDL（{@code dbo} 架构、方括号标识符）。
 */
public final class SqlServerJdbcDdlGenerator {

    private SqlServerJdbcDdlGenerator() {
    }

    private static String bk(String name) {
        if (name == null) {
            return "[]";
        }
        return "[" + name.replace("]", "]]") + "]";
    }

    private static void appendColumnBody(ColumnInfo c, StringBuilder sb, boolean identityPk) {
        if (identityPk) {
            sb.append(JdbcDdlColumnTypes.sqlServer(c));
            sb.append(" IDENTITY(1,1)");
            return;
        }
        sb.append(JdbcDdlColumnTypes.sqlServer(c));
        if (!c.isNull()) {
            sb.append(" NOT NULL");
        }
        JdbcDdlColumnTypes.appendDefaultClause(c, sb, JdbcDdlColumnTypes.SqlLiteralQuote.SQLSERVER_BIT);
    }

    public static String createTable(Map<String, Map<TableInfo, List<ColumnInfo>>> map) {
        StringBuilder all = new StringBuilder();
        Map<TableInfo, List<ColumnInfo>> parameter = map.get(RelationalSchemaSql.paramName);
        if (parameter == null) {
            return "";
        }
        for (Entry<TableInfo, List<ColumnInfo>> kv : parameter.entrySet()) {
            TableInfo tableInfo = kv.getKey();
            List<ColumnInfo> list = kv.getValue();
            String tname = tableInfo.getName();
            StringBuilder sb = new StringBuilder();
            sb.append("CREATE TABLE dbo.").append(bk(tname)).append(" (");
            StringBuilder pkCols = new StringBuilder();
            for (int i = 0; i < list.size(); i++) {
                ColumnInfo columnInfo = list.get(i);
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(bk(columnInfo.getName())).append(" ");
                boolean idPk = columnInfo.isKey() && columnInfo.isAutoIncrement();
                appendColumnBody(columnInfo, sb, idPk);
                if (idPk) {
                    sb.append(" PRIMARY KEY");
                } else if (columnInfo.isKey()) {
                    String sp = pkCols.length() == 0 ? "" : ",";
                    pkCols.append(sp).append(bk(columnInfo.getName()));
                }
                if (columnInfo.isUnique() && !columnInfo.isKey()) {
                    sb.append(" UNIQUE");
                }
            }
            if (pkCols.length() > 0) {
                sb.append(", PRIMARY KEY (").append(pkCols).append(")");
            }
            if (tableInfo.getUniqueKeyInfos() != null) {
                for (UniqueKeyInfo uk : tableInfo.getUniqueKeyInfos()) {
                    if (StringUtils.isBlank(uk.getName())) {
                        continue;
                    }
                    sb.append(", CONSTRAINT ").append(bk(uk.getName())).append(" UNIQUE (");
                    appendIndexFields(sb, uk.getFields());
                    sb.append(")");
                }
            }
            sb.append(")");
            all.append(sb);
            if (tableInfo.getIndexInfos() != null) {
                for (IndexInfo idx : tableInfo.getIndexInfos()) {
                    idx.resolve();
                    if (IndexTypeEnum.FULLTEXT.name().equals(idx.getIndexType())) {
                        continue;
                    }
                    if (StringUtils.isBlank(idx.getName())) {
                        continue;
                    }
                    all.append("; CREATE ");
                    if (IndexTypeEnum.UNIQUE.name().equals(idx.getIndexType())) {
                        all.append("UNIQUE ");
                    }
                    all.append("INDEX ").append(bk(idx.getName())).append(" ON dbo.").append(bk(tname)).append(" (");
                    appendIndexFields(all, idx.getFields());
                    all.append(")");
                }
            }
            all.append(";");
        }
        return all.toString();
    }

    private static void appendIndexFields(StringBuilder sb, Map<String, Integer> fields) {
        if (fields == null) {
            return;
        }
        Iterator<Entry<String, Integer>> it = fields.entrySet().iterator();
        while (it.hasNext()) {
            Entry<String, Integer> e = it.next();
            if (e.getValue() != null && e.getValue() > 0) {
                sb.append(bk(e.getKey())).append("(").append(e.getValue()).append(")");
            } else {
                sb.append(bk(e.getKey()));
            }
            if (it.hasNext()) {
                sb.append(", ");
            }
        }
    }

    public static String addColumns(Map<String, Map<TableInfo, ColumnInfo>> map) {
        StringBuilder sb = new StringBuilder();
        Map<TableInfo, ColumnInfo> parameter = map.get(RelationalSchemaSql.paramName);
        if (parameter == null) {
            return "";
        }
        for (Entry<TableInfo, ColumnInfo> kv : parameter.entrySet()) {
            ColumnInfo c = kv.getValue();
            sb.append("ALTER TABLE dbo.").append(bk(kv.getKey().getName())).append(" ADD ")
                    .append(bk(c.getName())).append(" ");
            appendColumnBody(c, sb, false);
            sb.append(";");
        }
        return sb.toString();
    }

    public static String modifyColumn(Map<String, Map<TableInfo, ColumnInfo>> map) {
        StringBuilder sb = new StringBuilder();
        Map<TableInfo, ColumnInfo> parameter = map.get(RelationalSchemaSql.paramName);
        if (parameter == null) {
            return "";
        }
        for (Entry<TableInfo, ColumnInfo> kv : parameter.entrySet()) {
            ColumnInfo c = kv.getValue();
            sb.append("ALTER TABLE dbo.").append(bk(kv.getKey().getName())).append(" ALTER COLUMN ")
                    .append(bk(c.getName())).append(" ");
            sb.append(JdbcDdlColumnTypes.sqlServer(c));
            if (!c.isNull()) {
                sb.append(" NOT NULL");
            } else {
                sb.append(" NULL");
            }
            sb.append(";");
        }
        return sb.toString();
    }

    public static String dropColumn(Map<String, Map<TableInfo, String>> map) {
        StringBuilder sb = new StringBuilder();
        Map<TableInfo, String> parameter = map.get(RelationalSchemaSql.paramName);
        if (parameter == null) {
            return "";
        }
        for (Entry<TableInfo, String> kv : parameter.entrySet()) {
            sb.append("ALTER TABLE dbo.").append(bk(kv.getKey().getName())).append(" DROP COLUMN ")
                    .append(bk(kv.getValue())).append(";");
        }
        return sb.toString();
    }

    public static String addIndex(Map<String, Map<TableInfo, IndexInfo>> map) {
        StringBuilder sb = new StringBuilder();
        Map<TableInfo, IndexInfo> parameter = map.get(RelationalSchemaSql.paramName);
        if (parameter == null) {
            return "";
        }
        for (Entry<TableInfo, IndexInfo> ii : parameter.entrySet()) {
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
            sb.append("INDEX ").append(bk(indexInfo.getName())).append(" ON dbo.")
                    .append(bk(ii.getKey().getName())).append(" (");
            appendIndexFields(sb, indexInfo.getFields());
            sb.append(");");
        }
        return sb.toString();
    }

    public static String dropIndex(Map<String, Map<TableInfo, Object>> map)
            throws NoSuchFieldException, IllegalAccessException {
        StringBuilder sb = new StringBuilder();
        Map<TableInfo, Object> parameter = map.get(RelationalSchemaSql.paramName);
        if (parameter == null) {
            return "";
        }
        for (Entry<TableInfo, Object> kv : parameter.entrySet()) {
            java.lang.reflect.Field f = kv.getValue().getClass().getDeclaredField("name");
            f.setAccessible(true);
            String name = (String) f.get(kv.getValue());
            sb.append("DROP INDEX IF EXISTS ").append(bk(name)).append(" ON dbo.")
                    .append(bk(kv.getKey().getName())).append(";");
        }
        return sb.toString();
    }
}
