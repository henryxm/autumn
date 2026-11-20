package cn.org.autumn.database.runtime.query;

/**
 * 启动同步前贡献 Query→Index 计划。实现类通常为 {@code *DaoSql}：
 * 在 {@link #contributeQueryIndexes()} 中调用与 Provider 相同的 {@link IndexedSelect} 链
 * （或直接调用无参 {@code String} Provider 方法）以触发 {@link QueryIndexRegistry#register}。
 */
public interface QueryIndexContributor {

    void contributeQueryIndexes();
}
