package cn.org.autumn.table.relational.dialect.oracle;

import cn.org.autumn.table.relational.support.AbstractDelegatingRelationalSchemaSql;

/**
 * 达梦 DM：Oracle 兼容语义为主，与 {@link OracleRelationalSchemaSql} 同构（差异可在子类覆盖）。
 */
public final class DamengRelationalSchemaSql extends AbstractDelegatingRelationalSchemaSql {

    public static final DamengRelationalSchemaSql INSTANCE = new DamengRelationalSchemaSql();

    public DamengRelationalSchemaSql() {
        super(OracleRelationalSchemaSql.INSTANCE);
    }
}
