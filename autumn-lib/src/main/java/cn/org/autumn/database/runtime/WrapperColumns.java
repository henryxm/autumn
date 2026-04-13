package cn.org.autumn.database.runtime;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;

import java.util.Map;

/**
 * 与 {@link RuntimeSqlDialect#columnInWrapper(String)} 一致（默认即 {@link RuntimeSqlDialect#quote(String)}），
 * 供未继承 {@link cn.org.autumn.service.DialectService} 的 {@link com.baomidou.mybatisplus.extension.service.impl.ServiceImpl}
 * 子类在 {@link com.baomidou.mybatisplus.core.conditions.query.QueryWrapper} 等中拼接列名。
 * <p>
 * <b>跨库语义</b>：由 {@link RuntimeSqlDialectRegistry#get()} → {@link RoutingRuntimeSqlDialect} 按<strong>当前路由</strong>
 * JDBC URL（{@link cn.org.autumn.database.DatabaseHolder#readCurrentRoutingJdbcUrl}）解析方言；内嵌 H2+{@code MODE=MySQL} 时运行期引用走
 * H2 双引号（见 {@link RoutingRuntimeSqlDialect}）。MyBatis-Plus 全局列名格式仍以主库为准，见
 * {@link cn.org.autumn.database.DatabaseHolder#getType()} 的类注释。
 * Derby/DB2/H2/SQLite 等需双引号以免标识符被折成大写；
 * MySQL 系为反引号（始终合法）；Oracle 与本项目 {@link cn.org.autumn.table.relational.support.ddl.OracleJdbcDdlGenerator}
 * 的双引号小写列名一致；SQL Server 为方括号。与仅对部分列做 {@code Escape.escape}（MySQL 关键词反引号）相比，
 * 全列按方言引用略增 SQL 长度，对执行计划影响可忽略。
 *
 * @see cn.org.autumn.service.DialectService#columnInWrapper(String)
 */
public final class WrapperColumns {

    private WrapperColumns() {
    }

    public static String columnInWrapper(String logicalColumnName) {
        return RuntimeSqlDialectRegistry.get().columnInWrapper(logicalColumnName);
    }

    /**
     * 替代 {@code BaseMapper#selectByMap} / {@code ServiceImpl#listByMap}：MP 对 Map 键名原样拼进 SQL，不套
     * {@code column-format}；在 Derby / DB2 / H2（双引号小写 DDL）下会变成大写标识符导致列找不到。
     * <p>
     * 仅处理非 null 的 Map 值（与 {@code selectByMap} 常见行为一致）；空 Map 得到无额外条件的 {@link QueryWrapper}，
     * 配合 {@code list(wrapper)} 等价于查全表。
     */
    public static <T> QueryWrapper<T> queryWrapperAllEqQuoted(Map<String, ?> columnEquals) {
        QueryWrapper<T> w = new QueryWrapper<>();
        if (columnEquals == null || columnEquals.isEmpty()) {
            return w;
        }
        for (Map.Entry<String, ?> e : columnEquals.entrySet()) {
            Object v = e.getValue();
            if (v != null) {
                w.eq(columnInWrapper(e.getKey()), v);
            }
        }
        return w;
    }
}
