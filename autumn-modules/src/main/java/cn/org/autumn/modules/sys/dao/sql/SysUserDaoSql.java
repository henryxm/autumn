package cn.org.autumn.modules.sys.dao.sql;

import cn.org.autumn.database.runtime.RuntimeSqlDialect;
import cn.org.autumn.database.runtime.RuntimeSqlDialectRegistry;

/**
 * {@link cn.org.autumn.modules.sys.dao.SysUserDao} 的可移植 SQL。
 * <p>
 * 模糊条件须使用 {@link RuntimeSqlDialect#likeContainsAny(String)}，勿手写 {@code concat('%', #{x}, '%')}，
 * 以便在 MySQL/MariaDB、PostgreSQL、Oracle、SQL Server 下语义一致（Oracle {@code CONCAT} 仅双参）。
 */
public class SysUserDaoSql {

    private RuntimeSqlDialect d() {
        return RuntimeSqlDialectRegistry.get();
    }

    /**
     * 权限菜单 JOIN：表名/列名均经方言引用。
     */
    public String getPermsByUserUuid() {
        RuntimeSqlDialect d = d();
        return "SELECT " + d.quote("m") + "." + d.quote("perms") + " FROM " + d.quote("sys_user_role") + " ur "
                + "LEFT JOIN " + d.quote("sys_role_menu") + " rm ON ur." + d.quote("role_key") + " = rm." + d.quote("role_key") + " "
                + "LEFT JOIN " + d.quote("sys_menu") + " m ON rm." + d.quote("menu_key") + " = m." + d.quote("menu_key") + " "
                + "WHERE ur." + d.quote("user_uuid") + " = #{userUuid}";
    }

    public String getMenus() {
        RuntimeSqlDialect d = d();
        return "SELECT DISTINCT " + d.quote("rm") + "." + d.quote("menu_key") + " FROM " + d.quote("sys_user_role") + " ur "
                + "LEFT JOIN " + d.quote("sys_role_menu") + " rm ON ur." + d.quote("role_key") + " = rm." + d.quote("role_key") + " "
                + "WHERE ur." + d.quote("user_uuid") + " = #{userUuid}";
    }

    public String verify() {
        return "update sys_user set " + d().quote("verify") + " = #{verify} where " + d().quote("uuid") + " = #{uuid}";
    }

    public String getByUsername() {
        return "select * from sys_user where " + d().quote("username") + " = #{username} and " + d().quote("status") + " >= 0";
    }

    public String getByUsernameLike() {
        return "select * from sys_user where " + d().quote("username") + " like " + d().likeContainsAny("#{username}") + " and " + d().quote("status") + " >= 0" + d().limitOne();
    }

    public String getByEmail() {
        return "SELECT * FROM sys_user WHERE " + d().quote("email") + " = #{email} and " + d().quote("status") + " >= 0";
    }

    public String getByEmailLike() {
        return "SELECT * FROM sys_user WHERE " + d().quote("email") + " like " + d().likeContainsAny("#{email}") + " and " + d().quote("status") + " >= 0" + d().limitOne();
    }

    public String getByPhone() {
        return "SELECT * FROM sys_user WHERE " + d().quote("mobile") + " = #{mobile} and " + d().quote("status") + " >= 0";
    }

    public String getByPhoneLike() {
        return "SELECT * FROM sys_user WHERE " + d().quote("mobile") + " like " + d().likeContainsAny("#{mobile}") + " and " + d().quote("status") + " >= 0" + d().limitOne();
    }

    public String getByUuid() {
        return "SELECT * FROM sys_user WHERE " + d().quote("uuid") + " = #{uuid} and " + d().quote("status") + " >= 0";
    }

    public String getForDelete() {
        return "SELECT * FROM sys_user WHERE " + d().quote("uuid") + " = #{uuid}";
    }

    public String getByUuidLike() {
        return "SELECT * FROM sys_user WHERE " + d().quote("uuid") + " like " + d().likeContainsAny("#{uuid}") + "  and " + d().quote("status") + " >= 0" + d().limitOne();
    }

    public String getByQq() {
        return "SELECT * FROM sys_user WHERE " + d().quote("qq") + " = #{qq} and " + d().quote("status") + " >= 0";
    }

    public String getByQqLike() {
        return "SELECT * FROM sys_user WHERE " + d().quote("qq") + " like " + d().likeContainsAny("#{qq}") + " and " + d().quote("status") + " >= 0" + d().limitOne();
    }

    public String getByWeixing() {
        return "SELECT * FROM sys_user WHERE " + d().quote("weixin") + " = #{weixin} and " + d().quote("status") + " >= 0";
    }

    public String getByWeixingLike() {
        return "SELECT * FROM sys_user WHERE " + d().quote("weixin") + " like " + d().likeContainsAny("#{weixin}") + " and " + d().quote("status") + " >= 0" + d().limitOne();
    }

    public String getByAlipay() {
        return "SELECT * FROM sys_user WHERE " + d().quote("alipay") + " = #{alipay} and " + d().quote("status") + " >= 0";
    }

    public String getByAlipayLike() {
        return "SELECT * FROM sys_user WHERE " + d().quote("alipay") + " like " + d().likeContainsAny("#{alipay}") + " and " + d().quote("status") + " >= 0" + d().limitOne();
    }

    public String getByIdCard() {
        return "SELECT * FROM sys_user WHERE " + d().quote("id_card") + " = #{idCard} and " + d().quote("status") + " >= 0";
    }

    public String getByIdCardLike() {
        return "SELECT * FROM sys_user WHERE " + d().quote("id_card") + " like " + d().likeContainsAny("#{idCard}") + " and " + d().quote("status") + " >= 0" + d().limitOne();
    }
}
