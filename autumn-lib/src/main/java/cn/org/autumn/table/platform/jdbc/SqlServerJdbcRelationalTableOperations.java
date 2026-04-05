package cn.org.autumn.table.platform.jdbc;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

/**
 * SQL Server：元数据/DROP 走 JDBC；注解同步未开放。
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
}
