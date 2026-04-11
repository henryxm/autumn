package cn.org.autumn.database.runtime;

import org.springframework.stereotype.Component;

/**
 * SQLite：双引号标识符；无 {@code TRUNCATE TABLE} 时用 {@code DELETE FROM}；逗号列表判断用 {@code instr}。
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
