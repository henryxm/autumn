package cn.org.autumn.database;

import java.util.Properties;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;

/**
 * MyBatis 写拦截：{@link Executor#update} 前校验 {@link CrudGuard}。
 * INSERT / UPDATE / DELETE 均走 {@code update}，只拦截此方法即可。
 */
@Intercepts({
        @Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class})
})
public class CrudInterceptor implements Interceptor {

    private final CrudGuard crudGuard;

    public CrudInterceptor(CrudGuard crudGuard) {
        this.crudGuard = crudGuard;
    }

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        MappedStatement ms = (MappedStatement) invocation.getArgs()[0];
        if (write(ms.getSqlCommandType()) && crudGuard != null) {
            crudGuard.enforceWrite();
        }
        return invocation.proceed();
    }

    static boolean write(SqlCommandType type) {
        if (type == null) {
            return true;
        }
        switch (type) {
            case INSERT:
            case UPDATE:
            case DELETE:
            case UNKNOWN:
                return true;
            default:
                return false;
        }
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {
    }
}
