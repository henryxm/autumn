package cn.org.autumn.table.platform.jdbc;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

/**
 * SQLite / H2 / HSQLDB / DB2 / Derby / Firebird / Informix / 达梦 等：标识符双引号，
 * 元数据、索引与 DDL 走 JDBC + {@link cn.org.autumn.table.relational.RoutingRelationalSchemaSql}。
 */
@Component
public class DoubleQuotedJdbcRelationalTableOperations extends AbstractJdbcVendorRelationalTableOperations {

    @Autowired
    public DoubleQuotedJdbcRelationalTableOperations(DataSource dataSource) {
        super(dataSource);
    }

    @Override
    protected String quoteTable(String rawName) {
        if (rawName == null) {
            return "\"\"";
        }
        return "\"" + rawName.replace("\"", "\"\"") + "\"";
    }
}
