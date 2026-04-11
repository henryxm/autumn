package cn.org.autumn.database.runtime;

/**
 * 与 {@link RuntimeSqlDialect#columnInWrapper(String)} 一致（默认即 {@link RuntimeSqlDialect#quote(String)}），
 * 供未继承 {@link cn.org.autumn.service.DialectService} 的 {@link com.baomidou.mybatisplus.service.impl.ServiceImpl}
 * 子类在 {@link com.baomidou.mybatisplus.mapper.EntityWrapper} 中拼接列名。
 * <p>
 * <b>跨库语义</b>：由 {@link RuntimeSqlDialectRegistry#get()} 按当前数据源解析方言——Derby/DB2/H2/SQLite 等需双引号以免标识符被折成大写；
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
}
