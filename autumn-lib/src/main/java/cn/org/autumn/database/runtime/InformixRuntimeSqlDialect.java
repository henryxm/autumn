package cn.org.autumn.database.runtime;

import org.springframework.stereotype.Component;

/**
 * Informix（12.10+ / 14.x 等）：双引号；末尾 {@code FETCH FIRST}；逗号列表用 {@code INSTR}。
 */
@Component
public class InformixRuntimeSqlDialect implements RuntimeSqlDialect {

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
        return "INSTR(',' || '" + esc + "' || ',', ',' || TRIM(CAST(" + qualifiedColumn + " AS VARCHAR(255))) || ',') > 0";
    }

    @Override
    public String likeContainsAny(String mybatisParamPlaceholder) {
        return "'%' || " + mybatisParamPlaceholder + " || '%'";
    }
}
