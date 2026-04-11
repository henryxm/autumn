package cn.org.autumn.table.platform.jdbc;

import cn.org.autumn.table.data.ColumnInfo;
import cn.org.autumn.table.data.IndexInfo;
import cn.org.autumn.table.data.TableInfo;
import cn.org.autumn.table.data.UniqueKeyInfo;
import cn.org.autumn.table.relational.RelationalSchemaSql;
import cn.org.autumn.table.relational.RoutingRelationalSchemaSql;
import cn.org.autumn.table.relational.model.ColumnMeta;
import cn.org.autumn.table.relational.model.TableMeta;
import cn.org.autumn.table.platform.RelationalTableOperations;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * 基于 JDBC {@link DatabaseMetaData} 的关系库操作：元数据、索引列表、{@code DROP TABLE}，
 * 以及通过 {@link RoutingRelationalSchemaSql} 生成并执行的注解建表 DDL。
 */
public abstract class AbstractJdbcVendorRelationalTableOperations implements RelationalTableOperations {

    private static final Logger log = LoggerFactory.getLogger(AbstractJdbcVendorRelationalTableOperations.class);

    private final DataSource dataSource;

    @Autowired
    private RoutingRelationalSchemaSql routingRelationalSchemaSql;

    protected AbstractJdbcVendorRelationalTableOperations(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    protected DataSource getDataSource() {
        return dataSource;
    }

    protected abstract String quoteTable(String rawName);

    protected Connection open() throws SQLException {
        return dataSource.getConnection();
    }

    /**
     * Derby 等驱动对 {@link Connection#getCatalog()} / {@link Connection#getSchema()} 可能抛出
     * {@link java.sql.SQLFeatureNotSupportedException}，导致元数据入口整体失败。
     */
    private static String safeCatalog(Connection c) {
        try {
            return c.getCatalog();
        } catch (SQLException e) {
            return null;
        }
    }

    private static String safeSchema(Connection c) {
        try {
            return c.getSchema();
        } catch (SQLException e) {
            return null;
        }
    }

    @Override
    public boolean hasTable(String tableName) {
        if (StringUtils.isBlank(tableName)) {
            return false;
        }
        try (Connection c = open()) {
            DatabaseMetaData md = c.getMetaData();
            String catalog = safeCatalog(c);
            String schema = safeSchema(c);
            String upper = tableName.toUpperCase(Locale.ROOT);
            // Derby 等：当前 schema 无效或 catalog/schema 组合不合法时 getTables 可能抛 SQLException（message 可能为 null），
            // 需逐项尝试而非首次失败即中断。
            if (tryHasTableLenient(md, catalog, schema, tableName)) {
                return true;
            }
            if (tryHasTableLenient(md, catalog, schema, upper)) {
                return true;
            }
            if (tryHasTableLenient(md, catalog, null, tableName)) {
                return true;
            }
            if (tryHasTableLenient(md, catalog, null, upper)) {
                return true;
            }
            if (tryHasTableLenient(md, null, schema, tableName)) {
                return true;
            }
            if (tryHasTableLenient(md, null, schema, upper)) {
                return true;
            }
            if (tryHasTableLenient(md, null, null, tableName)) {
                return true;
            }
            if (tryHasTableLenient(md, null, null, upper)) {
                return true;
            }
        } catch (SQLException e) {
            String detail = e.getMessage() != null ? e.getMessage() : e.getClass().getName();
            throw new RuntimeException("hasTable failed: " + detail, e);
        }
        return false;
    }

    private boolean tryHasTableLenient(DatabaseMetaData md, String catalog, String schema, String tableName) {
        try {
            return tryHasTable(md, catalog, schema, tableName);
        } catch (SQLException ex) {
            if (log.isDebugEnabled()) {
                log.debug("getTables skipped: catalog={} schema={} table={} : {}",
                        catalog, schema, tableName, ex.toString());
            }
            return false;
        }
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
            String catalog = safeCatalog(c);
            String schema = safeSchema(c);
            String effective = tableName;
            readColumns(md, catalog, schema, tableName, out);
            if (out.isEmpty()) {
                effective = tableName.toUpperCase(Locale.ROOT);
                readColumns(md, catalog, schema, effective, out);
            }
            enrichPrimaryKeyColumnKeys(md, catalog, schema, effective, out);
        } catch (SQLException e) {
            throw new RuntimeException("getColumnMetas failed: " + e.getMessage(), e);
        }
        return out;
    }

