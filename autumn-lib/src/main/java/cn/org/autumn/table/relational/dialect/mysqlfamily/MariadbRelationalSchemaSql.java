package cn.org.autumn.table.relational.dialect.mysqlfamily;

import cn.org.autumn.table.relational.dialect.mysql.MysqlSchemaSql;
import cn.org.autumn.table.relational.support.AbstractDelegatingRelationalSchemaSql;

/**
 * MariaDB：与 {@link MysqlSchemaSql} 同构。
 */
public final class MariadbRelationalSchemaSql extends AbstractDelegatingRelationalSchemaSql {

    public static final MariadbRelationalSchemaSql INSTANCE = new MariadbRelationalSchemaSql();

    public MariadbRelationalSchemaSql() {
        super(MysqlSchemaSql.INSTANCE);
    }
}
