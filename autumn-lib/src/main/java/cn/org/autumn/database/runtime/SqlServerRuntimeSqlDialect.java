package cn.org.autumn.database.runtime;

import org.springframework.stereotype.Component;

/**
 * SQL Server：标识符方括号；逗号列表成员判断对齐 {@code FIND_IN_SET}；取一行需 {@code ORDER BY} 以满足 {@code OFFSET/FETCH}。
 */
@Component
public class SqlServerRuntimeSqlDialect implements RuntimeSqlDialect {

    @Override
    public String quote(String identifier) {
        if (identifier == null) {
            return "[]";
        }
        return "[" + identifier.replace("]", "]]") + "]";
    }

    @Override
    public String limitOne() {
        return " ORDER BY (SELECT NULL) OFFSET 0 ROWS FETCH NEXT 1 ROW ONLY";
    }

    @Override
    public String likeContainsAny(String mybatisParamPlaceholder) {
        return "concat('%', cast(" + mybatisParamPlaceholder + " as varchar(max)), '%')";
    }

    @Override
    public String columnValueInCommaSeparatedList(String qualifiedColumn, String csvInner) {
        String esc = csvInner == null ? "" : csvInner.replace("'", "''");
        return "CHARINDEX(','+LTRIM(RTRIM(CAST(" + qualifiedColumn + " AS VARCHAR(MAX))))+',', ','+'" + esc + "'+',') > 0";
    }
}
