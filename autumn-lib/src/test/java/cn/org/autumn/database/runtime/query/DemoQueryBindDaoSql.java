package cn.org.autumn.database.runtime.query;

import cn.org.autumn.database.runtime.RuntimeSql;

/**
 * 新业务试点：Query→Index 写法示例（测试夹具，不改写生产 *DaoSql）。
 * 与 {@link QueryIndexMergeTest.DemoBindEntity} 同表；同指纹时覆盖 Entity @Indexes。
 * <p>
 * 实现 {@link QueryIndexContributor} 即可被启动扫描登记（不必再叠 {@link QueryIndexScan}）。
 */
public class DemoQueryBindDaoSql extends RuntimeSql implements QueryIndexContributor {

    @Override
    public void contributeQueryIndexes() {
        getByAuthenticationAndUpper();
        getByAuthenticationAndUser();
    }

    public String getByAuthenticationAndUpper() {
        return select("demo_query_bind")
                .eq("authentication", 32).eq("upper", 32)
                .unique()
                .name("auth_upper")
                .limitOne()
                .sql();
    }

    public String getByAuthenticationAndUser() {
        return select("demo_query_bind")
                .eq("authentication", 32).eq("user", 32)
                .unique()
                .name("auth_user")
                .limitOne()
                .sql();
    }
}
