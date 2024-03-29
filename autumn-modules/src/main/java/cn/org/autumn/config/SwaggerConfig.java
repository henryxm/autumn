package cn.org.autumn.config;

import cn.org.autumn.site.SwaggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import springfox.documentation.oas.annotations.EnableOpenApi;
import springfox.documentation.spring.web.plugins.Docket;

@Configuration
@EnableOpenApi
@DependsOn({"env"})
public class SwaggerConfig {
    @Autowired
    SwaggerFactory swaggerFactory;

    @Bean
    public Docket createRestApi() {
        return swaggerFactory.getSwaggerHandler().getDocket();
    }
}