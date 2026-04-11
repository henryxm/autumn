package cn.org.autumn.table.relational.dialect.oceanbase;

import cn.org.autumn.table.relational.dialect.mysql.MysqlSchemaSql;
import cn.org.autumn.table.relational.support.AbstractDelegatingRelationalSchemaSql;

/**
 * OceanBase MySQL 兼容模式：与 {@link MysqlSchemaSql} 同构。
 */
public final class OceanBaseMysqlRelationalSchemaSql extends AbstractDelegatingRelationalSchemaSql {

    public static final OceanBaseMysqlRelationalSchemaSql INSTANCE = new OceanBaseMysqlRelationalSchemaSql();

    public OceanBaseMysqlRelationalSchemaSql() {
        super(MysqlSchemaSql.INSTANCE);
    }
}
