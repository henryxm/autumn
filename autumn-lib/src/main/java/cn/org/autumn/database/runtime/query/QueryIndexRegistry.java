package cn.org.autumn.database.runtime.query;

import cn.org.autumn.table.data.IndexInfo;
import cn.org.autumn.table.utils.HumpConvert;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 进程级登记：{@link IndexedSelect} 在启动发现期写入的索引（按表名）。
 * 与 Entity {@code @Indexes} 并列；启动同步时由 {@link QueryIndexMerger} 读取。
 * <p>
 * 启动发现结束后 {@link #freezeAfterBootstrap()}：之后 {@link #register} 直接跳过。
 */
public final class QueryIndexRegistry {

    private static final Map<String, List<IndexInfo>> BY_TABLE = new ConcurrentHashMap<>();
    private static final AtomicBoolean FROZEN = new AtomicBoolean(false);

    private QueryIndexRegistry() {
    }

    static String normalizeTable(String table) {
        if (StringUtils.isBlank(table)) {
            return "";
        }
        String t = table.trim();
        if (t.indexOf('_') < 0 && !t.equals(t.toLowerCase(Locale.ROOT))) {
            t = HumpConvert.HumpToUnderline(t);
        }
        return t.toLowerCase(Locale.ROOT);
    }

    /**
     * 登记一条索引定义。已 {@link #freezeAfterBootstrap()} 时 no-op。
     * 同指纹已存在则保留原条目（不替换），降低启动期重复登记的写放大。
     */
    public static void register(String table, IndexInfo index) {
        if (index == null || StringUtils.isBlank(table)) {
            return;
        }
        if (FROZEN.get()) {
            return;
        }
        String key = normalizeTable(table);
        if (key.isEmpty()) {
            return;
        }
        index.setTable(table.trim());
        List<IndexInfo> list = BY_TABLE.computeIfAbsent(key, k -> new CopyOnWriteArrayList<>());
        for (IndexInfo existing : list) {
            if (existing != null && existing.sameIndexColumnsAndKind(index)) {
                return;
            }
        }
        list.add(index);
    }

    /** 启动发现结束后调用；之后运行期 {@link #register} 跳过。 */
    public static void freezeAfterBootstrap() {
        FROZEN.set(true);
    }

    public static boolean isFrozen() {
        return FROZEN.get();
    }

    public static List<IndexInfo> forTable(String table) {
        String key = normalizeTable(table);
        if (key.isEmpty()) {
            return Collections.emptyList();
        }
        List<IndexInfo> list = BY_TABLE.get(key);
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }
        return new ArrayList<>(list);
    }

    public static boolean isEmpty() {
        return BY_TABLE.isEmpty();
    }

    public static int tableCount() {
        return BY_TABLE.size();
    }

    /** 测试 / 重新发现前清空。 */
    public static void clear() {
        BY_TABLE.clear();
        FROZEN.set(false);
    }
}
