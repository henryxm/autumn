package cn.org.autumn.config;

import cn.org.autumn.model.StubMapper;
import cn.org.autumn.service.DefaultMapper;
import com.baomidou.mybatisplus.plugins.PaginationInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MybatisPlusConfig {

    @Bean
    public PaginationInterceptor paginationInterceptor() {
        return new PaginationInterceptor();
    }

    @Bean
    public DefaultMapper defaultMapper() {
        return new StubMapper();
    }
}
