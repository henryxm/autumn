package cn.org.autumn.table.platform;

import cn.org.autumn.table.data.IndexInfo;
import cn.org.autumn.table.data.TableInfo;
import cn.org.autumn.table.data.UniqueKeyInfo;
import cn.org.autumn.table.mysql.ColumnMeta;
import cn.org.autumn.table.mysql.TableMeta;

import java.util.List;
import java.util.Map;

/**
 * 注解驱动建表与结构同步所需的数据库操作，按库类型由 {@link cn.org.autumn.table.platform.RoutingRelationalTableOperations} 路由到 MySQL 或 PostgreSQL 实现。
 */
public interface RelationalTableOperations {

    void createTable(Map<TableInfo, List<Object>> map);

    boolean hasTable(String tableName);

    String getTableCharacterSetName(String tableName);

    void convertTableCharset(String tableName, String charset, String collation);

    List<ColumnMeta> getColumnMetas(String tableName);

    List<TableMeta> getTableMetas(String tableName, int offset, int rows);

    List<TableMeta> getTableMetas(String tableName);

    List<UniqueKeyInfo> getTableKeys(String tableName);

    List<IndexInfo> getTableIndex(String tableName);

    Integer getTableCount();

    void addColumns(Map<TableInfo, Object> map);

    void modifyColumn(Map<TableInfo, Object> map);

    void dropColumn(Map<TableInfo, Object> map);

    void dropPrimaryKey(Map<TableInfo, Object> map);

    void dropIndex(Map<TableInfo, Object> map);

    void dropTable(String tableName);

    void addIndex(Map<TableInfo, Object> map);
}
