package cn.org.autumn.table.relational.support;

import cn.org.autumn.table.relational.RelationalSchemaSql;
import cn.org.autumn.table.data.ColumnInfo;
import cn.org.autumn.table.data.IndexInfo;
import cn.org.autumn.table.data.TableInfo;

import java.util.List;
import java.util.Map;

/**
 * 全部方法返回同一占位 SQL：供当前仍以 JDBC 为主的厂商（如 Firebird、Informix）占位，避免误接 MyBatis 时执行危险 DDL。
 */
public abstract class AbstractFullNoopRelationalSchemaSql implements RelationalSchemaSql {

    private final String noop;

    protected AbstractFullNoopRelationalSchemaSql(String noop) {
        this.noop = noop;
    }

    protected String noop() {
        return noop;
    }

    @Override
    public String getColumnMetas() {
        return noop;
    }

    @Override
    public String getTableMetas(Map<String, Object> map) {
        return noop;
    }

    @Override
    public String getTableCount() {
        return noop;
    }

    @Override
    public String hasTable() {
        return noop;
    }

    @Override
    public String getTableCharacterSetName() {
        return noop;
    }

    @Override
    public String convertTableCharset(Map<String, Object> map) {
        return noop;
    }

    @Override
    public String dropTable(Map<String, String> map) {
        return noop;
    }

    @Override
    public String showKeys(Map<String, String> map) {
        return noop;
    }

    @Override
    public String showIndex(Map<String, String> map) {
        return noop;
    }

    @Override
    public String addIndex(Map<String, Map<TableInfo, IndexInfo>> map) {
        return noop;
    }

    @Override
    public String addColumns(Map<String, Map<TableInfo, ColumnInfo>> map) {
        return noop;
    }

    @Override
    public String modifyColumn(Map<String, Map<TableInfo, ColumnInfo>> map) {
        return noop;
    }

    @Override
    public String dropColumn(Map<String, Map<TableInfo, String>> map) {
        return noop;
    }

    @Override
    public String dropPrimaryKey(Map<String, Map<TableInfo, ColumnInfo>> map) {
        return noop;
    }

    @Override
    public String dropIndex(Map<String, Map<TableInfo, Object>> map) throws NoSuchFieldException, IllegalAccessException {
        return noop;
    }

    @Override
    public String createTable(Map<String, Map<TableInfo, List<ColumnInfo>>> map) {
        return noop;
    }
}
