package cn.org.autumn.database.runtime;

import org.springframework.stereotype.Component;

@Component
public class MysqlRuntimeSqlDialect implements RuntimeSqlDialect {

    @Override
    public String quote(String identifier) {
        if (identifier == null) {
            return "``";
        }
        return "`" + identifier.replace("`", "``") + "`";
    }

    @Override
    public String columnValueInCommaSeparatedList(String qualifiedColumn, String csvInner) {
        String esc = csvInner == null ? "" : csvInner.replace("'", "''");
        return "FIND_IN_SET(CAST(" + qualifiedColumn + " AS CHAR), '" + esc + "')";
    }
}
