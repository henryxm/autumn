package cn.org.autumn.database.runtime;

import cn.org.autumn.xss.SQLFilter;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * 与 {@link RuntimeSqlDialect#columnInWrapper(String)} 一致（默认即 {@link RuntimeSqlDialect#quote(String)}），
 * 供未继承 {@link cn.org.autumn.service.DialectService} 的 {@link com.baomidou.mybatisplus.extension.service.impl.ServiceImpl}
 * 子类在 {@link com.baomidou.mybatisplus.core.conditions.query.QueryWrapper} 等中拼接列名。
 *
 * @see cn.org.autumn.service.DialectService#columnInWrapper(String)
 */
public final class WrapperColumns {

    /**
     * 仅允许简单标识符作为排序列，防止 ORDER BY 注入（含方言引号旁路）。
     */
    private static final Pattern SIMPLE_SORT_COLUMN = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]*$");

    private WrapperColumns() {
    }

    public static String columnInWrapper(String logicalColumnName) {
        return RuntimeSqlDialectRegistry.get().columnInWrapper(logicalColumnName);
    }

    /**
     * 供 {@code ORDER BY} 使用的列片段：仅允许简单标识符，否则返回 null（调用方丢弃排序）。
     * <p>
     * 不再对「成对引号」原样放行，避免 `` `id`,if(1=1,id,name)` `` 类旁路。
     */
    public static String orderByColumnExpression(String sortColumn, boolean sqlAlreadySanitized) {
        if (StringUtils.isBlank(sortColumn)) {
            return null;
        }
        String trimmed = sortColumn.trim();
        String sanitized = sqlAlreadySanitized ? trimmed : SQLFilter.sqlInject(trimmed);
        if (StringUtils.isBlank(sanitized)) {
            return null;
        }
        // SQLFilter 会转小写；再剥一层可能残留的方言引号后校验标识符
        String ident = stripDialectQuotes(sanitized);
        if (!SIMPLE_SORT_COLUMN.matcher(ident).matches()) {
            return null;
        }
        return columnInWrapper(ident.toLowerCase());
    }

    private static String stripDialectQuotes(String s) {
        if (s == null || s.length() < 2) {
            return s;
        }
        char a = s.charAt(0);
        char z = s.charAt(s.length() - 1);
        if ((a == '`' && z == '`') || (a == '"' && z == '"') || (a == '[' && z == ']')) {
            return s.substring(1, s.length() - 1).trim();
        }
        return s;
    }

    /**
     * 替代 {@code BaseMapper#selectByMap} / {@code ServiceImpl#listByMap}：MP 对 Map 键名原样拼进 SQL，不套
     * {@code column-format}；在 Derby / DB2 / H2（双引号小写 DDL）下会变成大写标识符导致列找不到。
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
