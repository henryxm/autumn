package cn.org.autumn.table.platform.jdbc;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

/**
 * Oracle：JDBC 元数据 + {@link cn.org.autumn.table.relational.RoutingRelationalSchemaSql} 生成的 DDL。
 */
@Component
public class OracleJdbcRelationalTableOperations extends AbstractJdbcVendorRelationalTableOperations {

    @Autowired
    public OracleJdbcRelationalTableOperations(DataSource dataSource) {
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
