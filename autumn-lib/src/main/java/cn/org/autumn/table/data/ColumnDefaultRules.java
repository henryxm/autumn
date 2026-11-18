package cn.org.autumn.table.data;

import org.apache.commons.lang3.StringUtils;
import lombok.extern.slf4j.Slf4j;

/**
 * MySQL 系建表/改表时列默认值与不可索引类型的兼容规则。
 * <p>
 * TEXT/BLOB/JSON/GEOMETRY 等（及自增列）不能安全附带「字面量」{@code DEFAULT}；注解若仍写了
 * {@code defaultValue}（含空串 {@code ""}），会导致建表失败。此处将默认值重置为
 * {@code NULL}（DDL 不输出 DEFAULT），并 WARN 提示开发者修改注解。
 * <p>
 * MySQL 8.0.13+ 允许的括号表达式默认值（如 {@code (JSON_OBJECT())}、{@code ('')}）予以保留。
 * 数值/日期等非字符串类型上的空串默认值同样清理。
 */
@Slf4j
public final class ColumnDefaultRules {

    private ColumnDefaultRules() {
    }

    /**
     * 注解/元数据中的默认值是否会在 MySQL DDL 中输出（非 sentinel {@code NULL}）。
     * 空串 {@code ""} 也会输出为 {@code DEFAULT ''}，对 LOB/数值类型非法。
     */
    public static boolean willEmitMysqlDefault(String defaultValue) {
        return defaultValue != null && !"NULL".equalsIgnoreCase(defaultValue.trim());
    }

    /**
     * MySQL 8.0.13+ 表达式默认值形态：整段由括号包裹（如 {@code (0)}、{@code (JSON_ARRAY())}）。
     */
    public static boolean isParenthesizedDefaultExpression(String defaultValue) {
        if (StringUtils.isBlank(defaultValue)) {
            return false;
        }
        String dv = defaultValue.trim();
        return dv.length() >= 2 && dv.startsWith("(") && dv.endsWith(")");
    }

    /**
     * MySQL 上不宜（或不能）带显式字面量列 DEFAULT 的类型：TEXT/BLOB 族、JSON、GEOMETRY 等。
     */
    public static boolean forbidsExplicitMysqlDefault(String type) {
        String t = IndexPrefixRules.normalizeMysqlType(type);
        if (t.isEmpty()) {
            return false;
        }
        if (ColumnInfo.isLobTextType(t)) {
            return true;
        }
        return "json".equals(t)
                || "geometry".equals(t)
                || "point".equals(t)
                || "linestring".equals(t)
                || "polygon".equals(t)
                || "multipoint".equals(t)
                || "multilinestring".equals(t)
                || "multipolygon".equals(t)
                || "geometrycollection".equals(t);
    }

    /**
     * 不能作为普通 BTREE {@code UNIQUE KEY (col)} 的类型（需函数/生成列索引，框架不自动生成）。
     */
    public static boolean forbidsMysqlBtreeKey(String type) {
        String t = IndexPrefixRules.normalizeMysqlType(type);
        if (t.isEmpty()) {
            return false;
        }
        return "json".equals(t)
                || "geometry".equals(t)
                || "point".equals(t)
                || "linestring".equals(t)
                || "polygon".equals(t)
                || "multipoint".equals(t)
                || "multilinestring".equals(t)
                || "multipolygon".equals(t)
                || "geometrycollection".equals(t);
    }

    /**
     * 允许 {@code DEFAULT ''} 的类型（字符/二进制串）；数值、时间、LOB 等不允许空串字面量。
     */
    public static boolean allowsEmptyStringMysqlDefault(String type) {
        String t = IndexPrefixRules.normalizeMysqlType(type);
        if (t.isEmpty()) {
            return false;
        }
        switch (t) {
            case "char":
            case "varchar":
            case "binary":
            case "varbinary":
            case "enum":
            case "set":
                return true;
            default:
                return false;
        }
    }

    /**
     * 就地清理非法默认值，返回是否发生了清理。
     *
     * @param ownerHint 实体/表提示，写入日志便于定位
     */
    public static boolean sanitizeMysqlColumnDefault(ColumnInfo columnInfo, String ownerHint) {
        return sanitizeMysqlColumnDefault(columnInfo, ownerHint, true);
    }

    /**
     * @param logWarn 为 false 时仅静默修正（用于 DDL 发射兜底，避免与 collect 双 WARN）
     */
    public static boolean sanitizeMysqlColumnDefault(ColumnInfo columnInfo, String ownerHint, boolean logWarn) {
        if (columnInfo == null) {
            return false;
        }
        String dv = columnInfo.getDefaultValue();
        if (!willEmitMysqlDefault(dv)) {
            return false;
        }
        String owner = StringUtils.defaultIfBlank(ownerHint, "?");
        String col = StringUtils.defaultIfBlank(columnInfo.getName(), "?");
        String type = StringUtils.defaultIfBlank(columnInfo.getType(), "?");
        String trimmed = dv.trim();

        if (columnInfo.isAutoIncrement()) {
            warn(logWarn, "Column default ignored (AUTO_INCREMENT forbids DEFAULT): owner={} column={} type={} defaultValue={}",
                    owner, col, type, dv);
            columnInfo.setDefaultValue("NULL");
            return true;
        }

        if (forbidsExplicitMysqlDefault(columnInfo.getType())) {
            if (isParenthesizedDefaultExpression(trimmed)) {
                // MySQL 8.0.13+ expression default; keep as-is
                return false;
            }
            warn(logWarn, "Column default ignored (type forbids literal DEFAULT): owner={} column={} type={} defaultValue={}",
                    owner, col, type, dv);
            columnInfo.setDefaultValue("NULL");
            return true;
        }

        if (trimmed.isEmpty() && !allowsEmptyStringMysqlDefault(columnInfo.getType())) {
            warn(logWarn, "Column default ignored (empty string invalid for type): owner={} column={} type={} defaultValue={}",
                    owner, col, type, dv);
            columnInfo.setDefaultValue("NULL");
            return true;
        }
        return false;
    }

    private static void warn(boolean logWarn, String format, Object... args) {
        if (logWarn) {
            log.warn(format, args);
        }
    }
}
