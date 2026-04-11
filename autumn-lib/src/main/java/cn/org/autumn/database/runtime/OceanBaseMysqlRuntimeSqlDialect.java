package cn.org.autumn.database.runtime;

import org.springframework.stereotype.Component;

/**
 * OceanBase <b>MySQL 兼容模式</b>：官方 OceanBase Connector/J 为 {@code jdbc:oceanbase://host:port/db}；亦常用 {@code jdbc:mysql://} 连接 MySQL 租户（如 2881 端口）。
 * 语法与 MySQL 方言一致；分页使用 PageHelper {@code mysql}。
 */
@Component
public class OceanBaseMysqlRuntimeSqlDialect extends MysqlRuntimeSqlDialect {
}
