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

    @Override
    public String sqlTimestampBucketDay(String quotedColumn) {
        return "TO_CHAR(" + quotedColumn + ", 'YYYY-MM-DD')";
    }

    @Override
    public String sqlTimestampBucketMonth(String quotedColumn) {
        return "TO_CHAR(" + quotedColumn + ", 'YYYY-MM')";
    }

    @Override
    public String sqlTimestampBucketYear(String quotedColumn) {
        return "TO_CHAR(" + quotedColumn + ", 'YYYY')";
    }

    @Override
    public String sqlTimestampBucketIsoWeek(String quotedColumn) {
        return "TO_CHAR(" + quotedColumn + ", 'IYYY') || '-W' || LPAD(TO_CHAR(" + quotedColumn + ", 'IW'), 2, '0')";
    }

    @Override
    public String sqlLimitOffsetSuffix(long limit, long offset) {
        return " OFFSET " + offset + " ROWS FETCH NEXT " + limit + " ROWS ONLY";
    }

    @Override
    public String sqlLowerColumnContainsNeedle(String quotedColumn, String mybatisNeedleParam) {
        return "INSTR(LOWER(NVL(" + quotedColumn + ", '')), " + mybatisNeedleParam + ") > 0";
    }

    @Override
    public String sqlLimitOffsetMybatisParams(String limitPh, String offsetPh) {
        return " OFFSET " + offsetPh + " ROWS FETCH NEXT " + limitPh + " ROWS ONLY";
    }

    @Override
    public String sqlEpochMillisFromTimestamp(String quotedTimestampExpr) {
        return "COALESCE((CAST(" + quotedTimestampExpr + " AS DATE) - DATE '1970-01-01') * 86400000,0)";
    }

    @Override
    public String sqlUpdateWithJoin(String targetTable, String targetAlias, String joinTable, String joinAlias, String joinOn, String quotedColumn, String extraWhere) {
        return "MERGE INTO " + targetTable + " " + targetAlias + " USING " + joinTable + " " + joinAlias + " ON (" + joinOn + " AND " + extraWhere + ")"
                + " WHEN MATCHED THEN UPDATE SET " + targetAlias + "." + quotedColumn + " = 1";
    }
}
