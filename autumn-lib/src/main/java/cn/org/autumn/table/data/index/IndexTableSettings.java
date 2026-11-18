package cn.org.autumn.table.data.index;

/**
 * 索引同步运行时开关（系统属性优先，兼容无 Spring 场景）。
 * <p>
 * {@code autumn.table.index.rebuild-on-order-mismatch} 默认 {@code true}：
 * 列序/方向不一致时按索引<strong>成对</strong> DROP+ADD（见 {@code MysqlTableService#rebuildIndexes}），
 * 启动日志含 {@code Index rebuild upgrade begin/done}；大表可能耗时。
 * 设为 {@code false} 时退回「仅列集合」比对（逃生舱，跳过错序纠正）。
 */
public final class IndexTableSettings {

    public static final String PROP_REBUILD_ON_ORDER_MISMATCH = "autumn.table.index.rebuild-on-order-mismatch";

    private IndexTableSettings() {
    }

    public static boolean rebuildOnOrderMismatch() {
        String v = System.getProperty(PROP_REBUILD_ON_ORDER_MISMATCH, "true");
        if (v == null || v.isEmpty()) {
            return true;
        }
        return !"false".equalsIgnoreCase(v.trim()) && !"0".equals(v.trim());
    }
}
