package cn.org.autumn.table.dao.postgresql;

import cn.org.autumn.table.data.ColumnInfo;
import cn.org.autumn.table.data.IndexInfo;
import cn.org.autumn.table.data.TableInfo;
import cn.org.autumn.table.data.UniqueKeyInfo;
import cn.org.autumn.table.relational.dialect.postgresql.PostgresRelationalSchemaSql;
import cn.org.autumn.table.relational.model.ColumnMeta;
import cn.org.autumn.table.relational.model.TableMeta;
import cn.org.autumn.table.platform.RelationalTableOperations;
import cn.org.autumn.table.platform.jdbc.RelationalDdlStatementSplitter;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * PostgreSQL：元数据走 {@link PostgresTableDao}；DDL 多语句拆分执行（分号不在单引号字符串内截断，见 {@link RelationalDdlStatementSplitter}）。
 */
@Component
public class PostgresRelationalTableOperations implements RelationalTableOperations {

    private static final Logger log = LoggerFactory.getLogger(PostgresRelationalTableOperations.class);

    @Autowired
    private PostgresTableDao postgresTableDao;

    @Autowired
    private DataSource dataSource;

    private final PostgresRelationalSchemaSql ddl = PostgresRelationalSchemaSql.INSTANCE;

    @Override
    public void createTable(Map<TableInfo, List<Object>> map) {
        Map<TableInfo, List<ColumnInfo>> typed = castColumnMap(map);
        Map<String, Map<TableInfo, List<ColumnInfo>>> wrap = new HashMap<>();
        wrap.put(PostgresRelationalSchemaSql.paramName, typed);
        executeScript(ddl.createTable(wrap));
    }

    @Override
    public boolean hasTable(String tableName) {
        Integer n = postgresTableDao.countTable(tableName);
        return n != null && n > 0;
    }

    @Override
    public String getTableCharacterSetName(String tableName) {
        return null;
    }

    @Override
    public void convertTableCharset(String tableName, String charset, String collation) {
        // PostgreSQL 不使用 MySQL 风格表级 CONVERT
    }

    @Override
    public List<ColumnMeta> getColumnMetas(String tableName) {
        List<ColumnMeta> list = postgresTableDao.getColumnMetas(tableName);
        if (list == null) {
            return new ArrayList<>();
        }
        for (ColumnMeta c : list) {
            normalizeColumnMeta(c);
        }
        return list;
    }

    @Override
    public List<TableMeta> getTableMetas(String tableName, int offset, int rows) {
        return postgresTableDao.getTableMetasPage(tableName, offset, rows);
    }

    @Override
    public List<TableMeta> getTableMetas(String tableName) {
        return postgresTableDao.getTableMetas(tableName);
    }

    @Override
    public List<UniqueKeyInfo> getTableKeys(String tableName) {
        List<UniqueKeyInfo> keys = postgresTableDao.getTableKeys(tableName);
        return keys != null ? keys : new ArrayList<>();
    }

    @Override
    public List<IndexInfo> getTableIndex(String tableName) {
        List<IndexInfo> idx = postgresTableDao.getTableIndex(tableName);
        if (idx == null) {
            return new ArrayList<>();
        }
        for (IndexInfo i : idx) {
            i.resolve();
        }
        return idx;
    }

    @Override
    public Integer getTableCount() {
        return postgresTableDao.getTableCount();
    }

    @Override
    public void addColumns(Map<TableInfo, Object> map) {
        Map<TableInfo, ColumnInfo> m = new HashMap<>();
        for (Map.Entry<TableInfo, Object> e : map.entrySet()) {
            m.put(e.getKey(), (ColumnInfo) e.getValue());
        }
        Map<String, Map<TableInfo, ColumnInfo>> wrap = new HashMap<>();
        wrap.put(PostgresRelationalSchemaSql.paramName, m);
        executeScript(ddl.addColumns(wrap));
    }

    @Override
    public void modifyColumn(Map<TableInfo, Object> map) {
        Map<TableInfo, ColumnInfo> m = new HashMap<>();
        for (Map.Entry<TableInfo, Object> e : map.entrySet()) {
            m.put(e.getKey(), (ColumnInfo) e.getValue());
        }
        Map<String, Map<TableInfo, ColumnInfo>> wrap = new HashMap<>();
        wrap.put(PostgresRelationalSchemaSql.paramName, m);
        executeScript(ddl.modifyColumn(wrap));
    }

