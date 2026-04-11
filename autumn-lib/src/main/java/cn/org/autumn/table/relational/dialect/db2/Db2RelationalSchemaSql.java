package cn.org.autumn.table.relational.dialect.db2;

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
 * IBM DB2：元数据以 JDBC 为主；DDL 由 {@link AnsiDoubleQuotedDdlGenerator} 生成（需 LUW 11+ {@code IF NOT EXISTS} 语义）。
 */
public final class Db2RelationalSchemaSql extends AbstractFullNoopRelationalSchemaSql {

    public static final Db2RelationalSchemaSql INSTANCE = new Db2RelationalSchemaSql();

    public Db2RelationalSchemaSql() {
        super(SchemaSqlNoops.ANSI_FALSE);
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
        return AnsiDoubleQuotedDdlGenerator.createTable(AnsiDialect.DB2, map);
    }

    @Override
    public String addColumns(Map<String, Map<TableInfo, ColumnInfo>> map) {
        return AnsiDoubleQuotedDdlGenerator.addColumns(AnsiDialect.DB2, map);
    }

    @Override
    public String modifyColumn(Map<String, Map<TableInfo, ColumnInfo>> map) {
        return AnsiDoubleQuotedDdlGenerator.modifyColumn(AnsiDialect.DB2, map);
    }

    @Override
    public String dropColumn(Map<String, Map<TableInfo, String>> map) {
        return AnsiDoubleQuotedDdlGenerator.dropColumn(AnsiDialect.DB2, map);
    }

    @Override
    public String dropPrimaryKey(Map<String, Map<TableInfo, ColumnInfo>> map) {
        return AnsiDoubleQuotedDdlGenerator.dropPrimaryKey(AnsiDialect.DB2, map);
    }

    @Override
    public String dropIndex(Map<String, Map<TableInfo, Object>> map) throws NoSuchFieldException, IllegalAccessException {
        return AnsiDoubleQuotedDdlGenerator.dropIndex(AnsiDialect.DB2, map);
    }

    @Override
    public String addIndex(Map<String, Map<TableInfo, IndexInfo>> map) {
        return AnsiDoubleQuotedDdlGenerator.addIndex(AnsiDialect.DB2, map);
    }
}
