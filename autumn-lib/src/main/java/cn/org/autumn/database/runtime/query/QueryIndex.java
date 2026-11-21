package cn.org.autumn.database.runtime.query;

/**
 * 查询即索引能力标记：由 {@link cn.org.autumn.database.runtime.RuntimeSql} 实现，
 * 业务 {@code *DaoSql} 继承即可。
 * <p>
 * 启动发现见 {@link QueryIndexDiscovery}；索引由方法内 {@link IndexedSelect} 声明，
 * 方法上的空 {@code @Index} 仅作白名单。
 */
public interface QueryIndex {
}
