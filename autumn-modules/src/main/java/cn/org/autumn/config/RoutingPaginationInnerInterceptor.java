package cn.org.autumn.config;

import cn.org.autumn.database.DatabaseHolder;
import cn.org.autumn.database.DatabaseType;
import cn.org.autumn.database.H2EmbeddedMysqlDialect;
import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.springframework.core.env.Environment;

import java.sql.SQLException;

/**
 * 使 MyBatis-Plus 3.x 分页 {@link DbType} 随当前线程数据源（{@link DatabaseHolder#resolveTypeForCurrentRouting()}）变化；
 * {@link PaginationInnerInterceptor} 为单例，{@code dbType} 为可变字段，故在 {@link #willDoQuery}、{@link #beforeQuery} 上与
 * {@code super} 调用同锁串行化（与历史 2.x {@code ThreadLocalPaginationInterceptor} 语义一致）。
 * <p>
 * <b>吞吐</b>：锁会序列化命中分页插件的语句准备路径；异构多源高并发时若成瓶颈，需另行评估（例如 fork 插件在解析处读 ThreadLocal）。
 */
public class RoutingPaginationInnerInterceptor extends PaginationInnerInterceptor {

    private final DatabaseHolder databaseHolder;

    private final Environment environment;

    private final Object dialectGuard = new Object();

    public RoutingPaginationInnerInterceptor(DatabaseHolder databaseHolder, Environment environment) {
        super();
        this.databaseHolder = databaseHolder;
        this.environment = environment;
        synchronized (dialectGuard) {
            applyPrimaryDbType();
        }
    }

    private void applyPrimaryDbType() {
        String url = environment != null ? DatabaseHolder.readPrimaryJdbcUrl(environment) : "";
        if (H2EmbeddedMysqlDialect.isActive(url)) {
            setDbType(DbType.H2);
            return;
        }
        setDbType(toMpDbType(databaseHolder.getType()));
    }

    private void refreshDbTypeForRouting() {
        Environment env = environment != null ? environment : databaseHolder.getEnvironment();
        if (env == null) {
            applyPrimaryDbType();
            return;
        }
        String routingUrl = DatabaseHolder.readCurrentRoutingJdbcUrl(env);
        if (H2EmbeddedMysqlDialect.isActive(routingUrl)) {
            setDbType(DbType.H2);
            return;
        }
        setDbType(toMpDbType(databaseHolder.resolveTypeForCurrentRouting()));
    }

    private static DbType toMpDbType(DatabaseType t) {
        if (t == DatabaseType.POSTGRESQL || t == DatabaseType.KINGBASE) {
            return DbType.POSTGRE_SQL;
        }
        if (t == DatabaseType.ORACLE || t == DatabaseType.OCEANBASE_ORACLE || t == DatabaseType.DAMENG) {
            return DbType.ORACLE;
        }
        if (t == DatabaseType.SQLSERVER) {
            return DbType.SQL_SERVER;
        }
        if (t == DatabaseType.DERBY) {
            return DbType.DERBY;
        }
        if (t == DatabaseType.DB2) {
            return DbType.DB2;
        }
        if (t == DatabaseType.SQLITE) {
            return DbType.SQLITE;
        }
        if (t == DatabaseType.H2 || t == DatabaseType.HSQLDB) {
            return DbType.H2;
        }
        if (t == DatabaseType.MARIADB) {
            return DbType.MARIADB;
        }
        if (t == DatabaseType.TIDB || t == DatabaseType.OCEANBASE_MYSQL) {
            return DbType.MYSQL;
        }
        if (t == DatabaseType.FIREBIRD) {
            return DbType.FIREBIRD;
        }
        if (t == DatabaseType.INFORMIX) {
            return DbType.INFORMIX;
        }
        return DbType.MYSQL;
    }

    @Override
    public boolean willDoQuery(Executor executor, MappedStatement ms, Object parameter, RowBounds rowBounds,
                                 ResultHandler resultHandler, BoundSql boundSql) throws SQLException {
        synchronized (dialectGuard) {
            refreshDbTypeForRouting();
            return super.willDoQuery(executor, ms, parameter, rowBounds, resultHandler, boundSql);
        }
    }

    @Override
    public void beforeQuery(Executor executor, MappedStatement ms, Object parameter, RowBounds rowBounds,
                            ResultHandler resultHandler, BoundSql boundSql) {
        synchronized (dialectGuard) {
            refreshDbTypeForRouting();
            super.beforeQuery(executor, ms, parameter, rowBounds, resultHandler, boundSql);
        }
    }
}
