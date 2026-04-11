package cn.org.autumn.table.relational;

import cn.org.autumn.database.DatabaseType;
import cn.org.autumn.table.relational.dialect.db2.Db2RelationalSchemaSql;
import cn.org.autumn.table.relational.dialect.derby.DerbyRelationalSchemaSql;
import cn.org.autumn.table.relational.dialect.firebird.FirebirdRelationalSchemaSql;
import cn.org.autumn.table.relational.dialect.h2.H2NativeRelationalSchemaSql;
import cn.org.autumn.table.relational.dialect.hsqldb.HsqldbRelationalSchemaSql;
import cn.org.autumn.table.relational.dialect.informix.InformixRelationalSchemaSql;
import cn.org.autumn.table.relational.dialect.mysql.H2MysqlCompatSchemaSql;
import cn.org.autumn.table.relational.dialect.mysql.MysqlSchemaSql;
import cn.org.autumn.table.relational.dialect.oceanbase.OceanBaseMysqlRelationalSchemaSql;
import cn.org.autumn.table.relational.dialect.oceanbase.OceanBaseOracleRelationalSchemaSql;
import cn.org.autumn.table.relational.dialect.oracle.DamengRelationalSchemaSql;
import cn.org.autumn.table.relational.dialect.oracle.OracleRelationalSchemaSql;
import cn.org.autumn.table.relational.dialect.postgresql.KingbaseRelationalSchemaSql;
import cn.org.autumn.table.relational.dialect.postgresql.PostgresRelationalSchemaSql;
import cn.org.autumn.table.relational.dialect.sqlserver.SqlServerRelationalSchemaSql;
import cn.org.autumn.table.relational.dialect.sqlite.SqliteRelationalSchemaSql;
import cn.org.autumn.table.relational.dialect.mysqlfamily.MariadbRelationalSchemaSql;
import cn.org.autumn.table.relational.dialect.mysqlfamily.TidbRelationalSchemaSql;

/**
 * 按 {@link DatabaseType} 解析 {@link RelationalSchemaSql} 实现（与 {@link cn.org.autumn.database.DatabaseHolder#resolveType} 语义对齐）。
 * <p>
 * 内嵌 H2 且 JDBC URL 含 {@code MODE=MySQL} 时，{@link cn.org.autumn.database.DatabaseHolder#resolveType} 会将类型视为 {@link DatabaseType#MYSQL}，
 * {@link cn.org.autumn.table.relational.provider.EmbeddedH2MysqlMode} 与 {@link RoutingRelationalSchemaSql} 据此选用 {@link H2MysqlCompatSchemaSql}；
 * 纯 H2（无 MySQL 模式）仍映射为本目录中的 {@link DatabaseType#H2}。
 */
public final class RelationalSchemaSqlCatalog {

    public static RelationalSchemaSql forType(DatabaseType type) {
        if (type == null) {
            return MysqlSchemaSql.INSTANCE;
        }
        switch (type) {
            case POSTGRESQL:
                return PostgresRelationalSchemaSql.INSTANCE;
            case KINGBASE:
                return KingbaseRelationalSchemaSql.INSTANCE;
            case MYSQL:
                return MysqlSchemaSql.INSTANCE;
            case MARIADB:
                return MariadbRelationalSchemaSql.INSTANCE;
            case TIDB:
                return TidbRelationalSchemaSql.INSTANCE;
            case OCEANBASE_MYSQL:
                return OceanBaseMysqlRelationalSchemaSql.INSTANCE;
            case ORACLE:
                return OracleRelationalSchemaSql.INSTANCE;
            case OCEANBASE_ORACLE:
                return OceanBaseOracleRelationalSchemaSql.INSTANCE;
            case DAMENG:
                return DamengRelationalSchemaSql.INSTANCE;
            case SQLSERVER:
                return SqlServerRelationalSchemaSql.INSTANCE;
            case SQLITE:
                return SqliteRelationalSchemaSql.INSTANCE;
            case H2:
                return H2NativeRelationalSchemaSql.INSTANCE;
            case HSQLDB:
                return HsqldbRelationalSchemaSql.INSTANCE;
            case DB2:
                return Db2RelationalSchemaSql.INSTANCE;
            case DERBY:
                return DerbyRelationalSchemaSql.INSTANCE;
            case FIREBIRD:
                return FirebirdRelationalSchemaSql.INSTANCE;
            case INFORMIX:
                return InformixRelationalSchemaSql.INSTANCE;
            case OTHER:
            default:
                return MysqlSchemaSql.INSTANCE;
        }
    }
}
