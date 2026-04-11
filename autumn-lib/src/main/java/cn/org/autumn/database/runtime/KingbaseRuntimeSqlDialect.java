package cn.org.autumn.database.runtime;

import org.springframework.stereotype.Component;

/**
 * 人大金仓 KingbaseES：官方 JDBC 为 {@code jdbc:kingbase8:} / {@code jdbc:kingbase86:}（见产品手册）。
 * SQL 与 PostgreSQL 高度兼容：双引号标识符、{@code string_to_array}、布尔字面量 {@code TRUE}；分页与 PageHelper 的 {@code postgresql} 别名一致。
 */
@Component
public class KingbaseRuntimeSqlDialect extends PostgresqlRuntimeSqlDialect {
}
