package cn.org.autumn.table.relational.dialect.mysql;

import cn.org.autumn.table.data.ColumnDefaultRules;
import cn.org.autumn.table.data.ColumnInfo;
import cn.org.autumn.table.data.IndexPrefixRules;
import org.apache.commons.lang3.StringUtils;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * MySQL/MariaDB 列 {@code DEFAULT} 子句发射：数值不带引号、时间函数裸写、字符串加引号；
 * 空串对 VARCHAR 合法为 {@code DEFAULT ''}。不负责 LOB/自增非法默认的清理（见 {@link ColumnDefaultRules}）。
 */
public final class MysqlColumnDefaultSql {

    private static final Pattern INTEGER_DEFAULT = Pattern.compile("-?\\d+");
    private static final Pattern DECIMAL_DEFAULT = Pattern.compile("-?\\d+(\\.\\d+)?");
    /** CURRENT_TIMESTAMP / CURRENT_TIMESTAMP(n) / NOW() / LOCALTIME / LOCALTIMESTAMP 等 */
    private static final Pattern BARE_TIME_FUNCTION = Pattern.compile(
            "(?i)(CURRENT_TIMESTAMP|CURRENT_TIME|CURRENT_DATE|LOCALTIME|LOCALTIMESTAMP)(\\(\\d+\\))?|NOW\\(\\)");

    private MysqlColumnDefaultSql() {
    }

    public static void appendDefaultClause(ColumnInfo columnInfo, StringBuilder sb) {
        if (columnInfo == null || !ColumnDefaultRules.willEmitMysqlDefault(columnInfo.getDefaultValue())) {
            return;
        }
        String dv = columnInfo.getDefaultValue().trim();
        sb.append(" DEFAULT ");
        if (ColumnDefaultRules.isParenthesizedDefaultExpression(dv)) {
            sb.append(dv);
            return;
        }
        if (BARE_TIME_FUNCTION.matcher(dv).matches()) {
            sb.append(dv);
            return;
        }
        String type = IndexPrefixRules.normalizeMysqlType(columnInfo.getType());
        if (isNumericMysqlType(type)) {
            if (INTEGER_DEFAULT.matcher(dv).matches() || DECIMAL_DEFAULT.matcher(dv).matches()) {
                sb.append(dv);
                return;
            }
        }
        if (isTinyintBoolean(columnInfo) && INTEGER_DEFAULT.matcher(dv).matches() && dv.matches("[01]")) {
            sb.append(dv);
            return;
        }
        if ("true".equalsIgnoreCase(dv) || "false".equalsIgnoreCase(dv)) {
            sb.append("true".equalsIgnoreCase(dv) ? "1" : "0");
            return;
        }
        sb.append("'").append(MysqlSchemaSql.sqlStringLiteral(dv)).append("'");
    }

    static boolean isNumericMysqlType(String normalizedType) {
        if (StringUtils.isBlank(normalizedType)) {
            return false;
        }
        String t = normalizedType.toLowerCase(Locale.ROOT);
        return t.contains("int")
                || "decimal".equals(t)
                || "numeric".equals(t)
                || "float".equals(t)
                || "double".equals(t)
                || "real".equals(t)
                || "bit".equals(t);
    }

    private static boolean isTinyintBoolean(ColumnInfo columnInfo) {
        return columnInfo != null
                && "tinyint".equalsIgnoreCase(IndexPrefixRules.normalizeMysqlType(columnInfo.getType()))
                && columnInfo.getLength() == 1;
    }
}
