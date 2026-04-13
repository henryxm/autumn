package cn.org.autumn.database.runtime;

import org.springframework.stereotype.Component;

/**
 * Firebird 3+：双引号；{@code FETCH FIRST}；{@code POSITION(... IN ...)}；分页在 PageHelper 5.1.x 中可显式指定为 {@code sqlserver2012} 方言。
 */
@Component
public class FirebirdRuntimeSqlDialect implements RuntimeSqlDialect {

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
        return "POSITION(',' || CAST(" + qualifiedColumn + " AS VARCHAR(32000)) || ',' IN ',' || '" + esc + "' || ',') > 0";
    }

    @Override
    public String likeContainsAny(String mybatisParamPlaceholder) {
        return "'%' || " + mybatisParamPlaceholder + " || '%'";
    }

    @Override
    public String sqlTimestampBucketDay(String quotedColumn) {
        String c = quotedColumn;
        return "(CAST(EXTRACT(YEAR FROM " + c + ") AS VARCHAR(4)) || '-' || LPAD(CAST(EXTRACT(MONTH FROM " + c + ") AS VARCHAR(2)), 2, '0') || '-' || LPAD(CAST(EXTRACT(DAY FROM " + c + ") AS VARCHAR(2)), 2, '0'))";
    }

    @Override
    public String sqlTimestampBucketMonth(String quotedColumn) {
        String c = quotedColumn;
        return "(CAST(EXTRACT(YEAR FROM " + c + ") AS VARCHAR(4)) || '-' || LPAD(CAST(EXTRACT(MONTH FROM " + c + ") AS VARCHAR(2)), 2, '0'))";
    }

    @Override
    public String sqlTimestampBucketYear(String quotedColumn) {
        return "CAST(EXTRACT(YEAR FROM " + quotedColumn + ") AS VARCHAR(4))";
    }

    @Override
    public String sqlTimestampBucketIsoWeek(String quotedColumn) {
        String c = quotedColumn;
        return "(CAST(EXTRACT(YEAR FROM " + c + ") AS VARCHAR(4)) || '-W' || LPAD(CAST(EXTRACT(WEEK FROM " + c + ") AS VARCHAR(2)), 2, '0'))";
    }

    @Override
    public String sqlLimitOffsetSuffix(long limit, long offset) {
        return " ROWS " + (offset + 1) + " TO " + (offset + limit);
    }

    @Override
    public String sqlLowerColumnContainsNeedle(String quotedColumn, String mybatisNeedleParam) {
        return "POSITION(" + mybatisNeedleParam + " IN LOWER(COALESCE(" + quotedColumn + ", ''))) > 0";
    }
}
