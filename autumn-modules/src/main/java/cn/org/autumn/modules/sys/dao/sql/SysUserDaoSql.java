package cn.org.autumn.modules.sys.dao.sql;

import cn.org.autumn.database.runtime.RuntimeSql;
import cn.org.autumn.database.runtime.RuntimeSqlDialect;

/**
 * {@link cn.org.autumn.modules.sys.dao.SysUserDao} 的可移植 SQL。
 * <p>
 * 模糊条件须使用 {@link #likeContainsAny(String)}（委托 {@link RuntimeSqlDialect#likeContainsAny(String)}），勿手写 {@code concat('%', #{x}, '%')}，
 * 以便在 MySQL/MariaDB、PostgreSQL、Oracle、SQL Server 下语义一致（Oracle {@code CONCAT} 仅双参）。
 */
public class SysUserDaoSql extends RuntimeSql {

    /**
     * 权限菜单 JOIN：表名/列名均经方言引用。
     */
    public String getPermsByUserUuid() {
        return "SELECT " + quote("m") + "." + quote("perms") + " FROM " + quote("sys_user_role") + " ur "
                + "LEFT JOIN " + quote("sys_role_menu") + " rm ON ur." + quote("role_key") + " = rm." + quote("role_key") + " "
                + "LEFT JOIN " + quote("sys_menu") + " m ON rm." + quote("menu_key") + " = m." + quote("menu_key") + " "
                + "WHERE ur." + quote("user_uuid") + " = #{userUuid}";
    }

    public String getMenus() {
        return "SELECT DISTINCT " + quote("rm") + "." + quote("menu_key") + " FROM " + quote("sys_user_role") + " ur "
                + "LEFT JOIN " + quote("sys_role_menu") + " rm ON ur." + quote("role_key") + " = rm." + quote("role_key") + " "
                + "WHERE ur." + quote("user_uuid") + " = #{userUuid}";
    }

    public String verify() {
        return "update sys_user set " + quote("verify") + " = #{verify} where " + quote("uuid") + " = #{uuid}";
    }

    public String getByUsername() {
        return "select * from sys_user where " + quote("username") + " = #{username} and " + quote("status") + " >= 0";
    }

    public String getByUsernameLike() {
        return "select * from sys_user where " + quote("username") + " like " + likeContainsAny("#{username}") + " and " + quote("status") + " >= 0" + limitOne();
    }

    public String getByEmail() {
        return "SELECT * FROM sys_user WHERE " + quote("email") + " = #{email} and " + quote("status") + " >= 0";
    }

    public String getByEmailLike() {
        return "SELECT * FROM sys_user WHERE " + quote("email") + " like " + likeContainsAny("#{email}") + " and " + quote("status") + " >= 0" + limitOne();
    }

    public String getByPhone() {
        return "SELECT * FROM sys_user WHERE " + quote("mobile") + " = #{mobile} and " + quote("status") + " >= 0";
    }

    public String getByPhoneLike() {
        return "SELECT * FROM sys_user WHERE " + quote("mobile") + " like " + likeContainsAny("#{mobile}") + " and " + quote("status") + " >= 0" + limitOne();
    }

    public String getByUuid() {
        return "SELECT * FROM sys_user WHERE " + quote("uuid") + " = #{uuid} and " + quote("status") + " >= 0";
    }

    public String getForDelete() {
        return "SELECT * FROM sys_user WHERE " + quote("uuid") + " = #{uuid}";
    }

    public String getByUuidLike() {
        return "SELECT * FROM sys_user WHERE " + quote("uuid") + " like " + likeContainsAny("#{uuid}") + "  and " + quote("status") + " >= 0" + limitOne();
    }

    public String getByQq() {
        return "SELECT * FROM sys_user WHERE " + quote("qq") + " = #{qq} and " + quote("status") + " >= 0";
    }

    public String getByQqLike() {
        return "SELECT * FROM sys_user WHERE " + quote("qq") + " like " + likeContainsAny("#{qq}") + " and " + quote("status") + " >= 0" + limitOne();
    }

    public String getByWeixing() {
        return "SELECT * FROM sys_user WHERE " + quote("weixin") + " = #{weixin} and " + quote("status") + " >= 0";
    }

    public String getByWeixingLike() {
        return "SELECT * FROM sys_user WHERE " + quote("weixin") + " like " + likeContainsAny("#{weixin}") + " and " + quote("status") + " >= 0" + limitOne();
    }

    public String getByAlipay() {
        return "SELECT * FROM sys_user WHERE " + quote("alipay") + " = #{alipay} and " + quote("status") + " >= 0";
    }

    public String getByAlipayLike() {
        return "SELECT * FROM sys_user WHERE " + quote("alipay") + " like " + likeContainsAny("#{alipay}") + " and " + quote("status") + " >= 0" + limitOne();
    }

    public String getByIdCard() {
        return "SELECT * FROM sys_user WHERE " + quote("id_card") + " = #{idCard} and " + quote("status") + " >= 0";
    }

    public String getByIdCardLike() {
        return "SELECT * FROM sys_user WHERE " + quote("id_card") + " like " + likeContainsAny("#{idCard}") + " and " + quote("status") + " >= 0" + limitOne();
    }
}
