package cn.org.autumn.database.runtime;

import org.springframework.stereotype.Component;

/**
 * IBM DB2 与 Apache Derby：双引号标识符；{@code FETCH FIRST 1 ROW ONLY}；逗号列表用 {@code LOCATE}。
 */
@Component
public class Db2DerbyRuntimeSqlDialect implements RuntimeSqlDialect {

    @Override
    public String quote(String identifier) {
        if (identifier == null) {
            return "\"\"";
        }
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }

    @Override
    public String limitOne() {
        return " FETCH FIRST 1 ROW ONLY";
    }

    @Override
    public String columnValueInCommaSeparatedList(String qualifiedColumn, String csvInner) {
        String esc = csvInner == null ? "" : csvInner.replace("'", "''");
        return "LOCATE(',' || CAST(" + qualifiedColumn + " AS VARCHAR(32672)) || ',', ',' || '" + esc + "' || ',') > 0";
    }

    @Override
    public String likeContainsAny(String mybatisParamPlaceholder) {
        return "'%' || CAST(" + mybatisParamPlaceholder + " AS VARCHAR(32672)) || '%'";
    }
}
