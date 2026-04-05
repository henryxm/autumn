package cn.org.autumn.database.runtime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

@Component
public class RuntimeSqlDialectBootstrap {

    @Autowired
    private RoutingRuntimeSqlDialect routingRuntimeSqlDialect;

    @PostConstruct
    public void install() {
        RuntimeSqlDialectRegistry.set(routingRuntimeSqlDialect);
    }
}
