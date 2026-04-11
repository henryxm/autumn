package cn.org.autumn.table.platform.jdbc;

import cn.org.autumn.database.DatabaseType;
import cn.org.autumn.table.data.IndexInfo;
import cn.org.autumn.table.data.TableInfo;
import cn.org.autumn.table.data.UniqueKeyInfo;
import cn.org.autumn.table.mysql.ColumnMeta;
import cn.org.autumn.table.mysql.TableMeta;
import cn.org.autumn.table.platform.RelationalTableOperations;
import org.apache.commons.lang.StringUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Oracle / SQL Server：基于 JDBC {@link DatabaseMetaData} 的元数据与 {@code DROP TABLE}；
 * 注解建表（CREATE/ALTER 等）未实现，与 {@link DatabaseType#supportsAnnotationTableSync()} 为 false 配合使用。
 */
public abstract class AbstractJdbcVendorRelationalTableOperations implements RelationalTableOperations {

    private final DataSource dataSource;

    protected AbstractJdbcVendorRelationalTableOperations(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    protected abstract String quoteTable(String rawName);

    protected Connection open() throws SQLException {
        return dataSource.getConnection();
    }

    @Override
    public boolean hasTable(String tableName) {
        if (StringUtils.isBlank(tableName)) {
            return false;
        }
        try (Connection c = open()) {
            DatabaseMetaData md = c.getMetaData();
            String catalog = c.getCatalog();
            String schema = c.getSchema();
            if (tryHasTable(md, catalog, schema, tableName)) {
                return true;
            }
            if (tryHasTable(md, catalog, schema, tableName.toUpperCase(Locale.ROOT))) {
                return true;
            }
            if (tryHasTable(md, catalog, null, tableName)) {
                return true;
            }
            if (tryHasTable(md, null, null, tableName)) {
                return true;
            }
        } catch (SQLException e) {
            throw new RuntimeException("hasTable failed: " + e.getMessage(), e);
        }
        return false;
    }

    private boolean tryHasTable(DatabaseMetaData md, String catalog, String schema, String tableName) throws SQLException {
        try (ResultSet rs = md.getTables(catalog, schema, tableName, new String[]{"TABLE"})) {
            return rs.next();
        }
    }

    @Override
    public List<ColumnMeta> getColumnMetas(String tableName) {
        List<ColumnMeta> out = new ArrayList<>();
        if (StringUtils.isBlank(tableName)) {
            return out;
        }
        try (Connection c = open()) {
            DatabaseMetaData md = c.getMetaData();
            String catalog = c.getCatalog();
            String schema = c.getSchema();
            readColumns(md, catalog, schema, tableName, out);
            if (out.isEmpty()) {
                readColumns(md, catalog, schema, tableName.toUpperCase(Locale.ROOT), out);
            }
        } catch (SQLException e) {
            throw new RuntimeException("getColumnMetas failed: " + e.getMessage(), e);
        }
        return out;
    }

    private void readColumns(DatabaseMetaData md, String catalog, String schema, String tableName, List<ColumnMeta> out)
            throws SQLException {
        try (ResultSet rs = md.getColumns(catalog, schema, tableName, null)) {
            while (rs.next()) {
                ColumnMeta m = new ColumnMeta();
                m.setTableCatalog(rs.getString("TABLE_CAT"));
                m.setTableSchema(rs.getString("TABLE_SCHEM"));
                m.setTableName(rs.getString("TABLE_NAME"));
                m.setColumnName(rs.getString("COLUMN_NAME"));
                int dataType = rs.getInt("DATA_TYPE");
                m.setDataType(typeName(dataType, rs.getString("TYPE_NAME")));
                m.setColumnType(rs.getString("TYPE_NAME"));
                long sz = rs.getLong("COLUMN_SIZE");
                if (!rs.wasNull()) {
                    m.setCharacterMaximumLength(sz);
                }
                m.setIsNullable(rs.getString("IS_NULLABLE"));
                m.setColumnDefault(rs.getString("COLUMN_DEF"));
                out.add(m);
            }
        }
    }

    private static String typeName(int dataType, String typeName) {
        if (typeName != null && !typeName.isEmpty()) {
            return typeName.toLowerCase(Locale.ROOT);
        }
        return String.valueOf(dataType);
    }

    @Override
    public List<TableMeta> getTableMetas(String tableName, int offset, int rows) {
        List<TableMeta> all = getTableMetas(tableName);
        if (offset <= 0 && (rows <= 0 || rows >= all.size())) {
            return all;
        }
        int from = Math.max(0, offset);
        int to = rows > 0 ? Math.min(all.size(), from + rows) : all.size();
        if (from >= all.size()) {
            return Collections.emptyList();
        }
        return new ArrayList<>(all.subList(from, to));
    }

    @Override
    public List<TableMeta> getTableMetas(String tableName) {
        List<TableMeta> out = new ArrayList<>();
        if (StringUtils.isBlank(tableName)) {
            return out;
        }
        try (Connection c = open()) {
            DatabaseMetaData md = c.getMetaData();
            String catalog = c.getCatalog();
            String schema = c.getSchema();
            try (ResultSet rs = md.getTables(catalog, schema, tableName, new String[]{"TABLE"})) {
                while (rs.next()) {
                    TableMeta t = new TableMeta();
                    t.setTableCatalog(rs.getString("TABLE_CAT"));
                    t.setTableSchema(rs.getString("TABLE_SCHEM"));
                    t.setTableName(rs.getString("TABLE_NAME"));
                    t.setTableType(rs.getString("TABLE_TYPE"));
                    String remark = null;
                    try {
                        remark = rs.getString("REMARKS");
                    } catch (SQLException ignored) {
                    }
                    t.setTableComment(remark);
                    out.add(t);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("getTableMetas failed: " + e.getMessage(), e);
        }
        return out;
    }

    @Override
    public List<UniqueKeyInfo> getTableKeys(String tableName) {
        return new ArrayList<>();
    }

    @Override
    public List<IndexInfo> getTableIndex(String tableName) {
        return new ArrayList<>();
    }

    @Override
    public Integer getTableCount() {
        try (Connection c = open()) {
            DatabaseMetaData md = c.getMetaData();
            String catalog = c.getCatalog();
            String schema = c.getSchema();
            int n = 0;
            try (ResultSet rs = md.getTables(catalog, schema, "%", new String[]{"TABLE"})) {
                while (rs.next()) {
                    n++;
                }
            }
            return n;
        } catch (SQLException e) {
            throw new RuntimeException("getTableCount failed: " + e.getMessage(), e);
        }
    }

    @Override
    public String getTableCharacterSetName(String tableName) {
        return null;
    }

    @Override
    public void convertTableCharset(String tableName, String charset, String collation) {
        // Oracle / SQL Server 无 MySQL 风格表级 CONVERT
    }

    @Override
    public void dropTable(String tableName) {
        if (StringUtils.isBlank(tableName)) {
            return;
        }
        String sql = "DROP TABLE " + quoteTable(tableName);
        try (Connection c = open(); Statement st = c.createStatement()) {
            st.execute(sql);
        } catch (SQLException e) {
            throw new RuntimeException("dropTable failed: " + e.getMessage(), e);
        }
    }

    @Override
    public void createTable(Map<TableInfo, List<Object>> map) {
        unsupportedDdl();
    }

    @Override
    public void addColumns(Map<TableInfo, Object> map) {
        unsupportedDdl();
    }

    @Override
    public void modifyColumn(Map<TableInfo, Object> map) {
        unsupportedDdl();
    }

    @Override
    public void dropColumn(Map<TableInfo, Object> map) {
        unsupportedDdl();
    }

    @Override
    public void dropPrimaryKey(Map<TableInfo, Object> map) {
        unsupportedDdl();
    }

    @Override
    public void dropIndex(Map<TableInfo, Object> map) {
        unsupportedDdl();
    }

    @Override
    public void addIndex(Map<TableInfo, Object> map) {
        unsupportedDdl();
    }

    private void unsupportedDdl() {
        throw new UnsupportedOperationException(
                "当前库类型未接入注解建表 DDL，请使用 Flyway/Liquibase/手工脚本；参见 DatabaseType.supportsAnnotationTableSync()");
    }
}
