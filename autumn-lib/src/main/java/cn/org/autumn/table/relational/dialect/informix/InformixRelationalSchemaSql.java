package cn.org.autumn.table.relational.dialect.informix;

import cn.org.autumn.table.relational.RelationalSchemaSql;
import cn.org.autumn.table.relational.support.AbstractFullNoopRelationalSchemaSql;
import cn.org.autumn.table.relational.support.SchemaSqlNoops;
import cn.org.autumn.table.relational.support.ddl.AnsiDialect;
import cn.org.autumn.table.relational.support.ddl.AnsiDoubleQuotedDdlGenerator;
import cn.org.autumn.table.data.ColumnInfo;
import cn.org.autumn.table.data.IndexInfo;
import cn.org.autumn.table.data.TableInfo;

import java.util.List;
import java.util.Map;

/**
 * Informix：元数据以 JDBC 为主；DDL 由 {@link AnsiDoubleQuotedDdlGenerator} 生成（能力因版本而异）。
 */
public final class InformixRelationalSchemaSql extends AbstractFullNoopRelationalSchemaSql {

    public static final InformixRelationalSchemaSql INSTANCE = new InformixRelationalSchemaSql();

    public InformixRelationalSchemaSql() {
        super(SchemaSqlNoops.ANSI_FALSE);
    }

    @Override
    public String dropTable(Map<String, String> map) {
        String t = map.get(RelationalSchemaSql.paramName);
        if (t == null) {
            return SchemaSqlNoops.ANSI_FALSE;
        }
        return "DROP TABLE \"" + t.replace("\"", "\"\"") + "\"";
    }

    @Override
    public String createTable(Map<String, Map<TableInfo, List<ColumnInfo>>> map) {
        return AnsiDoubleQuotedDdlGenerator.createTable(AnsiDialect.INFORMIX, map);
    }

    @Override
    public String addColumns(Map<String, Map<TableInfo, ColumnInfo>> map) {
        return AnsiDoubleQuotedDdlGenerator.addColumns(AnsiDialect.INFORMIX, map);
    }

    @Override
    public String modifyColumn(Map<String, Map<TableInfo, ColumnInfo>> map) {
        return AnsiDoubleQuotedDdlGenerator.modifyColumn(AnsiDialect.INFORMIX, map);
    }

    @Override
    public String dropColumn(Map<String, Map<TableInfo, String>> map) {
        return AnsiDoubleQuotedDdlGenerator.dropColumn(AnsiDialect.INFORMIX, map);
    }

    @Override
    public String dropPrimaryKey(Map<String, Map<TableInfo, ColumnInfo>> map) {
        return AnsiDoubleQuotedDdlGenerator.dropPrimaryKey(AnsiDialect.INFORMIX, map);
    }

    @Override
    public String dropIndex(Map<String, Map<TableInfo, Object>> map) throws NoSuchFieldException, IllegalAccessException {
        return AnsiDoubleQuotedDdlGenerator.dropIndex(AnsiDialect.INFORMIX, map);
    }

    @Override
    public String addIndex(Map<String, Map<TableInfo, IndexInfo>> map) {
        return AnsiDoubleQuotedDdlGenerator.addIndex(AnsiDialect.INFORMIX, map);
    }
}
