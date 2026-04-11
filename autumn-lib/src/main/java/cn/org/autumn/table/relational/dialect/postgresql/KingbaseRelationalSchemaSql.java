package cn.org.autumn.table.relational.dialect.postgresql;

import cn.org.autumn.table.relational.support.AbstractDelegatingRelationalSchemaSql;

/**
 * 人大金仓 KingbaseES：PostgreSQL 兼容，与 {@link PostgresRelationalSchemaSql} 同构。
 */
public final class KingbaseRelationalSchemaSql extends AbstractDelegatingRelationalSchemaSql {

    public static final KingbaseRelationalSchemaSql INSTANCE = new KingbaseRelationalSchemaSql();

    public KingbaseRelationalSchemaSql() {
        super(PostgresRelationalSchemaSql.INSTANCE);
    }
}
