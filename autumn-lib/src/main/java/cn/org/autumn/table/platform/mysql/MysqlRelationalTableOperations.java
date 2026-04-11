package cn.org.autumn.table.platform.mysql;

import cn.org.autumn.table.dao.TableDao;
import cn.org.autumn.table.data.IndexInfo;
import cn.org.autumn.table.data.TableInfo;
import cn.org.autumn.table.data.UniqueKeyInfo;
import cn.org.autumn.table.relational.model.ColumnMeta;
import cn.org.autumn.table.relational.model.TableMeta;
import cn.org.autumn.table.platform.RelationalTableOperations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * MySQL / MariaDB：委托现有 {@link TableDao}（{@link cn.org.autumn.table.relational.provider.QuerySql}）。
 */
@Component
public class MysqlRelationalTableOperations implements RelationalTableOperations {

    @Autowired
    private TableDao tableDao;

    @Override
    public void createTable(Map<TableInfo, List<Object>> map) {
        tableDao.createTable(map);
    }

    @Override
    public boolean hasTable(String tableName) {
        return tableDao.hasTable(tableName);
    }

    @Override
    public String getTableCharacterSetName(String tableName) {
        return tableDao.getTableCharacterSetName(tableName);
    }

    @Override
    public void convertTableCharset(String tableName, String charset, String collation) {
        tableDao.convertTableCharset(tableName, charset, collation);
    }

    @Override
    public List<ColumnMeta> getColumnMetas(String tableName) {
        return tableDao.getColumnMetas(tableName);
    }

    @Override
    public List<TableMeta> getTableMetas(String tableName, int offset, int rows) {
        return tableDao.getTableMetasPage(tableName, offset, rows);
    }

    @Override
    public List<TableMeta> getTableMetas(String tableName) {
        return tableDao.getTableMetas(tableName);
    }

    @Override
    public List<UniqueKeyInfo> getTableKeys(String tableName) {
        return tableDao.getTableKeys(tableName);
    }

    @Override
    public List<IndexInfo> getTableIndex(String tableName) {
        return tableDao.getTableIndex(tableName);
    }

    @Override
    public Integer getTableCount() {
        return tableDao.getTableCount();
    }

    @Override
    public void addColumns(Map<TableInfo, Object> map) {
        tableDao.addColumns(map);
    }

    @Override
    public void modifyColumn(Map<TableInfo, Object> map) {
        tableDao.modifyColumn(map);
    }

    @Override
    public void dropColumn(Map<TableInfo, Object> map) {
        tableDao.dropColumn(map);
    }

    @Override
    public void dropPrimaryKey(Map<TableInfo, Object> map) {
        tableDao.dropPrimaryKey(map);
    }

    @Override
    public void dropIndex(Map<TableInfo, Object> map) {
        tableDao.dropIndex(map);
    }

    @Override
    public void dropTable(String tableName) {
        tableDao.dropTable(tableName);
    }

    @Override
    public void addIndex(Map<TableInfo, Object> map) {
        tableDao.addIndex(map);
    }
}
