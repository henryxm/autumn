package cn.org.autumn.config;

import cn.org.autumn.cluster.HealthHandler;
import cn.org.autumn.cluster.VersionHandler;
import org.springframework.stereotype.Component;

@Component
public class AutumnVersion implements VersionHandler, HealthHandler {

    @Override
    public Object value() {
        return "2.0.0.0";
    }

    @Override
    public String name() {
        return VersionHandler.super.name();
    }

    @Override
    public Object version() {
        return value();
    }
}
