package cn.org.autumn.modules.sys.dao.sql;

import cn.org.autumn.database.runtime.RuntimeSqlDialect;
import cn.org.autumn.database.runtime.RuntimeSqlDialectRegistry;

/**
 * {@link cn.org.autumn.modules.sys.dao.SysUserRoleDao} 可移植 SQL。
 */
public class SysUserRoleDaoSql {

    private RuntimeSqlDialect d() {
        return RuntimeSqlDialectRegistry.get();
    }

    public String getRoleKeys() {
        return "SELECT " + d().quote("role_key") + " FROM " + d().quote("sys_user_role") + " WHERE " + d().quote("user_uuid") + " = #{userUuid}";
    }

    public String getByUsername() {
        return "SELECT * FROM " + d().quote("sys_user_role") + " WHERE " + d().quote("username") + " = #{username}";
    }

    public String hasUserRole() {
        return "SELECT COUNT(*) FROM " + d().quote("sys_user_role") + " WHERE " + d().quote("user_uuid") + " = #{userUuid} AND " + d().quote("role_key")
                + " = #{roleKey}";
    }

    /**
     * {@code IN (#{roleKeys})} 由 {@link cn.org.autumn.mybatis.SelectInLangDriver} 展开。
     */
    public String deleteByRoleKeys() {
        return "DELETE FROM " + d().quote("sys_user_role") + " WHERE " + d().quote("role_key") + " IN (#{roleKeys})";
    }
}
