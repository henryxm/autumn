package cn.org.autumn.table.relational.dialect.oceanbase;

import cn.org.autumn.table.relational.dialect.oracle.OracleRelationalSchemaSql;
import cn.org.autumn.table.relational.support.AbstractDelegatingRelationalSchemaSql;

/**
 * OceanBase Oracle 兼容模式：与 {@link OracleRelationalSchemaSql} 同构。
 */
public final class OceanBaseOracleRelationalSchemaSql extends AbstractDelegatingRelationalSchemaSql {

    public static final OceanBaseOracleRelationalSchemaSql INSTANCE = new OceanBaseOracleRelationalSchemaSql();

    public OceanBaseOracleRelationalSchemaSql() {
        super(OracleRelationalSchemaSql.INSTANCE);
    }
}
