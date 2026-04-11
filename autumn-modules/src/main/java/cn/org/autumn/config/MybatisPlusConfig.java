package cn.org.autumn.config;

import cn.org.autumn.database.DatabaseHolder;
import cn.org.autumn.database.DatabaseType;
import cn.org.autumn.handler.EnumTypeHandler;
import cn.org.autumn.model.StubMapper;
import cn.org.autumn.service.DefaultMapper;
import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.autoconfigure.MybatisPlusPropertiesCustomizer;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeHandlerRegistry;
import org.mybatis.spring.boot.autoconfigure.ConfigurationCustomizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.Locale;

@Configuration
public class MybatisPlusConfig {

    @Autowired
    private DatabaseHolder databaseHolder;

    /**
     * Derby 不支持 {@code setNull(i, Types.OTHER)}（MyBatis 默认 {@link JdbcType#OTHER}），也不支持
     * {@link JdbcType#NULL}。在 {@code jdbc:derby} 下将 {@code jdbcTypeForNull} 改为 {@link JdbcType#VARCHAR}。
     * <p>
     * 列名双引号与小写 DDL 对齐由 {@link #mybatisPlusColumnFormatCustomizer()} 对 Derby/DB2/SQLite/H2 等设置
     * {@code column-format} 及 {@link GlobalConfig.DbConfig#setDbType(com.baomidou.mybatisplus.annotation.DbType)}（Derby
     * 下与 PostgreSQL 相同引用策略）完成；MyBatis-Plus 3.x 不再使用 MP 2.x 的 {@code SqlInjector} 路径。
     */
    private static void applyDerbyJdbcTypeForNull(Environment environment, org.apache.ibatis.session.Configuration configuration) {
        String url = environment.getProperty("spring.datasource.druid.first.url");
        if (StringUtils.isBlank(url)) {
            url = environment.getProperty("spring.datasource.url");
        }
        if (StringUtils.isBlank(url) || !url.trim().toLowerCase(Locale.ROOT).startsWith("jdbc:derby:")) {
            return;
        }
        configuration.setJdbcTypeForNull(JdbcType.VARCHAR);
    }

    /**
     * 与 {@link BooleanNumericTypeHandler} 单例绑定 {@link DatabaseHolder}：
     * PostgreSQL 走 {@code setBoolean} 适配原生 boolean 列；其它库走 {@code setInt(0/1)} 适配整型列。
     * <p>
     * 另见 {@link BooleanNumericParameterInterceptor}：替换已解析进 {@link org.apache.ibatis.mapping.ParameterMapping} 的
     * {@link org.apache.ibatis.type.BooleanTypeHandler}。
     */
    @Bean
    public BooleanNumericTypeHandler booleanNumericTypeHandler(DatabaseHolder databaseHolder) {
        return new BooleanNumericTypeHandler(databaseHolder);
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

    /**
     * 与 {@code application.yml} 中 {@code mybatis-plus.global-config.db-config.column-format} 配合：
     * YAML 为 MySQL 默认；此处按 {@link DatabaseHolder} 覆盖，避免 PG 等收到反引号。
     * Derby/DB2/SQLite/H2/HSQLDB 与注解双引号小写 DDL 一致，使用双引号并（Derby 时）将 {@code dbType} 设为与 PG 相同的引用策略。
     */
    @Bean
    public MybatisPlusPropertiesCustomizer mybatisPlusColumnFormatCustomizer() {
        return props -> {
            GlobalConfig gc = props.getGlobalConfig();
            if (gc == null) {
                gc = new GlobalConfig();
                props.setGlobalConfig(gc);
            }
            GlobalConfig.DbConfig db = gc.getDbConfig();
            if (db == null) {
                db = new GlobalConfig.DbConfig();
                gc.setDbConfig(db);
            }
            DatabaseType t = databaseHolder.getType();
            if (t == DatabaseType.POSTGRESQL || t == DatabaseType.KINGBASE) {
                db.setColumnFormat("\"%s\"");
            } else if (t == DatabaseType.ORACLE || t == DatabaseType.OCEANBASE_ORACLE || t == DatabaseType.DAMENG) {
                db.setColumnFormat("\"%s\"");
            } else if (t == DatabaseType.SQLSERVER) {
                db.setColumnFormat("[%s]");
            } else if (t == DatabaseType.DERBY || t == DatabaseType.DB2 || t == DatabaseType.SQLITE
                    || t == DatabaseType.H2 || t == DatabaseType.HSQLDB) {
                db.setColumnFormat("\"%s\"");
            } else if (t == DatabaseType.MARIADB || t == DatabaseType.TIDB || t == DatabaseType.OCEANBASE_MYSQL) {
                db.setColumnFormat("`%s`");
            } else {
                db.setColumnFormat("`%s`");
            }
        };
    }

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        PaginationInnerInterceptor paginationInnerInterceptor = new PaginationInnerInterceptor();
        DatabaseType t = databaseHolder.getType();
        if (t == DatabaseType.POSTGRESQL || t == DatabaseType.KINGBASE) {
            paginationInnerInterceptor.setDbType(DbType.POSTGRE_SQL);
        } else if (t == DatabaseType.ORACLE || t == DatabaseType.OCEANBASE_ORACLE || t == DatabaseType.DAMENG) {
            paginationInnerInterceptor.setDbType(DbType.ORACLE);
        } else if (t == DatabaseType.SQLSERVER) {
            paginationInnerInterceptor.setDbType(DbType.SQL_SERVER);
        } else if (t == DatabaseType.DERBY) {
            paginationInnerInterceptor.setDbType(DbType.DERBY);
        } else if (t == DatabaseType.DB2) {
            paginationInnerInterceptor.setDbType(DbType.DB2);
        } else if (t == DatabaseType.SQLITE) {
            paginationInnerInterceptor.setDbType(DbType.SQLITE);
        } else if (t == DatabaseType.H2 || t == DatabaseType.HSQLDB) {
            paginationInnerInterceptor.setDbType(DbType.H2);
        } else if (t == DatabaseType.MARIADB) {
            paginationInnerInterceptor.setDbType(DbType.MARIADB);
        } else if (t == DatabaseType.TIDB) {
            paginationInnerInterceptor.setDbType(DbType.MYSQL);
        } else if (t == DatabaseType.OCEANBASE_MYSQL) {
            paginationInnerInterceptor.setDbType(DbType.MYSQL);
        } else if (t == DatabaseType.FIREBIRD) {
            paginationInnerInterceptor.setDbType(DbType.FIREBIRD);
        } else if (t == DatabaseType.INFORMIX) {
            paginationInnerInterceptor.setDbType(DbType.INFORMIX);
        } else {
            paginationInnerInterceptor.setDbType(DbType.MYSQL);
        }
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(paginationInnerInterceptor);
        return interceptor;
    }

    @Bean
    public ConfigurationCustomizer derbyJdbcTypeForNullCustomizer(Environment environment) {
        return configuration -> applyDerbyJdbcTypeForNull(environment, configuration);
    }

    @Bean
    public DefaultMapper defaultMapper() {
        return new StubMapper();
    }

    @Bean
    public ConfigurationCustomizer mybatisConfigurationCustomizer() {
        return configuration -> configuration.setDefaultEnumTypeHandler(EnumTypeHandler.class);
    }
}
