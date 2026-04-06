package cn.org.autumn.modules.lan.dao.sql;

import cn.org.autumn.database.runtime.RuntimeSql;

/**
 * {@link cn.org.autumn.modules.lan.dao.LanguageDao} 可移植 SQL。
 */
public class LanguageDaoSql extends RuntimeSql {

    public String getByNameTag() {
        return "SELECT * FROM " + quote("sys_language") + " WHERE " + quote("name") + " = #{name} AND " + quote("tag") + " = #{tag}"
                + limitOne();
    }

    public String hasKey() {
        return "SELECT COUNT(*) FROM " + quote("sys_language") + " WHERE " + quote("name") + " = #{name}";
    }

    public String load() {
        return "SELECT * FROM " + quote("sys_language") + " WHERE " + quote("tag") + " IS NULL OR " + quote("tag") + " = ''";
    }
}
