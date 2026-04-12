package cn.org.autumn.table.relational;

import cn.org.autumn.database.DatabaseHolder;
import cn.org.autumn.database.DatabaseType;
import cn.org.autumn.table.relational.dialect.mysql.H2MysqlCompatSchemaSql;
import cn.org.autumn.table.relational.dialect.mysql.MysqlSchemaSql;
import cn.org.autumn.table.relational.provider.EmbeddedH2MysqlMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Spring 运行期：{@link TableDao} 路径仅在 MySQL 协议族上启用；内嵌 H2 且<strong>当前路由</strong> URL 声明 {@code MODE=MySQL} 时走
 * {@link H2MysqlCompatSchemaSql}。
 * <p>
 * 其它 {@link DatabaseType} 的完整 {@link RelationalSchemaSql} 见 {@link RelationalSchemaSqlCatalog}（如 {@code PostgresTableDao} 直连 PG 实现类）。
 */
@Primary
@Component
public class RoutingRelationalSchemaSql {

    @Autowired
    private DatabaseHolder databaseHolder;

    /**
     * 供 {@link RelationalSchemaSqlRegistry#get()} 与 MyBatis {@link cn.org.autumn.table.relational.provider.QuerySql} 使用。
     */
    public RelationalSchemaSql forTableDaoProvider() {
        if (EmbeddedH2MysqlMode.active()) {
            return H2MysqlCompatSchemaSql.INSTANCE;
        }
        DatabaseType t = databaseHolder.resolveTypeForCurrentRouting();
        if (t == DatabaseType.MARIADB || t == DatabaseType.TIDB || t == DatabaseType.OCEANBASE_MYSQL) {
            return RelationalSchemaSqlCatalog.forType(t);
        }
        return MysqlSchemaSql.INSTANCE;
    }

    /**
     * 按当前 {@link DatabaseHolder} 解析任意类型的方言（工具/测试/扩展用）。
     */
    public RelationalSchemaSql forCurrentDatabase() {
        if (EmbeddedH2MysqlMode.active()) {
            return H2MysqlCompatSchemaSql.INSTANCE;
        }
        return RelationalSchemaSqlCatalog.forType(databaseHolder.resolveTypeForCurrentRouting());
    }
}
