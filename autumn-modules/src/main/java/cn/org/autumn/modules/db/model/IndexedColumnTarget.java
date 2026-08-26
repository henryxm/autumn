package cn.org.autumn.modules.db.model;

import lombok.Getter;

/**
 * @Table 实体上某索引列的扫描目标。
 */
@Getter
public class IndexedColumnTarget {

    private final String entityClass;
    private final String entityFullName;
    private final String tableName;
    private final String columnName;
    private final TableScanColumnMeta columnMeta;

    public IndexedColumnTarget(Class<?> entityType, String tableName, String columnName, TableScanColumnMeta columnMeta) {
        this.entityClass = entityType.getSimpleName();
        this.entityFullName = entityType.getName();
        this.tableName = tableName;
        this.columnName = columnName;
        this.columnMeta = columnMeta == null ? TableScanColumnMeta.stringDefault() : columnMeta;
    }

    public String getColumnType() {
        return columnMeta.displayType();
    }
}
