package cn.org.autumn.table.utils;

import java.util.HashSet;
import java.util.Set;

public class Escape {
    /**
     * MySQL 关键词集合（用于快速查找）
     */
    private static final Set<String> keywords = new HashSet<>();

    static {
        // MySQL 关键词列表（常见的关键词）
        String[] keywords = {
                "order", "group", "select", "insert", "update", "delete", "create", "drop", "alter",
                "table", "index", "view", "database", "schema", "user", "password", "key", "value",
                "type", "status", "level", "rank", "desc", "asc", "limit", "offset", "where", "from",
                "join", "inner", "left", "right", "outer", "on", "as", "and", "or", "not", "in", "like",
                "between", "is", "null", "exists", "union", "all", "distinct", "having", "by",
                "case", "when", "then", "else", "end", "if", "elseif", "while", "for", "loop", "repeat",
                "until", "leave", "iterate", "return", "call", "procedure", "function", "trigger",
                "declare", "set", "show", "use", "grant", "revoke", "lock", "unlock", "commit", "rollback",
                "transaction", "savepoint", "release", "begin", "work", "start", "stop", "kill", "flush",
                "reset", "reload", "shutdown", "analyze", "optimize", "repair", "check", "explain", "describe"
        };
        for (String keyword : keywords) {
            Escape.keywords.add(keyword.toLowerCase());
        }
    }

    /**
     * 转义列名，避免与数据库关键词冲突
     * MySQL 使用反引号包裹列名
     *
     * @param columnName 列名
     * @return 转义后的列名
     */
    public static String escape(String columnName) {
        if (columnName == null || columnName.isEmpty()) {
            return columnName;
        }
        // 检查是否已经包含反引号
        if (columnName.startsWith("`") && columnName.endsWith("`")) {
            return columnName;
        }
        // 检查是否是关键词（不区分大小写）
        String lowerColumnName = columnName.toLowerCase();
        if (keywords.contains(lowerColumnName)) {
            // 使用反引号包裹关键词
            return "`" + columnName + "`";
        }
        // 对于包含特殊字符或空格的列名，也需要转义
        if (columnName.contains(" ") || columnName.contains("-") || columnName.contains(".")) {
            return "`" + columnName + "`";
        }
        return columnName;
    }
}
