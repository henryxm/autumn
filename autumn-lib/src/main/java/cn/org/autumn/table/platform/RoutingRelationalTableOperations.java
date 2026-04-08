package cn.org.autumn.table.platform;

import cn.org.autumn.database.DatabaseHolder;
import cn.org.autumn.database.DatabaseType;
import cn.org.autumn.table.data.IndexInfo;
import cn.org.autumn.table.data.TableInfo;
import cn.org.autumn.table.data.UniqueKeyInfo;
import cn.org.autumn.table.mysql.ColumnMeta;
import cn.org.autumn.table.mysql.TableMeta;
import cn.org.autumn.table.platform.jdbc.OracleJdbcRelationalTableOperations;
import cn.org.autumn.table.platform.jdbc.SqlServerJdbcRelationalTableOperations;
import cn.org.autumn.table.platform.mysql.MysqlRelationalTableOperations;
import cn.org.autumn.table.dao.postgresql.PostgresRelationalTableOperations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 按 {@code autumn.database} 路由：PostgreSQL、Oracle、SQL Server、MySQL/MariaDB（后两者共用 MySQL 实现）。
 */
@Primary
@Component
public class RoutingRelationalTableOperations implements RelationalTableOperations {

    @Autowired
    private DatabaseHolder databaseHolder;

    @Autowired
    private MysqlRelationalTableOperations mysqlRelationalTableOperations;

    @Autowired
    private PostgresRelationalTableOperations postgresRelationalTableOperations;

    @Autowired
    private OracleJdbcRelationalTableOperations oracleJdbcRelationalTableOperations;

    @Autowired
    private SqlServerJdbcRelationalTableOperations sqlServerJdbcRelationalTableOperations;

    private RelationalTableOperations delegate() {
        DatabaseType t = databaseHolder.getType();
        if (t == DatabaseType.POSTGRESQL) {
            return postgresRelationalTableOperations;
        }
        if (t == DatabaseType.ORACLE) {
            return oracleJdbcRelationalTableOperations;
        }
        if (t == DatabaseType.SQLSERVER) {
            return sqlServerJdbcRelationalTableOperations;
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