    private void enrichPrimaryKeyColumnKeys(DatabaseMetaData md, String catalog, String schema, String tableName,
                                            List<ColumnMeta> cols) throws SQLException {
        if (cols.isEmpty()) {
            return;
        }
        Set<String> pk = new HashSet<>();
        loadPrimaryKeyColumns(md, catalog, schema, tableName, pk);
        if (pk.isEmpty() && !tableName.equals(tableName.toUpperCase(Locale.ROOT))) {
            loadPrimaryKeyColumns(md, catalog, schema, tableName.toUpperCase(Locale.ROOT), pk);
        }
        for (ColumnMeta m : cols) {
            String cn = m.getColumnName();
            if (cn == null) {
                continue;
            }
            if (pk.contains(cn) || pk.contains(cn.toUpperCase(Locale.ROOT))) {
                m.setColumnKey("PRI");
            }
        }
    }

    private void loadPrimaryKeyColumns(DatabaseMetaData md, String catalog, String schema, String tableName, Set<String> out)
            throws SQLException {
        try (ResultSet rs = md.getPrimaryKeys(catalog, schema, tableName)) {
            while (rs.next()) {
                String col = rs.getString("COLUMN_NAME");
                if (col != null) {
                    out.add(col);
                    out.add(col.toUpperCase(Locale.ROOT));
                }
            }
        }
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
                m.setColumnType(buildColumnTypeLabel(rs));
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

    private static String buildColumnTypeLabel(ResultSet rs) throws SQLException {
        String typeName = rs.getString("TYPE_NAME");
        long sz = rs.getLong("COLUMN_SIZE");
        int dec = rs.getInt("DECIMAL_DIGITS");
        if (typeName == null) {
            return "";
        }
        String t = typeName.toLowerCase(Locale.ROOT);
        if (t.contains("char") || t.contains("text") || t.contains("blob")) {
            if (!rs.wasNull() && sz > 0) {
                return t + "(" + sz + ")";
            }
        }
        if (t.contains("decimal") || t.contains("numeric") || t.contains("number")) {
            if (!rs.wasNull() && sz > 0) {
                if (!rs.wasNull() && dec > 0) {
                    return t + "(" + sz + "," + dec + ")";
                }
                return t + "(" + sz + ")";
            }
        }
        return typeName.toLowerCase(Locale.ROOT);
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
            String catalog = safeCatalog(c);
            String schema = safeSchema(c);
            readTableMetasRows(md, catalog, schema, tableName, out);
            if (out.isEmpty()) {
                readTableMetasRows(md, catalog, schema, tableName.toUpperCase(Locale.ROOT), out);
            }
        } catch (SQLException e) {
            throw new RuntimeException("getTableMetas failed: " + e.getMessage(), e);
        }
        return out;
    }

    private void readTableMetasRows(DatabaseMetaData md, String catalog, String schema, String tableName, List<TableMeta> out)
            throws SQLException {
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
    }

    @Override
    public List<UniqueKeyInfo> getTableKeys(String tableName) {
        return new ArrayList<>();
    }

