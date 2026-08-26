package cn.org.autumn.modules.db.service;

import cn.org.autumn.annotation.FieldEncrypt;
import cn.org.autumn.modules.db.model.IndexedColumnTarget;
import cn.org.autumn.modules.db.model.TableScanColumnMeta;
import cn.org.autumn.table.annotation.Column;
import cn.org.autumn.table.data.ColumnInfo;
import cn.org.autumn.table.data.IndexInfo;
import cn.org.autumn.table.data.TableInfo;
import cn.org.autumn.table.platform.RelationalTableOperations;
import cn.org.autumn.table.service.MysqlTableService;
import cn.org.autumn.table.utils.HumpConvert;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 从 @Table 实体注解索引元数据中解析指定列名的扫描候选。
 */
@Component
public class EntityIndexedColumnResolver {

    @Autowired
    private MysqlTableService mysqlTableService;

    @Autowired
    private RelationalTableOperations tableOperations;

    public List<IndexedColumnTarget> resolve(String columnName) {
        if (StringUtils.isBlank(columnName)) {
            return List.of();
        }
        String normalizedNeedle = normalizeColumnKey(columnName.trim());
        Set<String> seen = new LinkedHashSet<>();
        List<IndexedColumnTarget> out = new ArrayList<>();
        for (Class<?> clazz : mysqlTableService.getClasses()) {
            TableInfo tableInfo = new TableInfo(clazz);
            if (!tableInfo.isValid()) {
                continue;
            }
            String tableName = tableInfo.getName();
            if (StringUtils.isBlank(tableName)) {
                continue;
            }
            try {
                if (!tableOperations.hasTable(tableName)) {
                    continue;
                }
            } catch (Exception ignored) {
                continue;
            }
            for (IndexInfo indexInfo : tableInfo.getIndexInfosCombine()) {
                if (indexInfo == null || indexInfo.getFields() == null) {
                    continue;
                }
                for (String indexedColumn : indexInfo.getFields().keySet()) {
                    if (StringUtils.isBlank(indexedColumn)) {
                        continue;
                    }
                    if (!columnKeysMatch(normalizedNeedle, indexedColumn)) {
                        continue;
                    }
                    String dedupeKey = tableName.toLowerCase(Locale.ROOT) + "\0" + indexedColumn.toLowerCase(Locale.ROOT);
                    if (!seen.add(dedupeKey)) {
                        continue;
                    }
                    TableScanColumnMeta columnMeta = resolveColumnMeta(clazz, indexedColumn);
                    out.add(new IndexedColumnTarget(clazz, tableName, indexedColumn, columnMeta));
                }
            }
        }
        return out;
    }

    public TableScanColumnMeta findColumnMeta(String tableName, String columnName) {
        if (StringUtils.isAnyBlank(tableName, columnName)) {
            return TableScanColumnMeta.stringDefault();
        }
        for (Class<?> clazz : mysqlTableService.getClasses()) {
            TableInfo tableInfo = new TableInfo(clazz);
            if (!tableInfo.isValid()) {
                continue;
            }
            if (!tableName.equalsIgnoreCase(tableInfo.getName())) {
                continue;
            }
            return resolveColumnMeta(clazz, columnName);
        }
        return TableScanColumnMeta.stringDefault();
    }

    TableScanColumnMeta resolveColumnMeta(Class<?> clazz, String dbColumnName) {
        Field field = findColumnField(clazz, dbColumnName);
        if (field == null) {
            return TableScanColumnMeta.stringDefault();
        }
        ColumnInfo columnInfo = ColumnInfo.from(field);
        FieldEncrypt fieldEncrypt = field.getAnnotation(FieldEncrypt.class);
        return new TableScanColumnMeta(
                columnInfo.getType(),
                field.getType(),
                columnInfo.isEnumType(),
                columnInfo.getEnumConstants(),
                fieldEncrypt != null
        );
    }

    private Field findColumnField(Class<?> clazz, String dbColumnName) {
        String needle = normalizeColumnKey(dbColumnName);
        for (Field field : allColumnFields(clazz)) {
            Column column = field.getAnnotation(Column.class);
            if (column == null) {
                continue;
            }
            String name = StringUtils.isNotBlank(column.value()) ? column.value() : HumpConvert.HumpToUnderline(field.getName());
            if (columnKeysMatch(needle, name)) {
                return field;
            }
        }
        return null;
    }

    private static Field[] allColumnFields(Class<?> clazz) {
        Field[] fields = clazz.getDeclaredFields();
        Class<?> parent = clazz.getSuperclass();
        while (parent != null && parent != Object.class) {
            fields = (Field[]) ArrayUtils.addAll(fields, parent.getDeclaredFields());
            parent = parent.getSuperclass();
        }
        return fields;
    }

    static boolean columnKeysMatch(String normalizedNeedle, String indexedColumn) {
        return normalizeColumnKey(indexedColumn).equals(normalizedNeedle);
    }

    static String normalizeColumnKey(String columnName) {
        if (columnName == null) {
            return "";
        }
        return columnName.replace("_", "").toLowerCase(Locale.ROOT).trim();
    }
}
