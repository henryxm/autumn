package cn.org.autumn.database;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 持有当前配置的 {@link AutumnDatabaseType}，供路由数据源、分页方言、建表适配等使用。
 */
@Component
public class AutumnDatabaseHolder {

    @Value("${autumn.database:mysql}")
    private String databaseRaw;

    public AutumnDatabaseType getType() {
        return AutumnDatabaseType.fromConfig(databaseRaw);
    }
}
