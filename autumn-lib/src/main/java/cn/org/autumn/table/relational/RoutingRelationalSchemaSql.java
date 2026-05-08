package cn.org.autumn.table.relational;

import cn.org.autumn.database.DatabaseHolder;
import cn.org.autumn.database.DatabaseType;
import cn.org.autumn.table.relational.dialect.mysql.H2MysqlCompatSchemaSql;
import cn.org.autumn.table.relational.provider.EmbeddedH2MysqlMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Spring 运行期：{@link TableDao} 路径仅在 MySQL 协议族上启用；内嵌 H2 且<b>当前路由数据源</b>的 JDBC URL 声明
 * {@code MODE=MySQL} 时走 {@link H2MysqlCompatSchemaSql}（与 {@link cn.org.autumn.table.relational.provider.EmbeddedH2MysqlMode} 同源判定）。
 * <p>
 * {@link DatabaseHolder#getType()} 与多数据源线程路由一致时，本类选用的方言与当前连接目标一致
 * （见 {@link cn.org.autumn.datasources.DataSourceDialectRegistry}）。
 * {@link #forTableDaoProvider()} 与 {@link #forCurrentDatabase()} 在 H2 原生等场景须走 {@link RelationalSchemaSqlCatalog}，
 * 禁止回退为 {@link cn.org.autumn.table.relational.dialect.mysql.MysqlSchemaSql}（否则注解建表会向 H2 下发 MySQL 专有 DDL 导致启动失败）。
 */
@Primary
@Component
public class RoutingRelationalSchemaSql {

    @Autowired(required = false)
    private DatabaseHolder databaseHolder;

    /**
     * 供 {@link RelationalSchemaSqlRegistry#get()} 与 MyBatis {@link cn.org.autumn.table.relational.provider.QuerySql} 使用。
     */
    public RelationalSchemaSql forTableDaoProvider() {
        return resolve();
    }

    /**
     * 按当前 {@link DatabaseHolder} 解析任意类型的方言（工具/测试/扩展用）。
     */
    public RelationalSchemaSql forCurrentDatabase() {
        return resolve();
    }

    private RelationalSchemaSql resolve() {
        if (EmbeddedH2MysqlMode.active()) {
            return H2MysqlCompatSchemaSql.INSTANCE;
        }
        DatabaseType type = databaseHolder != null ? databaseHolder.getType() : null;
        return RelationalSchemaSqlCatalog.forType(type);
    }
}
