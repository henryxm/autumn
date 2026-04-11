package cn.org.autumn.table.platform.jdbc;

import cn.org.autumn.table.data.TableInfo;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Map;

/**
 * SQL Server：JDBC 元数据 + 方言 DDL；删主键通过 {@code sys.key_constraints} 解析约束名。
 */
@Component
public class SqlServerJdbcRelationalTableOperations extends AbstractJdbcVendorRelationalTableOperations {

    @Autowired
    public SqlServerJdbcRelationalTableOperations(DataSource dataSource) {
        super(dataSource);
    }

    @Override
    protected String quoteTable(String rawName) {
        if (rawName == null) {
            return "[]";
        }
        return "[" + rawName.replace("]", "]]") + "]";
    }

    @Override
    public void dropPrimaryKey(Map<TableInfo, Object> map) {
        for (Map.Entry<TableInfo, Object> e : map.entrySet()) {
            String table = e.getKey().getName();
            if (StringUtils.isBlank(table)) {
                continue;
            }
            String con = findPrimaryKeyConstraintName(table);
            if (StringUtils.isBlank(con)) {
                con = findPrimaryKeyConstraintName(table.toUpperCase(Locale.ROOT));
            }
            if (StringUtils.isNotBlank(con)) {
                String sql = "ALTER TABLE dbo." + quoteTable(table) + " DROP CONSTRAINT " + quoteTable(con);
                RelationalDdlScriptExecutor.execute(getDataSource(), sql);
            }
        }
    }

    private String findPrimaryKeyConstraintName(String tableName) {
        String sql = "SELECT kc.name FROM sys.key_constraints kc "
                + "INNER JOIN sys.tables t ON kc.parent_object_id = t.object_id "
                + "WHERE kc.type = 'PK' AND t.name = ? AND SCHEMA_NAME(t.schema_id) = SCHEMA_NAME()";
        try (Connection cn = getDataSource().getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, tableName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString(1);
                }
            }
        } catch (SQLException e) {
            return null;
        }
        return null;
    }
}
