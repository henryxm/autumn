package cn.org.autumn.table.platform.jdbc;

import org.apache.commons.lang3.StringUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * 将方言 {@link cn.org.autumn.table.relational.RelationalSchemaSql} 生成的多语句脚本按分号拆分执行。
 * 与 {@link cn.org.autumn.table.dao.postgresql.PostgresRelationalTableOperations} 行为一致；
 * 拆分规则见 {@link RelationalDdlStatementSplitter}（单引号字符串内的分号不截断语句）。
 */
public final class RelationalDdlScriptExecutor {

    private RelationalDdlScriptExecutor() {
    }

    public static void execute(DataSource dataSource, String sql) {
        if (dataSource == null || StringUtils.isBlank(sql)) {
            return;
        }
        try (Connection cn = dataSource.getConnection(); Statement st = cn.createStatement()) {
            for (String part : RelationalDdlStatementSplitter.split(sql)) {
                String s = part.trim();
                if (s.isEmpty()) {
                    continue;
                }
                st.execute(s);
            }
        } catch (SQLException e) {
            throw new RuntimeException("DDL script failed: " + e.getMessage(), e);
        }
    }
}
