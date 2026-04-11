package cn.org.autumn.database.runtime;

import org.springframework.stereotype.Component;

/**
 * OceanBase <b>Oracle 兼容模式</b>：官方 {@code jdbc:oceanbase://host:port/schema}，连接参数中可出现 {@code compatibleMode=oracle} / {@code compatible-mode=oracle}（见 OceanBase Connector/J 文档）。
 * 标识符与分页片段与 Oracle 方言一致；PageHelper 使用 {@code oracle}。
 */
@Component
public class OceanBaseOracleRuntimeSqlDialect extends OracleRuntimeSqlDialect {
}
