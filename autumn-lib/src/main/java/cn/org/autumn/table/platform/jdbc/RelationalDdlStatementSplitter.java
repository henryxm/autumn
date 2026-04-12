package cn.org.autumn.table.platform.jdbc;

import java.util.ArrayList;
import java.util.List;

/**
 * 将多语句 DDL 按分号拆分，忽略单引号字符串内的分号（含 PostgreSQL {@code ''} 转义）。
 * 用于 {@link RelationalDdlScriptExecutor}、{@link cn.org.autumn.table.dao.postgresql.PostgresRelationalTableOperations}，
 * 避免 {@code COMMENT ... IS 'a;b'} 被错误截断。
 */
public final class RelationalDdlStatementSplitter {

    private RelationalDdlStatementSplitter() {
    }

    public static List<String> split(String sql) {
        List<String> parts = new ArrayList<>();
        if (sql == null || sql.isEmpty()) {
            return parts;
        }
        StringBuilder cur = new StringBuilder(sql.length());
        boolean inSingleQuote = false;
        for (int i = 0; i < sql.length(); i++) {
            char c = sql.charAt(i);
            if (inSingleQuote) {
                cur.append(c);
                if (c == '\'') {
                    if (i + 1 < sql.length() && sql.charAt(i + 1) == '\'') {
                        cur.append('\'');
                        i++;
                    } else {
                        inSingleQuote = false;
                    }
                }
            } else {
                if (c == '\'') {
                    inSingleQuote = true;
                    cur.append(c);
                } else if (c == ';') {
                    String t = cur.toString().trim();
                    if (!t.isEmpty()) {
                        parts.add(t);
                    }
                    cur.setLength(0);
                } else {
                    cur.append(c);
                }
            }
        }
        String tail = cur.toString().trim();
        if (!tail.isEmpty()) {
            parts.add(tail);
        }
        return parts;
    }
}