    @Override
    public List<IndexInfo> getTableIndex(String tableName) {
        List<IndexInfo> out = new ArrayList<>();
        if (StringUtils.isBlank(tableName)) {
            return out;
        }
        try (Connection c = open()) {
            DatabaseMetaData md = c.getMetaData();
            String catalog = safeCatalog(c);
            String schema = safeSchema(c);
            Set<String> pkCols = new HashSet<>();
            loadPrimaryKeyColumns(md, catalog, schema, tableName, pkCols);
            if (pkCols.isEmpty()) {
                loadPrimaryKeyColumns(md, catalog, schema, tableName.toUpperCase(Locale.ROOT), pkCols);
            }
            Map<String, IndexMerge> acc = new LinkedHashMap<>();
            try (ResultSet rs = md.getIndexInfo(catalog, schema, tableName, false, true)) {
                mergeIndexInfo(rs, acc);
            }
            if (acc.isEmpty()) {
                try (ResultSet rs = md.getIndexInfo(catalog, schema, tableName.toUpperCase(Locale.ROOT), false, true)) {
                    mergeIndexInfo(rs, acc);
                }
            }
            for (IndexMerge m : acc.values()) {
                if (m.isUniqueIndexMatchingPk(pkCols)) {
                    continue;
                }
                IndexInfo ii = m.toIndexInfo();
                ii.resolve();
                out.add(ii);
            }
        } catch (SQLException e) {
            throw new RuntimeException("getTableIndex failed: " + e.getMessage(), e);
        }
        return out;
    }

    private static void mergeIndexInfo(ResultSet rs, Map<String, IndexMerge> acc) throws SQLException {
        while (rs.next()) {
            short type = rs.getShort("TYPE");
            if (type == DatabaseMetaData.tableIndexStatistic) {
                continue;
            }
            String idxName = rs.getString("INDEX_NAME");
            if (idxName == null) {
                continue;
            }
            boolean nonUnique = rs.getBoolean("NON_UNIQUE");
            String col = rs.getString("COLUMN_NAME");
            if (col == null) {
                continue;
            }
            short ord = rs.getShort("ORDINAL_POSITION");
            acc.computeIfAbsent(idxName, k -> new IndexMerge(k, nonUnique)).addColumn(ord, col);
        }
    }

    private static final class IndexMerge {
        private final String name;
        private final boolean nonUnique;
        private final TreeMap<Short, String> columns = new TreeMap<>();

        IndexMerge(String name, boolean nonUnique) {
            this.name = name;
            this.nonUnique = nonUnique;
        }

        void addColumn(short ordinal, String col) {
            columns.put(ordinal, col);
        }

        boolean isUniqueIndexMatchingPk(Set<String> pkCols) {
            if (nonUnique || pkCols.isEmpty() || columns.isEmpty()) {
                return false;
            }
            Set<String> ixU = new HashSet<>();
            for (String c : columns.values()) {
                if (c != null) {
                    ixU.add(c.toUpperCase(Locale.ROOT));
                }
            }
            Set<String> pkU = new HashSet<>();
            for (String p : pkCols) {
                if (p != null) {
                    pkU.add(p.toUpperCase(Locale.ROOT));
                }
            }
            return ixU.equals(pkU);
        }

        IndexInfo toIndexInfo() {
            IndexInfo i = new IndexInfo();
            i.setName(name);
            i.setKeyName(name);
            i.setNonUnique(nonUnique ? "1" : "0");
            Map<String, Integer> fields = new LinkedHashMap<>();
            for (String col : columns.values()) {
                fields.put(col, 0);
            }
            i.setFields(fields);
            return i;
        }
    }

