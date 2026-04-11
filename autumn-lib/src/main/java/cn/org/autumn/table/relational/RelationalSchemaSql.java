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

    String addIndex(Map<String, Map<TableInfo, IndexInfo>> map);

    String addColumns(Map<String, Map<TableInfo, ColumnInfo>> map);

    String modifyColumn(Map<String, Map<TableInfo, ColumnInfo>> map);

    String dropColumn(Map<String, Map<TableInfo, String>> map);

    String dropPrimaryKey(Map<String, Map<TableInfo, ColumnInfo>> map);

    String dropIndex(Map<String, Map<TableInfo, Object>> map) throws NoSuchFieldException, IllegalAccessException;

    String createTable(Map<String, Map<TableInfo, List<ColumnInfo>>> map);
}
