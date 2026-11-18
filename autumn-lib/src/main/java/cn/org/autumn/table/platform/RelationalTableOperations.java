package cn.org.autumn.table.platform;

import cn.org.autumn.table.data.IndexInfo;
import cn.org.autumn.table.data.TableInfo;
import cn.org.autumn.table.data.UniqueKeyInfo;
import cn.org.autumn.table.relational.model.ColumnMeta;
import cn.org.autumn.table.relational.model.SchemaSyncMetadata;
import cn.org.autumn.table.relational.model.TableMeta;
import java.util.List;
import java.util.Map;

/**
 * 注解驱动建表与结构同步所需的数据库操作，由 {@link cn.org.autumn.table.platform.RoutingRelationalTableOperations} 路由到
 * MySQL/MariaDB、PostgreSQL，或 Oracle/SQL Server 的 JDBC 元数据实现（后两者未开放完整 DDL）。
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

    /**
     * 一次性预载当前库表/列/索引/字符集；不支持时返回 {@code null}（调用方回退逐表查询）。
     */
    default SchemaSyncMetadata loadSchemaSyncMetadata() {
        return null;
    }

    Integer getTableCount();

    void addColumns(Map<TableInfo, Object> map);

    void modifyColumn(Map<TableInfo, Object> map);

    void dropColumn(Map<TableInfo, Object> map);

    void dropPrimaryKey(Map<TableInfo, Object> map);

    void dropIndex(Map<TableInfo, Object> map);

    void dropTable(String tableName);

    void addIndex(Map<TableInfo, Object> map);

    /**
     * 仅改索引名。value 为 {@link cn.org.autumn.table.data.index.IndexRebuildTask}（含旧名/新名）。
     * 不支持时抛异常，由调用方回退 DROP+ADD。
     */
    void renameIndex(Map<TableInfo, Object> map);
}
