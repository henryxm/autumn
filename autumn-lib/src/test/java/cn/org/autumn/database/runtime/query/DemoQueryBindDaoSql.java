package cn.org.autumn.database.runtime.query;

import cn.org.autumn.database.runtime.RuntimeSql;
import cn.org.autumn.table.annotation.Index;

/**
 * Query→Index 试点：方法上空 {@link Index} 仅作发现标记；索引由 {@link IndexedSelect} 链声明。
 */
public class DemoQueryBindDaoSql extends RuntimeSql {

    @Index
    public String getByAuthenticationAndUpper() {
        return select("demo_query_bind")
                .eq("authentication", 32).eq("upper", 32)
                .unique().name("auth_upper")
                .limitOne()
                .sql();
    }

    @Index
    public String getByAuthenticationAndUser() {
        return select("demo_query_bind")
                .eq("authentication", 32).eq("user", 32)
                .unique().name("auth_user")
                .limitOne()
                .sql();
    }
}
