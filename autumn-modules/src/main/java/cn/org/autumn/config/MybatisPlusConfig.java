package cn.org.autumn.config;

import cn.org.autumn.database.AutumnDatabaseHolder;
import cn.org.autumn.database.AutumnDatabaseType;
import cn.org.autumn.model.StubMapper;
import cn.org.autumn.service.DefaultMapper;
import com.baomidou.mybatisplus.plugins.PaginationInterceptor;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeHandlerRegistry;
import org.mybatis.spring.boot.autoconfigure.ConfigurationCustomizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MybatisPlusConfig {

    @Autowired
    private AutumnDatabaseHolder autumnDatabaseHolder;

    /**
     * 与 {@link BooleanNumericTypeHandler} 单例绑定 {@link AutumnDatabaseHolder}：
     * PostgreSQL 走 {@code setBoolean} 适配原生 boolean 列；其它库走 {@code setInt(0/1)} 适配整型列。
     * <p>
     * 另见 {@link BooleanNumericParameterInterceptor}：替换已解析进 {@link org.apache.ibatis.mapping.ParameterMapping} 的
     * {@link org.apache.ibatis.type.BooleanTypeHandler}。
     */
    @Bean
    public BooleanNumericTypeHandler booleanNumericTypeHandler(AutumnDatabaseHolder autumnDatabaseHolder) {
        return new BooleanNumericTypeHandler(autumnDatabaseHolder);
    }

    @Bean
    public ConfigurationCustomizer booleanNumericTypeHandlerCustomizer(BooleanNumericTypeHandler handler) {
        return configuration -> {
            TypeHandlerRegistry registry = configuration.getTypeHandlerRegistry();
            JdbcType[] jdbcTypes = {
                    JdbcType.TINYINT, JdbcType.SMALLINT, JdbcType.INTEGER, JdbcType.BIGINT, JdbcType.BIT,
                    JdbcType.BOOLEAN
            };
            for (JdbcType jdbcType : jdbcTypes) {
                registry.register(Boolean.class, jdbcType, handler);
                registry.register(boolean.class, jdbcType, handler);
            }
            registry.register(Boolean.class, handler);
            registry.register(boolean.class, handler);
            registry.register(JdbcType.BOOLEAN, handler);
            registry.register(JdbcType.BIT, handler);
        };
    }

    /**
     * 由 Spring Boot MyBatis 自动挂到 SqlSessionFactory（见 mybatis-spring-boot-starter）。
     */
    @Bean
    public Interceptor booleanNumericParameterInterceptor(BooleanNumericTypeHandler handler) {
        return new BooleanNumericParameterInterceptor(handler);
    }

    @Bean
    public PaginationInterceptor paginationInterceptor() {
        PaginationInterceptor p = new PaginationInterceptor();
        AutumnDatabaseType t = autumnDatabaseHolder.getType();
        if (t == AutumnDatabaseType.POSTGRESQL) {
            p.setDialectType("postgresql");
        } else if (t == AutumnDatabaseType.ORACLE) {
            p.setDialectType("oracle");
        } else if (t == AutumnDatabaseType.SQLSERVER) {
            p.setDialectType("sqlserver");
        } else if (t == AutumnDatabaseType.MARIADB) {
            p.setDialectType("mariadb");
        } else {
            p.setDialectType("mysql");
        }
        return p;
    }

    @Bean
    public DefaultMapper defaultMapper() {
        return new StubMapper();
    }
}
