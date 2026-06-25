package cn.org.autumn.database.runtime;

import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class RuntimeSqlDialectBootstrap {

    @Autowired
    private RoutingRuntimeSqlDialect routingRuntimeSqlDialect;

    @PostConstruct
    public void install() {
        RuntimeSqlDialectRegistry.set(routingRuntimeSqlDialect);
    }
}
