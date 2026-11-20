package cn.org.autumn.table.data.index;

/**
 * 索引同步运行时开关（系统属性优先，兼容无 Spring 场景）。
 * <p>
 * {@code autumn.table.index.rebuild-on-order-mismatch} 默认 {@code true}：
 * 列序/方向不一致时按索引<strong>成对</strong> DROP+ADD（见 {@code MysqlTableService#rebuildIndexes}），
 * 启动日志含 {@code Index rebuild upgrade begin/done}；大表可能耗时。
 * 设为 {@code false} 时退回「仅列集合」比对（逃生舱，跳过错序纠正）。
 * <p>
 * 限制配额（见 {@link IndexLimitGuard}）：
 * {@code max-columns-per-index}/{@code max-per-table}/{@code max-key-bytes}/
 * {@code skip-on-limit-violation}/{@code warn-unique-blank-default}。
 * <p>
 * Query→Index 与 Entity {@code @Indexes} 均为默认支持的索引来源（无单独开关）；
 * 合并见 {@code QueryIndexMerger}。
 */
public final class IndexTableSettings {

    public static final String PROP_REBUILD_ON_ORDER_MISMATCH = "autumn.table.index.rebuild-on-order-mismatch";
    public static final String PROP_MAX_COLUMNS_PER_INDEX = "autumn.table.index.max-columns-per-index";
    public static final String PROP_MAX_PER_TABLE = "autumn.table.index.max-per-table";
    public static final String PROP_MAX_KEY_BYTES = "autumn.table.index.max-key-bytes";
    public static final String PROP_SKIP_ON_LIMIT_VIOLATION = "autumn.table.index.skip-on-limit-violation";
    public static final String PROP_WARN_UNIQUE_BLANK_DEFAULT = "autumn.table.index.warn-unique-blank-default";
    public static final String PROP_MAX_NAME_LENGTH = "autumn.table.index.max-name-length";

    public static final int DEFAULT_MAX_COLUMNS_PER_INDEX = 16;
    public static final int DEFAULT_MAX_PER_TABLE = 64;
    public static final int DEFAULT_MAX_KEY_BYTES = 3072;

    private IndexTableSettings() {
    }

    public static boolean rebuildOnOrderMismatch() {
        return booleanProp(PROP_REBUILD_ON_ORDER_MISMATCH, true);
    }

    /** 单索引最大列数（MySQL 16）。 */
    public static int maxColumnsPerIndex() {
        return intProp(PROP_MAX_COLUMNS_PER_INDEX, DEFAULT_MAX_COLUMNS_PER_INDEX);
    }

    /** 单表索引建议上限（含 isUnique 内联 UNIQUE）；仅 WARN，除非 skip-on-limit。 */
    public static int maxPerTable() {
        return intProp(PROP_MAX_PER_TABLE, DEFAULT_MAX_PER_TABLE);
    }

    /** utf8mb4 键长粗估上限；0 关闭粗估。 */
    public static int maxKeyBytes() {
        return intProp(PROP_MAX_KEY_BYTES, DEFAULT_MAX_KEY_BYTES);
    }

    /**
     * 软限制（键长粗估 / 表索引数）超限时是否 skipEmit。
     * 列数硬限 / 未知列不受本开关影响，默认仍会 skipEmit。
     */
    public static boolean skipOnLimitViolation() {
        return booleanProp(PROP_SKIP_ON_LIMIT_VIOLATION, false);
    }

    public static boolean warnUniqueBlankDefault() {
        return booleanProp(PROP_WARN_UNIQUE_BLANK_DEFAULT, true);
    }

    /** 0=跟随方言 {@link IndexDialectCapability#maxIndexNameLength}。 */
    public static int maxNameLengthOverride() {
        return intProp(PROP_MAX_NAME_LENGTH, 0);
    }

    private static boolean booleanProp(String key, boolean defaultValue) {
        String v = System.getProperty(key);
        if (v == null || v.isEmpty()) {
            return defaultValue;
        }
        String t = v.trim();
        if ("true".equalsIgnoreCase(t) || "1".equals(t)) {
            return true;
        }
        if ("false".equalsIgnoreCase(t) || "0".equals(t)) {
            return false;
        }
        return defaultValue;
    }

    private static int intProp(String key, int defaultValue) {
        String v = System.getProperty(key);
        if (v == null || v.isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(v.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
