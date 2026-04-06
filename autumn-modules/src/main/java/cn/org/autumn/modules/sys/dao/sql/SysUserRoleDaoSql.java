package cn.org.autumn.modules.sys.dao.sql;

import cn.org.autumn.database.runtime.RuntimeSql;

/**
 * {@link cn.org.autumn.modules.sys.dao.SysUserRoleDao} 可移植 SQL。
 */
public class SysUserRoleDaoSql extends RuntimeSql {

    public String getRoleKeys() {
        return "SELECT " + quote("role_key") + " FROM " + quote("sys_user_role") + " WHERE " + quote("user_uuid") + " = #{userUuid}";
    }

    public String getByUsername() {
        return "SELECT * FROM " + quote("sys_user_role") + " WHERE " + quote("username") + " = #{username}";
    }

    public String hasUserRole() {
        return "SELECT COUNT(*) FROM " + quote("sys_user_role") + " WHERE " + quote("user_uuid") + " = #{userUuid} AND " + quote("role_key")
                + " = #{roleKey}";
    }

    /**
     * {@code IN (#{roleKeys})} 由 {@link cn.org.autumn.mybatis.SelectInLangDriver} 展开。
     */
    public String deleteByRoleKeys() {
        return "DELETE FROM " + quote("sys_user_role") + " WHERE " + quote("role_key") + " IN (#{roleKeys})";
    }
}
