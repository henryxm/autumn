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

    @Override
    public String sqlTimestampBucketDay(String quotedColumn) {
        return "TO_CHAR(" + quotedColumn + ", '%Y-%m-%d')";
    }

    @Override
    public String sqlTimestampBucketMonth(String quotedColumn) {
        return "TO_CHAR(" + quotedColumn + ", '%Y-%m')";
    }

    @Override
    public String sqlTimestampBucketYear(String quotedColumn) {
        return "TO_CHAR(" + quotedColumn + ", '%Y')";
    }

    @Override
    public String sqlTimestampBucketIsoWeek(String quotedColumn) {
        String c = quotedColumn;
        return "TO_CHAR(" + c + ", '%Y') || '-W' || (CASE WHEN WEEK(" + c + ") < 10 THEN '0' ELSE '' END) || WEEK(" + c + ")";
    }

    @Override
    public String sqlLimitOffsetSuffix(long limit, long offset) {
        return " SKIP " + offset + " FIRST " + limit;
    }

    @Override
    public String sqlLowerColumnContainsNeedle(String quotedColumn, String mybatisNeedleParam) {
        return "INSTR(LOWER(COALESCE(" + quotedColumn + ", '')), " + mybatisNeedleParam + ") > 0";
    }
}
