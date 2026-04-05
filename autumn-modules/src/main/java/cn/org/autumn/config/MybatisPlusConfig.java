package cn.org.autumn.config;

import cn.org.autumn.database.AutumnDatabaseHolder;
import cn.org.autumn.database.AutumnDatabaseType;
import cn.org.autumn.model.StubMapper;
import cn.org.autumn.service.DefaultMapper;
import com.baomidou.mybatisplus.plugins.PaginationInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MybatisPlusConfig {

    @Autowired
    private AutumnDatabaseHolder autumnDatabaseHolder;

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
