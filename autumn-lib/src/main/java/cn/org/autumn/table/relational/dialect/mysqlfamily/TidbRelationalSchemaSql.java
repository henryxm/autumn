package cn.org.autumn.table.relational.dialect.mysqlfamily;

import cn.org.autumn.table.relational.dialect.mysql.MysqlSchemaSql;
import cn.org.autumn.table.relational.support.AbstractDelegatingRelationalSchemaSql;

/**
 * TiDB：MySQL 协议与方言，与 {@link MysqlSchemaSql} 同构。
 */
public final class TidbRelationalSchemaSql extends AbstractDelegatingRelationalSchemaSql {

    public static final TidbRelationalSchemaSql INSTANCE = new TidbRelationalSchemaSql();

    public TidbRelationalSchemaSql() {
        super(MysqlSchemaSql.INSTANCE);
    }
}
