package cn.org.autumn.database.runtime;

import cn.org.autumn.xss.SQLFilter;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import org.apache.commons.lang.StringUtils;

import java.util.Map;
import java.util.regex.Pattern;

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

    /**
     * 经 {@link SQLFilter#sqlInject(String)} 后的单列名（小写）或与之一致的逻辑列名。
     */
    private static final Pattern SIMPLE_SORT_COLUMN = Pattern.compile("^[a-z_][a-z0-9_]*$");

    private WrapperColumns() {
    }

    public static String columnInWrapper(String logicalColumnName) {
        return RuntimeSqlDialectRegistry.get().columnInWrapper(logicalColumnName);
    }

    /**
     * 供分页 {@code ORDER BY} 使用的列片段：已呈方言引用形态（反引号/双引号/方括号成对）则原样返回；否则在
     * {@code sqlAlreadySanitized==false} 时先做 {@link SQLFilter#sqlInject(String)}；简单标识符则
     * {@link #columnInWrapper(String)}，否则返回过滤后的片段。
     * <p>
     * {@link cn.org.autumn.utils.Query} 中 {@code sidx} 已过滤时应设 {@code sqlAlreadySanitized=true}。
     */
    public static String orderByColumnExpression(String sortColumn, boolean sqlAlreadySanitized) {
        if (StringUtils.isBlank(sortColumn)) {
            return null;
        }
        String trimmed = sortColumn.trim();
        if (looksLikeDialectQuotedColumn(trimmed)) {
            return trimmed;
        }
        String sanitized = sqlAlreadySanitized ? trimmed : SQLFilter.sqlInject(trimmed);
        if (StringUtils.isBlank(sanitized)) {
            return null;
        }
        if (SIMPLE_SORT_COLUMN.matcher(sanitized).matches()) {
            return columnInWrapper(sanitized);
        }
        return sanitized;
    }

    private static boolean looksLikeDialectQuotedColumn(String s) {
        if (s == null || s.length() < 2) {
            return false;
        }
        char a = s.charAt(0);
        char z = s.charAt(s.length() - 1);
        return (a == '`' && z == '`') || (a == '"' && z == '"') || (a == '[' && z == ']');
    }

    /**
     * 替代 {@code BaseMapper#selectByMap} / {@code allEq(Map)} 的裸键：MP 2.x 将 Map 键原样拼进 WHERE，Derby/DB2/H2 等会折成大写列名失效。
     */
    public static <T> EntityWrapper<T> entityWrapperAllEqQuoted(Map<String, ?> columnEquals) {
        EntityWrapper<T> w = new EntityWrapper<T>();
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
