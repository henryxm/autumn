package cn.org.autumn.table.platform;

import cn.org.autumn.database.AutumnDatabaseHolder;
import cn.org.autumn.database.AutumnDatabaseType;
import cn.org.autumn.table.data.IndexInfo;
import cn.org.autumn.table.data.TableInfo;
import cn.org.autumn.table.data.UniqueKeyInfo;
import cn.org.autumn.table.mysql.ColumnMeta;
import cn.org.autumn.table.mysql.TableMeta;
import cn.org.autumn.table.platform.mysql.MysqlRelationalTableOperations;
import cn.org.autumn.table.platform.postgresql.PostgresRelationalTableOperations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 按 {@code autumn.database} 在 MySQL 系与 PostgreSQL 之间路由建表/结构同步操作。
 */
@Primary
@Component
public class RoutingRelationalTableOperations implements RelationalTableOperations {

    @Autowired
    private AutumnDatabaseHolder databaseHolder;

    @Autowired
    private MysqlRelationalTableOperations mysqlRelationalTableOperations;

    @Autowired
    private PostgresRelationalTableOperations postgresRelationalTableOperations;

    private RelationalTableOperations delegate() {
        AutumnDatabaseType t = databaseHolder.getType();
        if (t == AutumnDatabaseType.POSTGRESQL) {
            return postgresRelationalTableOperations;
        }
        return mysqlRelationalTableOperations;
    }

    @Override
    public void createTable(Map<TableInfo, List<Object>> map) {
        delegate().createTable(map);
    }

    @Override
    public boolean hasTable(String tableName) {
        return delegate().hasTable(tableName);
    }

    @Override
    public String getTableCharacterSetName(String tableName) {
        return delegate().getTableCharacterSetName(tableName);
    }

    @Override
    public void convertTableCharset(String tableName, String charset, String collation) {
        delegate().convertTableCharset(tableName, charset, collation);
    }

    @Override
    public List<ColumnMeta> getColumnMetas(String tableName) {
        return delegate().getColumnMetas(tableName);
    }

    @Override
    public List<TableMeta> getTableMetas(String tableName, int offset, int rows) {
        return delegate().getTableMetas(tableName, offset, rows);
    }

    @Override
    public List<TableMeta> getTableMetas(String tableName) {
        return delegate().getTableMetas(tableName);
    }

    @Override
    public List<UniqueKeyInfo> getTableKeys(String tableName) {
        return delegate().getTableKeys(tableName);
    }

    @Override
    public List<IndexInfo> getTableIndex(String tableName) {
        return delegate().getTableIndex(tableName);
    }

    @Override
    public Integer getTableCount() {
        return delegate().getTableCount();
    }

    @Override
    public void addColumns(Map<TableInfo, Object> map) {
        delegate().addColumns(map);
    }

    @Override
    public void modifyColumn(Map<TableInfo, Object> map) {
        delegate().modifyColumn(map);
    }

    @Override
    public void dropColumn(Map<TableInfo, Object> map) {
        delegate().dropColumn(map);
    }

    @Override
    public void dropPrimaryKey(Map<TableInfo, Object> map) {
        delegate().dropPrimaryKey(map);
    }

    @Override
    public void dropIndex(Map<TableInfo, Object> map) {
        delegate().dropIndex(map);
    }

    @Override
    public void dropTable(String tableName) {
        delegate().dropTable(tableName);
    }

    @Override
    public void addIndex(Map<TableInfo, Object> map) {
        delegate().addIndex(map);
    }
}
