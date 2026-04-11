package cn.org.autumn.table.relational.support.ddl;

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
 * 双引号标识符系（SQLite / H2 / HSQLDB / DB2 / Derby / Firebird / Informix）的 DDL 生成。
 * <p>
 * Derby：Boot 2.7 默认 10.14 在 Java 8 下使用，不支持 {@code CREATE TABLE/INDEX IF NOT EXISTS}、{@code DROP INDEX IF EXISTS}
 *（10.16+ 才具备）；{@link AnsiDialect#DERBY} 按 10.14 能力生成，依赖上层 {@code hasTable} 等避免重复建表。
 * <p>
 * 索引名在 Derby/DB2 等为 schema 内全局唯一（与 MySQL 每表可重名不同），故 CREATE 使用 {@code 表名_逻辑名}，
 * 与 {@link cn.org.autumn.table.data.IndexInfo#relationalSchemaScopedIndexNamesMatch}、{@code MysqlTableService} 迁移比对一致。
 */
public final class AnsiDoubleQuotedDdlGenerator {

    private AnsiDoubleQuotedDdlGenerator() {
    }

    /** 双引号 ANSI 路径下 CREATE INDEX 的物理名（schema 内唯一）。 */
    public static String relationalSchemaScopedIndexName(String tableName, String logicalIndexName) {
        if (StringUtils.isBlank(logicalIndexName)) {
            return logicalIndexName;
        }
        if (StringUtils.isBlank(tableName)) {
            return logicalIndexName;
        }
        return tableName + "_" + logicalIndexName;
    }

    /** Derby 10.14（与当前项目 JDK8 + Boot 管理版本一致）无 IF NOT EXISTS / IF EXISTS DDL 扩展。 */
    private static boolean derbyLegacyDdl(AnsiDialect dialect) {
        return dialect == AnsiDialect.DERBY;
    }

    private static String qi(String name) {
        if (name == null) {
            return "\"\"";
        }
        return "\"" + name.replace("\"", "\"\"") + "\"";
    }

    public static String createTable(AnsiDialect dialect, Map<String, Map<TableInfo, List<ColumnInfo>>> map) {
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
            appendCreateTableHeader(sb, dialect, tname);
            StringBuilder pkCols = new StringBuilder();
            for (int i = 0; i < list.size(); i++) {
                ColumnInfo columnInfo = list.get(i);
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(qi(columnInfo.getName())).append(" ");
                boolean serialPk = columnInfo.isKey() && columnInfo.isAutoIncrement();
                if (serialPk) {
                    appendAutoIncrementColumn(sb, dialect, columnInfo);
                } else {
                    sb.append(JdbcDdlColumnTypes.ansiDoubleQuoted(columnInfo, dialect));
                    appendNullDefault(columnInfo, sb, dialect);
                }
                if (serialPk && dialect != AnsiDialect.SQLITE) {
                    sb.append(" PRIMARY KEY");
                } else if (serialPk && dialect == AnsiDialect.SQLITE) {
                    // INTEGER PRIMARY KEY AUTOINCREMENT already in appendAutoIncrementColumn
                } else if (columnInfo.isKey()) {
                    String sp = pkCols.length() == 0 ? "" : ",";
                    pkCols.append(sp).append(qi(columnInfo.getName()));
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
                    sb.append(", CONSTRAINT ").append(qi(uk.getName())).append(" UNIQUE (");
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
                    if (dialect != AnsiDialect.INFORMIX && !derbyLegacyDdl(dialect)) {
                        all.append("INDEX IF NOT EXISTS ");
                    } else {
                        all.append("INDEX ");
                    }
                    all.append(qi(relationalSchemaScopedIndexName(tname, idx.getName()))).append(" ON ").append(qi(tname)).append(" (");
                    appendIndexFields(all, idx.getFields());
                    all.append(")");
                }
            }
            all.append(";");
        }
        return all.toString();
    }

    private static void appendCreateTableHeader(StringBuilder sb, AnsiDialect dialect, String tname) {
        sb.append("CREATE ");
        if (dialect == AnsiDialect.INFORMIX || derbyLegacyDdl(dialect)) {
            sb.append("TABLE ").append(qi(tname)).append(" (");
        } else {
            sb.append("TABLE IF NOT EXISTS ").append(qi(tname)).append(" (");
        }
    }

    private static void appendAutoIncrementColumn(StringBuilder sb, AnsiDialect dialect, ColumnInfo c) {
        switch (dialect) {
            case SQLITE:
                sb.append("INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT");
                break;
            case INFORMIX:
                if ("bigint".equalsIgnoreCase(c.getType())) {
                    sb.append("BIGSERIAL NOT NULL PRIMARY KEY");
                } else {
                    sb.append("SERIAL NOT NULL PRIMARY KEY");
                }
                break;
            case DB2:
            case DERBY:
                sb.append("BIGINT NOT NULL GENERATED BY DEFAULT AS IDENTITY");
                break;
            case FIREBIRD:
                sb.append("BIGINT GENERATED BY DEFAULT AS IDENTITY NOT NULL");
                break;
            case H2:
            case HSQLDB:
            default:
                if ("bigint".equalsIgnoreCase(c.getType())) {
                    sb.append("BIGINT NOT NULL GENERATED BY DEFAULT AS IDENTITY");
                } else {
                    sb.append("INTEGER NOT NULL GENERATED BY DEFAULT AS IDENTITY");
                }
                break;
        }
    }

    private static void appendNullDefault(ColumnInfo c, StringBuilder sb, AnsiDialect dialect) {
        if (!c.isNull()) {
            sb.append(" NOT NULL");
        }
        JdbcDdlColumnTypes.SqlLiteralQuote q = JdbcDdlColumnTypes.SqlLiteralQuote.ANSI;
        if (dialect == AnsiDialect.SQLITE) {
            JdbcDdlColumnTypes.appendDefaultClause(c, sb, q);
        } else {
            JdbcDdlColumnTypes.appendDefaultClause(c, sb, q);
        }
    }

    /**
     * 仅输出列名。MySQL 风格索引前缀 {@code "col"(n)} 在本生成器覆盖的方言（Derby/DB2/H2/SQLite/…）中均非法，
     * 会在 {@code (} 处语法错误；前缀长度由 {@link cn.org.autumn.table.data.IndexPrefixRules} 为 MySQL 路径保留即可。
     */
    private static void appendIndexFields(StringBuilder sb, Map<String, Integer> fields) {
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

    public static String addColumns(AnsiDialect dialect, Map<String, Map<TableInfo, ColumnInfo>> map) {
        StringBuilder sb = new StringBuilder();
        Map<TableInfo, ColumnInfo> parameter = map.get(RelationalSchemaSql.paramName);
        if (parameter == null) {
            return "";
        }
        for (Entry<TableInfo, ColumnInfo> kv : parameter.entrySet()) {
            TableInfo tableInfo = kv.getKey();
            ColumnInfo c = kv.getValue();
            sb.append("ALTER TABLE ").append(qi(tableInfo.getName())).append(" ADD COLUMN ").append(qi(c.getName()))
                    .append(" ");
            sb.append(JdbcDdlColumnTypes.ansiDoubleQuoted(c, dialect));
            appendNullDefault(c, sb, dialect);
            sb.append(";");
        }
        return sb.toString();
    }

    public static String modifyColumn(AnsiDialect dialect, Map<String, Map<TableInfo, ColumnInfo>> map) {
        if (dialect == AnsiDialect.SQLITE) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        Map<TableInfo, ColumnInfo> parameter = map.get(RelationalSchemaSql.paramName);
        if (parameter == null) {
            return "";
        }
        for (Entry<TableInfo, ColumnInfo> kv : parameter.entrySet()) {
            TableInfo tableInfo = kv.getKey();
            ColumnInfo c = kv.getValue();
            sb.append("ALTER TABLE ").append(qi(tableInfo.getName())).append(" ");
            switch (dialect) {
                case H2:
                case HSQLDB:
                    sb.append("ALTER COLUMN ").append(qi(c.getName())).append(" ")
                            .append(JdbcDdlColumnTypes.ansiDoubleQuoted(c, dialect));
                    if (!c.isNull()) {
                        sb.append(" NOT NULL");
                    } else {
                        sb.append(" NULL");
                    }
                    break;
                case DB2:
                case DERBY:
                    sb.append("ALTER COLUMN ").append(qi(c.getName())).append(" SET DATA TYPE ")
                            .append(JdbcDdlColumnTypes.ansiDoubleQuoted(c, dialect));
                    break;
                case FIREBIRD:
                    sb.append("ALTER COLUMN ").append(qi(c.getName())).append(" TYPE ")
                            .append(JdbcDdlColumnTypes.ansiDoubleQuoted(c, dialect));
                    break;
                case INFORMIX:
                    sb.append("MODIFY ").append(qi(c.getName())).append(" ")
                            .append(JdbcDdlColumnTypes.ansiDoubleQuoted(c, dialect));
                    break;
                default:
                    sb.append("ALTER COLUMN ").append(qi(c.getName())).append(" ")
                            .append(JdbcDdlColumnTypes.ansiDoubleQuoted(c, dialect));
                    break;
            }
            sb.append(";");
        }
        return sb.toString();
    }

    public static String dropColumn(AnsiDialect dialect, Map<String, Map<TableInfo, String>> map) {
        StringBuilder sb = new StringBuilder();
        Map<TableInfo, String> parameter = map.get(RelationalSchemaSql.paramName);
        if (parameter == null) {
            return "";
        }
        for (Entry<TableInfo, String> kv : parameter.entrySet()) {
            if (dialect == AnsiDialect.SQLITE) {
                sb.append("ALTER TABLE ").append(qi(kv.getKey().getName())).append(" DROP COLUMN ")
                        .append(qi(kv.getValue())).append(";");
            } else {
                sb.append("ALTER TABLE ").append(qi(kv.getKey().getName())).append(" DROP COLUMN ")
                        .append(qi(kv.getValue())).append(";");
            }
        }
        return sb.toString();
    }

    public static String dropPrimaryKey(AnsiDialect dialect, Map<String, Map<TableInfo, ColumnInfo>> map) {
        if (dialect == AnsiDialect.SQLITE) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        Map<TableInfo, ColumnInfo> parameter = map.get(RelationalSchemaSql.paramName);
        if (parameter == null) {
            return "";
        }
        for (TableInfo ti : parameter.keySet()) {
            switch (dialect) {
                case INFORMIX:
                case FIREBIRD:
                    break;
                default:
                    sb.append("ALTER TABLE ").append(qi(ti.getName())).append(" DROP PRIMARY KEY;");
                    break;
            }
        }
        return sb.toString();
    }

    public static String dropIndex(AnsiDialect dialect, Map<String, Map<TableInfo, Object>> map)
            throws NoSuchFieldException, IllegalAccessException {
        StringBuilder sb = new StringBuilder();
        Map<TableInfo, Object> parameter = map.get(RelationalSchemaSql.paramName);
        if (parameter == null) {
            return "";
        }
        for (Entry<TableInfo, Object> kv : parameter.entrySet()) {
            Object indexInfo = kv.getValue();
            Field field = indexInfo.getClass().getDeclaredField("name");
            field.setAccessible(true);
            String name = (String) field.get(indexInfo);
            if (dialect == AnsiDialect.INFORMIX || derbyLegacyDdl(dialect)) {
                sb.append("DROP INDEX ").append(qi(name)).append(";");
            } else {
                sb.append("DROP INDEX IF EXISTS ").append(qi(name)).append(";");
            }
        }
        return sb.toString();
    }

    public static String addIndex(AnsiDialect dialect, Map<String, Map<TableInfo, IndexInfo>> map) {
        StringBuilder sb = new StringBuilder();
        Map<TableInfo, IndexInfo> parameter = map.get(RelationalSchemaSql.paramName);
        if (parameter == null) {
            return "";
        }
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
            if (dialect != AnsiDialect.INFORMIX && !derbyLegacyDdl(dialect)) {
                sb.append("INDEX IF NOT EXISTS ");
            } else {
                sb.append("INDEX ");
            }
            sb.append(qi(relationalSchemaScopedIndexName(table.getName(), indexInfo.getName())))
                    .append(" ON ").append(qi(table.getName())).append(" (");
            appendIndexFields(sb, indexInfo.getFields());
            sb.append(");");
        }
        return sb.toString();
    }
}