    @Override
    public Integer getTableCount() {
        try (Connection c = open()) {
            DatabaseMetaData md = c.getMetaData();
            String catalog = safeCatalog(c);
            String schema = safeSchema(c);
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
        RelationalSchemaSql ddl = routingRelationalSchemaSql.forCurrentDatabase();
        Map<TableInfo, List<ColumnInfo>> typed = castColumnMap(map);
        Map<String, Map<TableInfo, List<ColumnInfo>>> wrap = wrapListParam(typed);
        RelationalDdlScriptExecutor.execute(dataSource, ddl.createTable(wrap));
    }

    @Override
    public void addColumns(Map<TableInfo, Object> map) {
        RelationalSchemaSql ddl = routingRelationalSchemaSql.forCurrentDatabase();
        Map<String, Map<TableInfo, ColumnInfo>> wrap = wrapSingleParam(map);
        RelationalDdlScriptExecutor.execute(dataSource, ddl.addColumns(wrap));
    }

    @Override
    public void modifyColumn(Map<TableInfo, Object> map) {
        RelationalSchemaSql ddl = routingRelationalSchemaSql.forCurrentDatabase();
        Map<String, Map<TableInfo, ColumnInfo>> wrap = wrapSingleParam(map);
        RelationalDdlScriptExecutor.execute(dataSource, ddl.modifyColumn(wrap));
    }

    @Override
    public void dropColumn(Map<TableInfo, Object> map) {
        RelationalSchemaSql ddl = routingRelationalSchemaSql.forCurrentDatabase();
        Map<String, Map<TableInfo, String>> w = new HashMap<>();
        Map<TableInfo, String> inner = new HashMap<>();
        for (Map.Entry<TableInfo, Object> e : map.entrySet()) {
            inner.put(e.getKey(), (String) e.getValue());
        }
        w.put(RelationalSchemaSql.paramName, inner);
        RelationalDdlScriptExecutor.execute(dataSource, ddl.dropColumn(w));
    }

    @Override
    public void dropPrimaryKey(Map<TableInfo, Object> map) {
        RelationalSchemaSql ddl = routingRelationalSchemaSql.forCurrentDatabase();
        Map<String, Map<TableInfo, ColumnInfo>> wrap = wrapSingleParam(map);
        RelationalDdlScriptExecutor.execute(dataSource, ddl.dropPrimaryKey(wrap));
    }

    @Override
    public void dropIndex(Map<TableInfo, Object> map) {
        RelationalSchemaSql ddl = routingRelationalSchemaSql.forCurrentDatabase();
        Map<String, Map<TableInfo, Object>> w = new HashMap<>();
        w.put(RelationalSchemaSql.paramName, map);
        try {
            RelationalDdlScriptExecutor.execute(dataSource, ddl.dropIndex(w));
        } catch (NoSuchFieldException | IllegalAccessException ex) {
            log.debug("dropIndex: {}", ex.getMessage());
        } catch (RuntimeException ex) {
            log.debug("dropIndex: {}", ex.getMessage());
        }
    }

    @Override
    public void addIndex(Map<TableInfo, Object> map) {
        RelationalSchemaSql ddl = routingRelationalSchemaSql.forCurrentDatabase();
        Map<String, Map<TableInfo, IndexInfo>> w = new HashMap<>();
        Map<TableInfo, IndexInfo> inner = new HashMap<>();
        for (Map.Entry<TableInfo, Object> e : map.entrySet()) {
            inner.put(e.getKey(), (IndexInfo) e.getValue());
        }
        w.put(RelationalSchemaSql.paramName, inner);
        RelationalDdlScriptExecutor.execute(dataSource, ddl.addIndex(w));
    }

    private static Map<TableInfo, List<ColumnInfo>> castColumnMap(Map<TableInfo, List<Object>> map) {
        Map<TableInfo, List<ColumnInfo>> out = new HashMap<>();
        for (Map.Entry<TableInfo, List<Object>> e : map.entrySet()) {
            List<ColumnInfo> cols = new ArrayList<>();
            for (Object o : e.getValue()) {
                cols.add((ColumnInfo) o);
            }
            out.put(e.getKey(), cols);
        }
        return out;
    }

    private static Map<String, Map<TableInfo, List<ColumnInfo>>> wrapListParam(Map<TableInfo, List<ColumnInfo>> typed) {
        Map<String, Map<TableInfo, List<ColumnInfo>>> w = new HashMap<>();
        w.put(RelationalSchemaSql.paramName, typed);
        return w;
    }

    private static Map<String, Map<TableInfo, ColumnInfo>> wrapSingleParam(Map<TableInfo, Object> map) {
        Map<TableInfo, ColumnInfo> inner = new HashMap<>();
        for (Map.Entry<TableInfo, Object> e : map.entrySet()) {
            inner.put(e.getKey(), (ColumnInfo) e.getValue());
        }
        Map<String, Map<TableInfo, ColumnInfo>> w = new HashMap<>();
        w.put(RelationalSchemaSql.paramName, inner);
        return w;
    }
}
