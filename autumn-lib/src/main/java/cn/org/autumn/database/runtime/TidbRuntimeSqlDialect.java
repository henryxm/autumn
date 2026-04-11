package cn.org.autumn.database.runtime;

import org.springframework.stereotype.Component;

/**
 * PingCAP TiDB：官方推荐使用 MySQL 协议与 {@code jdbc:mysql://host:port/db}（见 TiDB 文档）；生态中亦可能出现 {@code jdbc:tidb://}。
 * 标识符与 {@code FIND_IN_SET} 语义与 MySQL 方言一致；分页使用 PageHelper {@code mysql}。
 */
@Component
public class TidbRuntimeSqlDialect extends MysqlRuntimeSqlDialect {
}
