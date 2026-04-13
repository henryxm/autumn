package cn.org.autumn.config;

import cn.org.autumn.database.DatabaseType;
import com.baomidou.mybatisplus.entity.GlobalConfiguration;

/**
 * 将 MP 2.x {@link GlobalConfiguration} 的 {@code dbType} 与 {@code identifier-quote}
 * 与 Autumn 解析到的 {@link DatabaseType} 对齐；供 {@link MybatisPlusSqlSessionFactoryDialectBeanPostProcessor}
 * 与 {@link AutumnQuotedTableSqlInjector} 共用。
 */
public final class MybatisPlusGlobalDialectAlign {

    private MybatisPlusGlobalDialectAlign() {
    }

    public static void apply(DatabaseType t, GlobalConfiguration gc) {
        if (gc == null || t == null || t == DatabaseType.OTHER) {
            return;
        }
        String mpDb = toMybatisPlusDbTypeName(t);
        if (mpDb != null) {
            gc.setDbType(mpDb);
        }
        String quote = t.mybatisPlusIdentifierQuotePattern();
        if (quote != null) {
            gc.setIdentifierQuote(quote);
        }
    }

    static String toMybatisPlusDbTypeName(DatabaseType type) {
        switch (type) {
            case MYSQL:
            case MARIADB:
            case TIDB:
            case OCEANBASE_MYSQL:
                return "mysql";
            case POSTGRESQL:
            case KINGBASE:
                return "postgresql";
            case ORACLE:
            case OCEANBASE_ORACLE:
            case DAMENG:
                return "oracle";
            case SQLSERVER:
                return "sqlserver";
            case SQLITE:
                return "sqlite";
            case H2:
                return "h2";
            case HSQLDB:
                return "hsql";
            case DB2:
                return "db2";
            case DERBY:
                return "postgresql";
            default:
                return null;
        }
    }
}
