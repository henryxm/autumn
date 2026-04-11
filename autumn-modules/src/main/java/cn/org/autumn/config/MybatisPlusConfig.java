package cn.org.autumn.config;

import cn.org.autumn.database.DatabaseHolder;
import cn.org.autumn.model.StubMapper;
import cn.org.autumn.service.DefaultMapper;
import com.baomidou.mybatisplus.entity.GlobalConfiguration;
import com.baomidou.mybatisplus.plugins.PaginationInterceptor;
import com.baomidou.mybatisplus.toolkit.GlobalConfigUtils;
import org.apache.commons.lang.StringUtils;
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
     * Derby 将未加引号的标识符折成大写；注解 DDL 使用双引号小写列名。MP 2.x 仅在
     * {@link com.baomidou.mybatisplus.enums.DBType#POSTGRE}（{@code setDbType("postgresql")}）时对所有列走
     * {@link com.baomidou.mybatisplus.toolkit.SqlReservedWords#convert} 加引号；若用 {@code db2} 则只转义保留字，
     * {@code param_key} 会变成 {@code PARAM_KEY} 与库表不一致。分页方言仍由 {@link PaginationInterceptor#setDialectType}
     * 的 {@link DatabaseHolder} 决定（derby），不受此处影响。
     */
    private static void applyDerbyDbTypeFromEnvironment(Environment environment, org.apache.ibatis.session.Configuration configuration) {
        String url = environment.getProperty("spring.datasource.druid.first.url");
        if (StringUtils.isBlank(url)) {
            url = environment.getProperty("spring.datasource.url");
        }
        if (StringUtils.isBlank(url) || !url.trim().toLowerCase(Locale.ROOT).startsWith("jdbc:derby:")) {
            return;
        }
        GlobalConfiguration gc = GlobalConfigUtils.getGlobalConfig(configuration);
        if (gc != null) {
            gc.setDbType("postgresql");
        }
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

    @Bean
    public PaginationInterceptor paginationInterceptor() {
        PaginationInterceptor p = new PaginationInterceptor();
        p.setDialectType(databaseHolder.getType().pageHelperDialectName());
        return p;
    }

    /**
     * 供 PageHelper 引入的 {@code mybatis-spring-boot-starter} 使用（若其创建的 Configuration 参与合并）。
     */
    @Bean
    public ConfigurationCustomizer derbyMybatisPlusDbTypeCustomizer(Environment environment) {
        return (org.apache.ibatis.session.Configuration configuration) ->
                applyDerbyDbTypeFromEnvironment(environment, configuration);
    }

    /**
     * 供 {@code mybatisplus-spring-boot-starter} 在构建 {@link org.apache.ibatis.session.SqlSessionFactory} 时调用；
     * 与仅注册 {@link ConfigurationCustomizer} 不同，否则 Derby 下 MP 初始化阶段仍会对 {@code jdbc:derby} 打 WARN。
     */
    @Bean
    public com.baomidou.mybatisplus.spring.boot.starter.ConfigurationCustomizer derbyMybatisPlusDbTypeCustomizerForMpStarter(
            Environment environment) {
        return (org.apache.ibatis.session.Configuration configuration) ->
                applyDerbyDbTypeFromEnvironment(environment, configuration);
    }

    @Bean
    public DefaultMapper defaultMapper() {
        return new StubMapper();
    }
}
