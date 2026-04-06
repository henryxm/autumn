package cn.org.autumn.config;

import cn.org.autumn.database.DatabaseHolder;
import org.apache.ibatis.executor.parameter.ParameterHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.scripting.defaults.DefaultParameterHandler;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.BooleanTypeHandler;
import org.apache.ibatis.type.TypeHandler;

import java.lang.reflect.Field;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.Properties;

/**
 * MyBatis 在解析 #{...} 时会把 {@link BooleanTypeHandler} 固化进 {@link ParameterMapping}，
 * 后续 {@link org.apache.ibatis.scripting.defaults.DefaultParameterHandler} 不再查 {@link org.apache.ibatis.type.TypeHandlerRegistry}，
 * 因此在 {@link ParameterHandler#setParameters} 前把仍使用 {@link BooleanTypeHandler} 的映射替换为
 * 与 Spring 容器一致的 {@link BooleanNumericTypeHandler}（含 {@link DatabaseHolder}，以区分 PG boolean 列与 MySQL 整型列）。
 */
@Intercepts({
        @Signature(type = ParameterHandler.class, method = "setParameters", args = {PreparedStatement.class})
})
public class BooleanNumericParameterInterceptor implements Interceptor {

    private final TypeHandler<?> handler;

    public BooleanNumericParameterInterceptor(BooleanNumericTypeHandler handler) {
        this.handler = handler;
    }

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        Object target = invocation.getTarget();
        if (!(target instanceof DefaultParameterHandler)) {
            return invocation.proceed();
        }
        DefaultParameterHandler dph = (DefaultParameterHandler) target;
        Configuration configuration = readConfiguration(dph);
        BoundSql boundSql = readBoundSql(dph);
        if (configuration == null || boundSql == null) {
            return invocation.proceed();
        }
        List<ParameterMapping> mappings = boundSql.getParameterMappings();
        if (mappings == null || mappings.isEmpty()) {
            return invocation.proceed();
        }
        for (int i = 0; i < mappings.size(); i++) {
            ParameterMapping pm = mappings.get(i);
            TypeHandler<?> th = pm.getTypeHandler();
            if (!(th instanceof BooleanTypeHandler)) {
                continue;
            }
            ParameterMapping rebuilt = new ParameterMapping.Builder(configuration, pm.getProperty(), handler)
                    .javaType(pm.getJavaType())
                    .jdbcType(pm.getJdbcType())
                    .mode(pm.getMode())
                    .numericScale(pm.getNumericScale())
                    .resultMapId(pm.getResultMapId())
                    .jdbcTypeName(pm.getJdbcTypeName())
                    .expression(pm.getExpression())
                    .build();
            mappings.set(i, rebuilt);
        }
        return invocation.proceed();
    }

    private static Configuration readConfiguration(DefaultParameterHandler dph) {
        try {
            Field f = DefaultParameterHandler.class.getDeclaredField("configuration");
            f.setAccessible(true);
            return (Configuration) f.get(dph);
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    private static BoundSql readBoundSql(DefaultParameterHandler dph) {
        try {
            Field f = DefaultParameterHandler.class.getDeclaredField("boundSql");
            f.setAccessible(true);
            return (BoundSql) f.get(dph);
        } catch (ReflectiveOperationException e) {
            return null;
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
