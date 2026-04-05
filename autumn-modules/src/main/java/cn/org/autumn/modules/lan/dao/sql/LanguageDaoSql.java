package cn.org.autumn.modules.lan.dao.sql;

import cn.org.autumn.database.runtime.RuntimeSqlDialect;
import cn.org.autumn.database.runtime.RuntimeSqlDialectRegistry;

/**
 * {@link cn.org.autumn.modules.lan.dao.LanguageDao} 可移植 SQL。
 */
public class LanguageDaoSql {

    private RuntimeSqlDialect d() {
        return RuntimeSqlDialectRegistry.get();
    }

    public String getByNameTag() {
        return "SELECT * FROM " + d().quote("sys_language") + " WHERE " + d().quote("name") + " = #{name} AND " + d().quote("tag") + " = #{tag}"
                + d().limitOne();
    }

    public String hasKey() {
        return "SELECT COUNT(*) FROM " + d().quote("sys_language") + " WHERE " + d().quote("name") + " = #{name}";
    }

    public String load() {
        return "SELECT * FROM " + d().quote("sys_language") + " WHERE " + d().quote("tag") + " IS NULL OR " + d().quote("tag") + " = ''";
    }
}
