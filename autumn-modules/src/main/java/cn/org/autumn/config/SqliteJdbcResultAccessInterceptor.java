package cn.org.autumn.config;

import cn.org.autumn.database.DatabaseHolder;
import cn.org.autumn.database.DatabaseType;
import org.apache.ibatis.executor.resultset.ResultSetHandler;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MyBatis-Plus 2.x 等为 {@link java.util.Date} 生成的映射仍可能走 {@link ResultSet#getTimestamp}，
 * {@link SqliteDateTypeHandler} 在注册顺序上无法覆盖时，本拦截器在 {@link ResultSetHandler} 层包装
 * {@link Statement}，对 TEXT 日期列在 {@code getTimestamp}/{@code getDate}/{@code getTime} 失败时改读
 * {@link ResultSet#getString} 并解析。
 * <p>
 * 仅在 {@link cn.org.autumn.database.DatabaseHolder#getType()} 为 {@link cn.org.autumn.database.DatabaseType#SQLITE} 时生效，
 * 不污染其它方言；因 Holder 表示首源，若 second 为 SQLite 而 first 非 SQLite，本拦截不会对 second 上的结果集生效
 * （与 {@link cn.org.autumn.database.DatabaseHolder} 的「首源方言」模型一致，异构线程级方言需另行设计）。
 */
@Intercepts({
        @Signature(type = ResultSetHandler.class, method = "handleResultSets", args = {Statement.class}),
        @Signature(type = ResultSetHandler.class, method = "handleCursorResultSets", args = {Statement.class}),
})
public class SqliteJdbcResultAccessInterceptor implements Interceptor {

    private static final Map<Statement, Statement> WRAPPED = new ConcurrentHashMap<>();

    private final DatabaseHolder databaseHolder;

    public SqliteJdbcResultAccessInterceptor(DatabaseHolder databaseHolder) {
        this.databaseHolder = databaseHolder;
    }

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        if (databaseHolder == null || databaseHolder.getType() != DatabaseType.SQLITE) {
            return invocation.proceed();
        }
        Object[] args = invocation.getArgs();
        if (args == null || args.length == 0 || !(args[0] instanceof Statement)) {
            return invocation.proceed();
        }
        Statement stmt = (Statement) args[0];
        args[0] = wrapStatement(stmt);
        try {
            return invocation.proceed();
        } finally {
            if (args[0] instanceof Statement) {
                WRAPPED.remove(stmt);
            }
        }
    }

    private static Statement wrapStatement(Statement delegate) {
        if (delegate == null) {
            return null;
        }
        return WRAPPED.computeIfAbsent(delegate, SqliteJdbcResultAccessInterceptor::doWrapStatement);
    }

    private static Statement doWrapStatement(Statement delegate) {
        ClassLoader cl = delegate.getClass().getClassLoader();
        if (delegate instanceof CallableStatement) {
            return (Statement) Proxy.newProxyInstance(cl, new Class<?>[]{CallableStatement.class},
                    new StatementProxyHandler(delegate));
        }
        if (delegate instanceof PreparedStatement) {
            return (Statement) Proxy.newProxyInstance(cl, new Class<?>[]{PreparedStatement.class},
                    new StatementProxyHandler(delegate));
        }
        return (Statement) Proxy.newProxyInstance(cl, new Class<?>[]{Statement.class},
                new StatementProxyHandler(delegate));
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {
        // no-op
    }

    private static final class StatementProxyHandler implements InvocationHandler {

        private final Statement delegate;

        StatementProxyHandler(Statement delegate) {
            this.delegate = delegate;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String name = method.getName();
            if ("getResultSet".equals(name)) {
                ResultSet rs = (ResultSet) method.invoke(delegate, args);
                return rs == null ? null : wrapResultSet(rs);
            }
            if ("executeQuery".equals(name) && method.getDeclaringClass() == PreparedStatement.class) {
                ResultSet rs = (ResultSet) method.invoke(delegate, args);
                return rs == null ? null : wrapResultSet(rs);
            }
            if ("close".equals(name)) {
                try {
                    return method.invoke(delegate, args);
                } finally {
                    WRAPPED.remove(delegate);
                }
            }
            return method.invoke(delegate, args);
        }
    }

    private static ResultSet wrapResultSet(ResultSet delegate) {
        return (ResultSet) Proxy.newProxyInstance(delegate.getClass().getClassLoader(),
                new Class<?>[]{ResultSet.class},
                new ResultSetProxyHandler(delegate));
    }

    private static final class ResultSetProxyHandler implements InvocationHandler {

        private final ResultSet delegate;

        ResultSetProxyHandler(ResultSet delegate) {
            this.delegate = delegate;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String name = method.getName();
            if ("getTimestamp".equals(name) && args != null && args.length >= 1) {
                return getTimestampWithStringFallback(args);
            }
            if ("getDate".equals(name) && args != null && args.length >= 1) {
                return getDateWithStringFallback(args);
            }
            if ("getTime".equals(name) && args != null && args.length >= 1) {
                return getTimeWithStringFallback(args);
            }
            try {
                return method.invoke(delegate, args);
            } catch (InvocationTargetException e) {
                throw e.getTargetException();
            }
        }

        private Timestamp getTimestampWithStringFallback(Object[] args) throws SQLException {
            try {
                if (args.length == 1) {
                    if (args[0] instanceof String) {
                        return delegate.getTimestamp((String) args[0]);
                    }
                    return delegate.getTimestamp((Integer) args[0]);
                }
                if (args[0] instanceof String) {
                    return delegate.getTimestamp((String) args[0], (Calendar) args[1]);
                }
                return delegate.getTimestamp((Integer) args[0], (Calendar) args[1]);
            } catch (SQLException e) {
                if (!SqliteDateTextParseUtil.isTextualTimestampParseFailure(e)) {
                    throw e;
                }
                java.util.Date util = readAsUtilDate(args[0]);
                return util == null ? null : new Timestamp(util.getTime());
            }
        }

        private java.sql.Date getDateWithStringFallback(Object[] args) throws SQLException {
            try {
                if (args.length == 1) {
                    if (args[0] instanceof String) {
                        return delegate.getDate((String) args[0]);
                    }
                    return delegate.getDate((Integer) args[0]);
                }
                if (args[0] instanceof String) {
                    return delegate.getDate((String) args[0], (Calendar) args[1]);
                }
                return delegate.getDate((Integer) args[0], (Calendar) args[1]);
            } catch (SQLException e) {
                if (!SqliteDateTextParseUtil.isTextualTimestampParseFailure(e)) {
                    throw e;
                }
                java.util.Date util = readAsUtilDate(args[0]);
                return util == null ? null : new java.sql.Date(util.getTime());
            }
        }

        private java.sql.Time getTimeWithStringFallback(Object[] args) throws SQLException {
            try {
                if (args.length == 1) {
                    if (args[0] instanceof String) {
                        return delegate.getTime((String) args[0]);
                    }
                    return delegate.getTime((Integer) args[0]);
                }
                if (args[0] instanceof String) {
                    return delegate.getTime((String) args[0], (Calendar) args[1]);
                }
                return delegate.getTime((Integer) args[0], (Calendar) args[1]);
            } catch (SQLException e) {
                if (!SqliteDateTextParseUtil.isTextualTimestampParseFailure(e)) {
                    throw e;
                }
                java.util.Date util = readAsUtilDate(args[0]);
                return util == null ? null : new java.sql.Time(util.getTime());
            }
        }

        private java.util.Date readAsUtilDate(Object col) throws SQLException {
            String s;
            if (col instanceof String) {
                s = delegate.getString((String) col);
            } else {
                s = delegate.getString((Integer) col);
            }
            if (delegate.wasNull()) {
                return null;
            }
            return SqliteDateTextParseUtil.parseToDate(s);
        }
    }
}
