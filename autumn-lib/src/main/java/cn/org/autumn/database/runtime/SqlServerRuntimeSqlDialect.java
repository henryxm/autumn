package cn.org.autumn.database.runtime;

import org.springframework.stereotype.Component;

/**
 * SQL Server：标识符方括号；逗号列表成员判断对齐 {@code FIND_IN_SET}；取一行需 {@code ORDER BY} 以满足 {@code OFFSET/FETCH}。
 */
@Component
public class SqlServerRuntimeSqlDialect implements RuntimeSqlDialect {

    @Override
    public String quote(String identifier) {
        if (identifier == null) {
            return "[]";
        }
        return "[" + identifier.replace("]", "]]") + "]";
    }

    @Override
    public String limitOne() {
        return " ORDER BY (SELECT NULL) OFFSET 0 ROWS FETCH NEXT 1 ROW ONLY";
    }

    @Override
    public String likeContainsAny(String mybatisParamPlaceholder) {
        return "concat('%', cast(" + mybatisParamPlaceholder + " as varchar(max)), '%')";
    }

    @Override
    public String columnValueInCommaSeparatedList(String qualifiedColumn, String csvInner) {
        String esc = csvInner == null ? "" : csvInner.replace("'", "''");
        return "CHARINDEX(','+LTRIM(RTRIM(CAST(" + qualifiedColumn + " AS VARCHAR(MAX))))+',', ','+'" + esc + "'+',') > 0";
    }

    @Override
    public String sqlTimestampBucketDay(String quotedColumn) {
        return "FORMAT(" + quotedColumn + ", 'yyyy-MM-dd')";
    }

    @Override
    public String sqlTimestampBucketMonth(String quotedColumn) {
        return "FORMAT(" + quotedColumn + ", 'yyyy-MM')";
    }

    @Override
    public String sqlTimestampBucketYear(String quotedColumn) {
        return "FORMAT(" + quotedColumn + ", 'yyyy')";
    }

    @Override
    public String sqlTimestampBucketIsoWeek(String quotedColumn) {
        String c = quotedColumn;
        String isoYear = "(CASE WHEN MONTH(" + c + ") = 1 AND DATEPART(ISO_WEEK, " + c + ") > 50 THEN YEAR(" + c + ") - 1 "
                + "WHEN MONTH(" + c + ") = 12 AND DATEPART(ISO_WEEK, " + c + ") < 10 THEN YEAR(" + c + ") + 1 "
                + "ELSE YEAR(" + c + ") END)";
        return "CONCAT(" + isoYear + ", '-W', RIGHT(CONCAT('0', CAST(DATEPART(ISO_WEEK, " + c + ") AS VARCHAR(2))), 2))";
    }

    @Override
    public String sqlLimitOffsetSuffix(long limit, long offset) {
        return " OFFSET " + offset + " ROWS FETCH NEXT " + limit + " ROWS ONLY";
    }

    @Override
    public String sqlLowerColumnContainsNeedle(String quotedColumn, String mybatisNeedleParam) {
        return "CHARINDEX(" + mybatisNeedleParam + ", LOWER(ISNULL(" + quotedColumn + ", ''))) > 0";
    }
}
