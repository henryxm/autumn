package cn.org.autumn.table.relational.model;

import cn.org.autumn.table.data.IndexInfo;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 注解建表启动期一次性从 information_schema 预载的表/列/索引/字符集快照，
 * 避免对每张表分别 hasTable / getColumnMetas / getTableIndex / getTableCharacterSetName。
 */
public final class SchemaSyncMetadata {

    private final Set<String> tableNames;
    private final Map<String, List<ColumnMeta>> columnsByTable;
    private final Map<String, List<IndexInfo>> indexRowsByTable;
    private final Map<String, String> charsetByTable;

    private SchemaSyncMetadata(Set<String> tableNames,
                               Map<String, List<ColumnMeta>> columnsByTable,
                               Map<String, List<IndexInfo>> indexRowsByTable,
                               Map<String, String> charsetByTable) {
        this.tableNames = tableNames;
        this.columnsByTable = columnsByTable;
        this.indexRowsByTable = indexRowsByTable;
        this.charsetByTable = charsetByTable;
    }

    public static SchemaSyncMetadata from(List<ColumnMeta> columns,
                                          List<IndexInfo> indexRows,
                                          List<TableCharsetMeta> charsets) {
        Set<String> tables = new HashSet<>();
        Map<String, List<ColumnMeta>> cols = new HashMap<>();
        Map<String, List<IndexInfo>> idxs = new HashMap<>();
        Map<String, String> cs = new HashMap<>();

        if (charsets != null) {
            for (TableCharsetMeta row : charsets) {
                if (row == null || StringUtils.isBlank(row.getTableName())) {
                    continue;
                }
                String k = key(row.getTableName());
                tables.add(k);
                if (StringUtils.isNotBlank(row.getCharacterSetName())) {
                    cs.put(k, row.getCharacterSetName());
                }
            }
        }
        if (columns != null) {
            for (ColumnMeta c : columns) {
                if (c == null || StringUtils.isBlank(c.getTableName())) {
                    continue;
                }
                String k = key(c.getTableName());
                tables.add(k);
                cols.computeIfAbsent(k, x -> new ArrayList<>()).add(c);
            }
        }
        if (indexRows != null) {
            for (IndexInfo row : indexRows) {
                if (row == null) {
                    continue;
                }
                String table = StringUtils.isNotBlank(row.getTable()) ? row.getTable() : null;
                if (StringUtils.isBlank(table)) {
                    continue;
                }
                String k = key(table);
                tables.add(k);
                idxs.computeIfAbsent(k, x -> new ArrayList<>()).add(row);
            }
        }
        return new SchemaSyncMetadata(tables, cols, idxs, cs);
    }

    private static String key(String tableName) {
        return tableName.toLowerCase(Locale.ROOT);
    }

    public int tableCount() {
        return tableNames.size();
    }

    public int columnCount() {
        int n = 0;
        for (List<ColumnMeta> list : columnsByTable.values()) {
            n += list.size();
        }
        return n;
    }

    public int indexRowCount() {
        int n = 0;
        for (List<IndexInfo> list : indexRowsByTable.values()) {
            n += list.size();
        }
        return n;
    }

    public boolean hasTable(String tableName) {
        if (StringUtils.isBlank(tableName)) {
            return false;
        }
        return tableNames.contains(key(tableName));
    }

    public void removeTable(String tableName) {
        if (StringUtils.isBlank(tableName)) {
            return;
        }
        String k = key(tableName);
        tableNames.remove(k);
        columnsByTable.remove(k);
        indexRowsByTable.remove(k);
        charsetByTable.remove(k);
    }

    public List<ColumnMeta> getColumns(String tableName) {
        if (StringUtils.isBlank(tableName)) {
            return Collections.emptyList();
        }
        List<ColumnMeta> list = columnsByTable.get(key(tableName));
        return list != null ? list : Collections.emptyList();
    }

    public List<IndexInfo> getIndexRows(String tableName) {
        if (StringUtils.isBlank(tableName)) {
            return Collections.emptyList();
        }
        List<IndexInfo> list = indexRowsByTable.get(key(tableName));
        return list != null ? list : Collections.emptyList();
    }

    public String getCharacterSetName(String tableName) {
        if (StringUtils.isBlank(tableName)) {
            return null;
        }
        return charsetByTable.get(key(tableName));
    }
}
