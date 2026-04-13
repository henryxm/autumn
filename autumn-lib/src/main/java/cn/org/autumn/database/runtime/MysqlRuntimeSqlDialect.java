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

    @Override
    public String sqlTimestampBucketDay(String quotedColumn) {
        return "DATE_FORMAT(" + quotedColumn + ", '%Y-%m-%d')";
    }

    @Override
    public String sqlTimestampBucketMonth(String quotedColumn) {
        return "DATE_FORMAT(" + quotedColumn + ", '%Y-%m')";
    }

    @Override
    public String sqlTimestampBucketYear(String quotedColumn) {
        return "DATE_FORMAT(" + quotedColumn + ", '%Y')";
    }

    @Override
    public String sqlTimestampBucketIsoWeek(String quotedColumn) {
        String yw = "YEARWEEK(" + quotedColumn + ", 3)";
        return "CONCAT(FLOOR(" + yw + " / 100), '-W', LPAD(MOD(" + yw + ", 100), 2, '0'))";
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