    @Override
    public void dropColumn(Map<TableInfo, Object> map) {
        Map<TableInfo, String> m = new HashMap<>();
        for (Map.Entry<TableInfo, Object> e : map.entrySet()) {
            m.put(e.getKey(), (String) e.getValue());
        }
        Map<String, Map<TableInfo, String>> wrap = new HashMap<>();
        wrap.put(PostgresRelationalSchemaSql.paramName, m);
        executeScript(ddl.dropColumn(wrap));
    }

    @Override
    public void dropPrimaryKey(Map<TableInfo, Object> map) {
        for (Map.Entry<TableInfo, Object> e : map.entrySet()) {
            String table = e.getKey().getName();
            String con = findPrimaryKeyConstraintName(table);
            if (StringUtils.isBlank(con)) {
                continue;
            }
            String sql = "ALTER TABLE \"" + table.replace("\"", "\"\"") + "\" DROP CONSTRAINT \"" + con.replace("\"", "\"\"") + "\"";
            executeScript(sql);
        }
    }

    @Override
    public void dropIndex(Map<TableInfo, Object> map) {
        Map<String, Map<TableInfo, Object>> wrap = new HashMap<>();
        wrap.put(PostgresRelationalSchemaSql.paramName, map);
        try {
            executeScript(ddl.dropIndex(wrap));
        } catch (Exception ex) {
            log.debug("Drop index: {}", ex.getMessage());
        }
    }

    @Override
    public void dropTable(String tableName) {
        Map<String, String> m = new HashMap<>();
        m.put(PostgresRelationalSchemaSql.paramName, tableName);
        executeScript(ddl.dropTable(m));
    }

    @Override
    public void addIndex(Map<TableInfo, Object> map) {
        Map<String, Map<TableInfo, IndexInfo>> wrap = new HashMap<>();
        Map<TableInfo, IndexInfo> inner = new HashMap<>();
        for (Map.Entry<TableInfo, Object> e : map.entrySet()) {
            inner.put(e.getKey(), (IndexInfo) e.getValue());
        }
        wrap.put(PostgresRelationalSchemaSql.paramName, inner);
        executeScript(ddl.addIndex(wrap));
    }

    private String findPrimaryKeyConstraintName(String tableName) {
        String sql = "SELECT c.conname FROM pg_constraint c "
                + "JOIN pg_class r ON c.conrelid = r.oid "
                + "JOIN pg_namespace n ON r.relnamespace = n.oid "
                + "WHERE n.nspname = current_schema() AND r.relname = ? AND c.contype = 'p' LIMIT 1";
        try (Connection cn = dataSource.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, tableName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString(1);
                }
            }
        } catch (SQLException e) {
            log.debug("findPrimaryKeyConstraintName: {}", e.getMessage());
        }
        return null;
    }

    private void executeScript(String sql) {
        if (StringUtils.isBlank(sql)) {
            return;
        }
        try (Connection cn = dataSource.getConnection(); Statement st = cn.createStatement()) {
            for (String part : RelationalDdlStatementSplitter.split(sql)) {
                String s = part.trim();
                if (s.isEmpty()) {
                    continue;
                }
                st.execute(s);
            }
        } catch (SQLException e) {
            throw new RuntimeException("PostgreSQL DDL failed: " + e.getMessage(), e);
        }
    }

    private Map<TableInfo, List<ColumnInfo>> castColumnMap(Map<TableInfo, List<Object>> map) {
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

    /**
     * 将 information_schema / format_type 结果规整到与实体 {@link ColumnInfo} 中 {@code DataType} 命名接近，减少误判为“类型变更”。
     */
    private void normalizeColumnMeta(ColumnMeta c) {
        if (c == null) {
            return;
        }
        String dt = c.getDataType();
        if (dt != null) {
            String dtl = dt.toLowerCase();
            if ("character varying".equals(dtl)) {
                c.setDataType("varchar");
            } else if ("character".equals(dtl)) {
                c.setDataType("char");
            } else if ("integer".equals(dtl)) {
                c.setDataType("int");
            } else if ("double precision".equals(dtl)) {
                c.setDataType("double");
            } else if ("timestamp without time zone".equals(dtl) || "timestamp with time zone".equals(dtl)) {
                c.setDataType("datetime");
            }
        }
        String ct = c.getColumnType();
        if (ct != null) {
            String n = ct.toLowerCase();
            n = n.replace("character varying", "varchar");
            n = n.replace("timestamp without time zone", "datetime");
            n = n.replace("timestamp with time zone", "datetime");
            c.setColumnType(n);
        }
    }
}
