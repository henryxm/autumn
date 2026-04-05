package cn.org.autumn.database.runtime;

import org.springframework.stereotype.Component;

@Component
public class PostgresqlRuntimeSqlDialect implements RuntimeSqlDialect {

    @Override
    public String quote(String identifier) {
        if (identifier == null) {
            return "\"\"";
        }
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }

    @Override
    public String columnValueInCommaSeparatedList(String qualifiedColumn, String csvInner) {
        String esc = csvInner == null ? "" : csvInner.replace("'", "''");
        return "CAST(" + qualifiedColumn + " AS VARCHAR) = ANY(string_to_array('" + esc + "', ','))";
    }
}
