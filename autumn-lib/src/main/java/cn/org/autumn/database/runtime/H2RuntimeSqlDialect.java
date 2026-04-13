package cn.org.autumn.database.runtime;

import org.springframework.stereotype.Component;

/**
 * H2 / HSQLDB：双引号标识符；{@code LIMIT 1}；逗号列表用 {@code LOCATE}（与 PageHelper 5.1.x 将二者映射到同一分页方言一致）。
 */
@Component
public class H2RuntimeSqlDialect implements RuntimeSqlDialect {

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
        return "LOCATE(',' || CAST(" + qualifiedColumn + " AS VARCHAR) || ',', ',' || '" + esc + "' || ',') > 0";
    }

    @Override
    public String likeContainsAny(String mybatisParamPlaceholder) {
        return "concat('%', CAST(" + mybatisParamPlaceholder + " AS VARCHAR), '%')";
    }

    @Override
    public String sqlTimestampBucketDay(String quotedColumn) {
        return "FORMATDATETIME(" + quotedColumn + ", 'yyyy-MM-dd')";
    }

    @Override
    public String sqlTimestampBucketMonth(String quotedColumn) {
        return "FORMATDATETIME(" + quotedColumn + ", 'yyyy-MM')";
    }

    @Override
    public String sqlTimestampBucketYear(String quotedColumn) {
        return "FORMATDATETIME(" + quotedColumn + ", 'yyyy')";
    }

    @Override
    public String sqlTimestampBucketIsoWeek(String quotedColumn) {
        String c = quotedColumn;
        return "FORMATDATETIME(" + c + ", 'yyyy') || '-W' || RIGHT('0' || CAST(WEEK(" + c + ") AS VARCHAR), 2)";
    }

    @Override
    public String sqlLimitOffsetSuffix(long limit, long offset) {
        return " LIMIT " + limit + " OFFSET " + offset;
    }

    @Override
    public String sqlLowerColumnContainsNeedle(String quotedColumn, String mybatisNeedleParam) {
        return "LOCATE(" + mybatisNeedleParam + ", LOWER(COALESCE(" + quotedColumn + ", ''))) > 0";
    }
}
