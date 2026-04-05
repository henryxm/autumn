package cn.org.autumn.database.runtime;

import org.springframework.stereotype.Component;

/**
 * Oracle：标识符双引号；逗号列表成员判断对齐 {@code FIND_IN_SET} 语义；分页取一行使用 12c+ {@code FETCH FIRST}。
 */
@Component
public class OracleRuntimeSqlDialect implements RuntimeSqlDialect {

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
    public String likeContainsAny(String mybatisParamPlaceholder) {
        return "'%' || " + mybatisParamPlaceholder + " || '%'";
    }

    @Override
    public String columnValueInCommaSeparatedList(String qualifiedColumn, String csvInner) {
        String esc = csvInner == null ? "" : csvInner.replace("'", "''");
        return "INSTR(','||'" + esc + "'||',', ','||CAST(" + qualifiedColumn + " AS VARCHAR2(4000))||',') > 0";
    }
}
