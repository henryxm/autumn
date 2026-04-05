package cn.org.autumn.config;

import cn.org.autumn.database.AutumnDatabaseHolder;
import cn.org.autumn.database.AutumnDatabaseType;
import cn.org.autumn.handler.EnumTypeHandler;
import cn.org.autumn.model.StubMapper;
import cn.org.autumn.service.DefaultMapper;
import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.mybatis.spring.boot.autoconfigure.ConfigurationCustomizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MybatisPlusConfig {

    @Autowired
    private AutumnDatabaseHolder autumnDatabaseHolder;

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        PaginationInnerInterceptor paginationInnerInterceptor = new PaginationInnerInterceptor();
        AutumnDatabaseType t = autumnDatabaseHolder.getType();
        if (t == AutumnDatabaseType.POSTGRESQL) {
            paginationInnerInterceptor.setDbType(DbType.POSTGRE_SQL);
        } else if (t == AutumnDatabaseType.ORACLE) {
            paginationInnerInterceptor.setDbType(DbType.ORACLE);
        } else if (t == AutumnDatabaseType.SQLSERVER) {
            paginationInnerInterceptor.setDbType(DbType.SQL_SERVER);
        } else if (t == AutumnDatabaseType.MARIADB) {
            paginationInnerInterceptor.setDbType(DbType.MARIADB);
        } else {
            paginationInnerInterceptor.setDbType(DbType.MYSQL);
        }
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(paginationInnerInterceptor);
        return interceptor;
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
