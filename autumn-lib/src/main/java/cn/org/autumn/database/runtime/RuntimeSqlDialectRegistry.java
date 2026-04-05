package cn.org.autumn.database.runtime;

/**
 * 供 MyBatis {@code SqlProvider} 等非 Spring 托管类在启动后取得当前方言（由 {@link RuntimeSqlDialectBootstrap} 注入）。
 */
public final class RuntimeSqlDialectRegistry {

    private static volatile RuntimeSqlDialect dialect = new MysqlRuntimeSqlDialect();

    private RuntimeSqlDialectRegistry() {
    }

    public static RuntimeSqlDialect get() {
        return dialect;
    }

    public static void set(RuntimeSqlDialect d) {
        if (d != null) {
            dialect = d;
        }
    }
}
