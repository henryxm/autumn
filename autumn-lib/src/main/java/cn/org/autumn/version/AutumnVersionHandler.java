package cn.org.autumn.version;

import cn.org.autumn.cluster.HealthHandler;
import cn.org.autumn.config.VersionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Autumn 框架版本
 */
@Order(1)
@Component
public class AutumnVersionHandler implements VersionHandler, HealthHandler {
    @Override
    public String value() {
        return Autumn.getVersion();
    }

    @Override
    public String name() {
        return "Autumn";
    }

    @Override
    public String version() {
        return value();
    }
}
