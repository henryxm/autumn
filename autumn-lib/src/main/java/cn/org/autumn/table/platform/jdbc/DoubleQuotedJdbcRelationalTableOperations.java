package cn.org.autumn.table.platform.jdbc;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

/**
 * SQLite / H2 / HSQLDB / DB2 / Derby / Firebird / Informix / 达梦 / KingbaseES 等：标识符双引号，
 * 元数据与 {@code DROP TABLE} 走 JDBC；注解建表未开放（与 {@link cn.org.autumn.database.DatabaseType#supportsAnnotationTableSync()} 为 false 配合）。
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
