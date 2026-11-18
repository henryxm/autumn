package cn.org.autumn.table.relational;

import cn.org.autumn.table.data.ColumnInfo;
import cn.org.autumn.table.data.IndexInfo;
import cn.org.autumn.table.data.TableInfo;
import java.util.List;
import java.util.Map;

/**
 * 关系库 DDL / 元数据查询 SQL 方言契约，与 {@code TableDao}、{@code PostgresTableDao} 的 {@code @SelectProvider} 方法签名一致。
 * <p>
 * 各数据库在 {@code cn.org.autumn.table.relational.dialect.*} 下提供对等实现，由 {@link RelationalSchemaSqlCatalog}、
 * {@link RoutingRelationalSchemaSql}、{@link RelationalSchemaSqlRegistry} 在运行期选用。
 */
public interface RelationalSchemaSql {

    String paramName = "paramName";

    String getColumnMetas();

    String getTableMetas(Map<String, Object> map);

    String getTableCount();

    String hasTable();

    String getTableCharacterSetName();

    String convertTableCharset(Map<String, Object> map);

    String dropTable(Map<String, String> map);

    String showKeys(Map<String, String> map);

    String showIndex(Map<String, String> map);

    /**
     * 当前库全部列元数据（启动批量预载）。不支持时返回空串，调用方勿执行。
     */
    default String listSchemaColumnMetas() {
        return "";
    }

    /**
     * 当前库全部索引行（对齐 SHOW INDEX 字段别名）。不支持时返回空串。
     */
    default String listSchemaIndexRows() {
        return "";
    }

    /**
     * 当前库全部表名与字符集。不支持时返回空串。
     */
    default String listSchemaTableCharsets() {
        return "";
    }

    String addIndex(Map<String, Map<TableInfo, IndexInfo>> map);

    /**
     * 重命名索引；value 为 {@link cn.org.autumn.table.data.index.IndexRebuildTask}。
     * 不支持时返回空串。
     */
    default String renameIndex(Map<String, Map<TableInfo, Object>> map) {
        return "";
    }

    String addColumns(Map<String, Map<TableInfo, ColumnInfo>> map);

    String modifyColumn(Map<String, Map<TableInfo, ColumnInfo>> map);

    String dropColumn(Map<String, Map<TableInfo, String>> map);

    String dropPrimaryKey(Map<String, Map<TableInfo, ColumnInfo>> map);

    String dropIndex(Map<String, Map<TableInfo, Object>> map) throws NoSuchFieldException, IllegalAccessException;

    String createTable(Map<String, Map<TableInfo, List<ColumnInfo>>> map);
}
