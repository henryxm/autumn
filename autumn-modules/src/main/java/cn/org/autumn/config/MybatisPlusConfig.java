package cn.org.autumn.config;

import cn.org.autumn.database.DatabaseHolder;
import cn.org.autumn.database.DatabaseType;
import cn.org.autumn.database.H2EmbeddedMysqlDialect;
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

    private static String primaryDataSourceUrl(Environment environment) {
        return DatabaseHolder.readPrimaryJdbcUrl(environment);
    }

    /**
     * Derby 不支持 {@code setNull(i, Types.OTHER)}（MyBatis 默认 {@link JdbcType#OTHER}），也不支持
     * {@link JdbcType#NULL}。在 {@code jdbc:derby} 下将 {@code jdbcTypeForNull} 改为 {@link JdbcType#VARCHAR}。
     * <p>
     * 列名双引号与小写 DDL 对齐由 {@link #mybatisPlusColumnFormatCustomizer()} 对 Derby/DB2/SQLite/H2 等设置
     * {@code column-format} 及 {@link GlobalConfig.DbConfig#setDbType(com.baomidou.mybatisplus.annotation.DbType)}（Derby
     * 下与 PostgreSQL 相同引用策略）完成；MyBatis-Plus 3.x 不再使用 MP 2.x 的 {@code SqlInjector} 路径。
     */
    private static void applyDerbyJdbcTypeForNull(Environment environment, org.apache.ibatis.session.Configuration configuration) {
        String url = DatabaseHolder.readPrimaryJdbcUrl(environment);
        if (StringUtils.isBlank(url) || !url.toLowerCase(Locale.ROOT).startsWith("jdbc:derby:")) {
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
     * <p>
     * <b>任意 {@code jdbc:h2:}</b>（含 {@code MODE=MySQL}）：{@link DatabaseHolder} 可能仍解析为 {@link DatabaseType#MYSQL} 以便注解建表；
     * 但 MyBatis-Plus 若按 MySQL 使用反引号（或由 {@link JdbcEnvironmentPostProcessor} 误注入 MYSQL 的 identifier-quote），会在 H2 上触发
     * {@code Column "`id`" not found}。故凡主库为 H2 即强制列名为双引号，与 {@link #mybatisPlusInterceptor(Environment)} 中分页
     * {@link DbType#H2}（{@link H2EmbeddedMysqlDialect#isActive(String)} 分支）一致。
     * <p>
     * <b>表名</b>：Derby 等注解 DDL 使用双引号小写表名；须同步 {@link GlobalConfig.DbConfig#setTableFormat(String)}，
     * 否则 MP 生成无引号的 {@code INSERT INTO sys_config}，Derby 会折叠为 {@code SYS_CONFIG}，与 {@code "sys_config"} 不符。
     */
    @Bean
    public MybatisPlusPropertiesCustomizer mybatisPlusColumnFormatCustomizer(Environment environment) {
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
            if (H2EmbeddedMysqlDialect.isJdbcH2(primaryDataSourceUrl(environment))) {
                applyColumnAndTableQuotes(db, "\"%s\"", "\"%s\"");
                return;
            }
            DatabaseType t = databaseHolder.getType();
            if (t == DatabaseType.POSTGRESQL || t == DatabaseType.KINGBASE) {
                applyColumnAndTableQuotes(db, "\"%s\"", "\"%s\"");
            } else if (t == DatabaseType.ORACLE || t == DatabaseType.OCEANBASE_ORACLE || t == DatabaseType.DAMENG) {
                applyColumnAndTableQuotes(db, "\"%s\"", "\"%s\"");
            } else if (t == DatabaseType.SQLSERVER) {
                applyColumnAndTableQuotes(db, "[%s]", "[%s]");
            } else if (t == DatabaseType.DERBY || t == DatabaseType.DB2 || t == DatabaseType.SQLITE
                    || t == DatabaseType.H2 || t == DatabaseType.HSQLDB) {
                applyColumnAndTableQuotes(db, "\"%s\"", "\"%s\"");
            } else if (t == DatabaseType.FIREBIRD || t == DatabaseType.INFORMIX) {
                applyColumnAndTableQuotes(db, "\"%s\"", "\"%s\"");
            } else if (t == DatabaseType.MARIADB || t == DatabaseType.TIDB || t == DatabaseType.OCEANBASE_MYSQL) {
                applyColumnAndTableQuotes(db, "`%s`", "`%s`");
            } else {
                applyColumnAndTableQuotes(db, "`%s`", "`%s`");
            }
        };
    }

    private static void applyColumnAndTableQuotes(GlobalConfig.DbConfig db, String columnFormat, String tableFormat) {
        db.setColumnFormat(columnFormat);
        db.setTableFormat(tableFormat);
    }

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor(Environment environment) {
        PaginationInnerInterceptor paginationInnerInterceptor = new PaginationInnerInterceptor();
        if (H2EmbeddedMysqlDialect.isActive(primaryDataSourceUrl(environment))) {
            paginationInnerInterceptor.setDbType(DbType.H2);
        } else {
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
