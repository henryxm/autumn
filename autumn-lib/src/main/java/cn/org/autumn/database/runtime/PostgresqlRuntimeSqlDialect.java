package cn.org.autumn.database.runtime;

import org.springframework.stereotype.Component;

@Component
public class PostgresqlRuntimeSqlDialect implements RuntimeSqlDialect {

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
        return "CAST(" + qualifiedColumn + " AS VARCHAR) = ANY(string_to_array('" + esc + "', ','))";
    }

    @Override
    public String enabledTrueSqlLiteral() {
        return "TRUE";
    }

    @Override
    public String enabledFalseSqlLiteral() {
        return "FALSE";
    }

    @Override
    public String sqlTimestampBucketDay(String quotedColumn) {
        return "to_char(" + quotedColumn + ", 'YYYY-MM-DD')";
    }

    @Override
    public String sqlTimestampBucketMonth(String quotedColumn) {
        return "to_char(" + quotedColumn + ", 'YYYY-MM')";
    }

    @Override
    public String sqlTimestampBucketYear(String quotedColumn) {
        return "to_char(" + quotedColumn + ", 'YYYY')";
    }

    @Override
    public String sqlTimestampBucketIsoWeek(String quotedColumn) {
        return "to_char(" + quotedColumn + ", 'IYYY\"-W\"IW')";
    }

    @Override
    public String sqlLimitOffsetSuffix(long limit, long offset) {
        return " LIMIT " + limit + " OFFSET " + offset;
    }

    @Override
    public String sqlLowerColumnContainsNeedle(String quotedColumn, String mybatisNeedleParam) {
        return "POSITION(" + mybatisNeedleParam + " IN LOWER(COALESCE(" + quotedColumn + ", ''))) > 0";
    }
}
