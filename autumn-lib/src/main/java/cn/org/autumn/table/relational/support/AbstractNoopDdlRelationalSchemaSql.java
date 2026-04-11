package cn.org.autumn.table.relational.support;

import cn.org.autumn.table.relational.RelationalSchemaSql;
import cn.org.autumn.table.data.ColumnInfo;
import cn.org.autumn.table.data.IndexInfo;
import cn.org.autumn.table.data.TableInfo;

import java.util.List;
import java.util.Map;

/**
 * DDL 变更类方法统一返回占位查询；元数据方法由子类实现或继续占位。
 * 适用于当前以 JDBC 元数据为主、尚未接注解同步的厂商。
 */
public abstract class AbstractNoopDdlRelationalSchemaSql implements RelationalSchemaSql {

    private final String ddlNoop;

    protected AbstractNoopDdlRelationalSchemaSql(String ddlNoop) {
        this.ddlNoop = ddlNoop;
    }

    protected String ddlNoop() {
        return ddlNoop;
    }

    @Override
    public String getTableCharacterSetName() {
        return ddlNoop;
    }

    @Override
    public String convertTableCharset(Map<String, Object> map) {
        return ddlNoop;
    }

    @Override
    public String addIndex(Map<String, Map<TableInfo, IndexInfo>> map) {
        return ddlNoop;
    }

    @Override
    public String addColumns(Map<String, Map<TableInfo, ColumnInfo>> map) {
        return ddlNoop;
    }

    @Override
    public String modifyColumn(Map<String, Map<TableInfo, ColumnInfo>> map) {
        return ddlNoop;
    }

    @Override
    public String dropColumn(Map<String, Map<TableInfo, String>> map) {
        return ddlNoop;
    }

    @Override
    public String dropPrimaryKey(Map<String, Map<TableInfo, ColumnInfo>> map) {
        return ddlNoop;
    }

    @Override
    public String dropIndex(Map<String, Map<TableInfo, Object>> map) throws NoSuchFieldException, IllegalAccessException {
        return ddlNoop;
    }

    @Override
    public String createTable(Map<String, Map<TableInfo, List<ColumnInfo>>> map) {
        return ddlNoop;
    }
}
