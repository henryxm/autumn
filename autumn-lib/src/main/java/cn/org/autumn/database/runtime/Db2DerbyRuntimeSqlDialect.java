package cn.org.autumn.database.runtime;

import org.springframework.stereotype.Component;

/**
 * IBM DB2 与 Apache Derby：双引号标识符；{@code FETCH FIRST 1 ROW ONLY}；逗号列表用 {@code LOCATE}。
 * <p>
 * 注解 DDL 将实体 {@code tinyint(1)} 开关列映为 {@code BOOLEAN}（见 {@code JdbcDdlColumnTypes#ansiDoubleQuoted}），
 * 手写 SQL 中不能与整型字面量 {@code 1} 比较，需 {@code TRUE}（与 {@link PostgresqlRuntimeSqlDialect} 对齐）。
 */
@Component
public class Db2DerbyRuntimeSqlDialect implements RuntimeSqlDialect {

    @Override
    public String enabledTrueSqlLiteral() {
        return "TRUE";
    }

    @Override
    public String enabledFalseSqlLiteral() {
        return "FALSE";
    }

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

    @Override
    public String sqlTimestampBucketDay(String quotedColumn) {
        String c = quotedColumn;
        return "(CAST(EXTRACT(YEAR FROM " + c + ") AS VARCHAR(4)) || '-' || RIGHT('0' || CAST(EXTRACT(MONTH FROM " + c + ") AS VARCHAR(2)), 2) || '-' || RIGHT('0' || CAST(EXTRACT(DAY FROM " + c + ") AS VARCHAR(2)), 2))";
    }

    @Override
    public String sqlTimestampBucketMonth(String quotedColumn) {
        String c = quotedColumn;
        return "(CAST(EXTRACT(YEAR FROM " + c + ") AS VARCHAR(4)) || '-' || RIGHT('0' || CAST(EXTRACT(MONTH FROM " + c + ") AS VARCHAR(2)), 2))";
    }

    @Override
    public String sqlTimestampBucketYear(String quotedColumn) {
        return "CAST(EXTRACT(YEAR FROM " + quotedColumn + ") AS VARCHAR(4))";
    }

    @Override
    public String sqlTimestampBucketIsoWeek(String quotedColumn) {
        String c = quotedColumn;
        return "(CAST(EXTRACT(YEAR FROM " + c + ") AS VARCHAR(4)) || '-W' || RIGHT('0' || CAST(WEEK(" + c + ") AS VARCHAR(2)), 2))";
    }

    @Override
    public String sqlLimitOffsetSuffix(long limit, long offset) {
        return " OFFSET " + offset + " ROWS FETCH FIRST " + limit + " ROWS ONLY";
    }

    @Override
    public String sqlLowerColumnContainsNeedle(String quotedColumn, String mybatisNeedleParam) {
        return "LOCATE(" + mybatisNeedleParam + ", LOWER(COALESCE(" + quotedColumn + ", ''))) > 0";
    }
}
