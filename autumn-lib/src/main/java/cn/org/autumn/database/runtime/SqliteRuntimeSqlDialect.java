package cn.org.autumn.database.runtime;

import org.springframework.stereotype.Component;

/**
 * SQLite：双引号标识符；{@code LIMIT 1} 单行限制；无 {@code TRUNCATE TABLE} 时用 {@code DELETE FROM}；逗号列表用 {@code instr}。
 * <p>
 * 与 {@link cn.org.autumn.database.DatabaseType#SQLITE}、{@link cn.org.autumn.table.relational.dialect.sqlite.SqliteRelationalSchemaSql} 对齐；单行限制沿用接口默认 {@code LIMIT 1}。
 */
@Component
public class SqliteRuntimeSqlDialect implements RuntimeSqlDialect {

    @Override
    public String quote(String identifier) {
        if (identifier == null) {
            return "\"\"";
        }
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }

    @Override
    public String truncateTable(String tableName) {
        return "DELETE FROM " + quote(tableName);
    }

    @Override
    public String columnValueInCommaSeparatedList(String qualifiedColumn, String csvInner) {
        String esc = csvInner == null ? "" : csvInner.replace("'", "''");
        return "instr(',' || '" + esc + "' || ',', ',' || CAST(" + qualifiedColumn + " AS TEXT) || ',') > 0";
    }

    @Override
    public String likeContainsAny(String mybatisParamPlaceholder) {
        return "'%' || " + mybatisParamPlaceholder + " || '%'";
    }
}
