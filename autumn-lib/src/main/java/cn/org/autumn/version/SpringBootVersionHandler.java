package cn.org.autumn.version;

import cn.org.autumn.config.VersionHandler;
import org.springframework.boot.SpringApplication;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Spring Boot 版本
 */
@Order(3)
@Component
public class SpringBootVersionHandler implements VersionHandler {

    @Override
    public String name() {
        return "Spring Boot";
    }

    @Override
    public String version() {
        return version(SpringApplication.class);
    }
}
