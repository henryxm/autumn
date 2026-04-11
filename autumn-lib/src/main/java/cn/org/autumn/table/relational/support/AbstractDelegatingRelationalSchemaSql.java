package cn.org.autumn.table.relational.support;

import cn.org.autumn.table.relational.RelationalSchemaSql;
import cn.org.autumn.table.data.ColumnInfo;
import cn.org.autumn.table.data.IndexInfo;
import cn.org.autumn.table.data.TableInfo;

import java.util.List;
import java.util.Map;

/**
 * 将 {@link RelationalSchemaSql} 全部转发到另一实现（MySQL 族、PG 族委托用）。
 */
public abstract class AbstractDelegatingRelationalSchemaSql implements RelationalSchemaSql {

    private final RelationalSchemaSql delegate;

    protected AbstractDelegatingRelationalSchemaSql(RelationalSchemaSql delegate) {
        this.delegate = delegate;
    }

    protected RelationalSchemaSql delegate() {
        return delegate;
    }

    @Override
    public String getColumnMetas() {
        return delegate.getColumnMetas();
    }

    @Override
    public String getTableMetas(Map<String, Object> map) {
        return delegate.getTableMetas(map);
    }

    @Override
    public String getTableCount() {
        return delegate.getTableCount();
    }

    @Override
    public String hasTable() {
        return delegate.hasTable();
    }

    @Override
    public String getTableCharacterSetName() {
        return delegate.getTableCharacterSetName();
    }

    @Override
    public String convertTableCharset(Map<String, Object> map) {
        return delegate.convertTableCharset(map);
    }

    @Override
    public String dropTable(Map<String, String> map) {
        return delegate.dropTable(map);
    }

    @Override
    public String showKeys(Map<String, String> map) {
        return delegate.showKeys(map);
    }

    @Override
    public String showIndex(Map<String, String> map) {
        return delegate.showIndex(map);
    }

    @Override
    public String addIndex(Map<String, Map<TableInfo, IndexInfo>> map) {
        return delegate.addIndex(map);
    }

    @Override
    public String addColumns(Map<String, Map<TableInfo, ColumnInfo>> map) {
        return delegate.addColumns(map);
    }

    @Override
    public String modifyColumn(Map<String, Map<TableInfo, ColumnInfo>> map) {
        return delegate.modifyColumn(map);
    }

    @Override
    public String dropColumn(Map<String, Map<TableInfo, String>> map) {
        return delegate.dropColumn(map);
    }

    @Override
    public String dropPrimaryKey(Map<String, Map<TableInfo, ColumnInfo>> map) {
        return delegate.dropPrimaryKey(map);
    }

    @Override
    public String dropIndex(Map<String, Map<TableInfo, Object>> map) throws NoSuchFieldException, IllegalAccessException {
        return delegate.dropIndex(map);
    }

    @Override
    public String createTable(Map<String, Map<TableInfo, List<ColumnInfo>>> map) {
        return delegate.createTable(map);
    }
}
