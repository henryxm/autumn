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

    private String tbl() {
        return quote("sys_user");
    }

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
        return "UPDATE " + tbl() + " SET " + quote("verify") + " = #{verify} WHERE " + quote("uuid") + " = #{uuid}";
    }

    public String getByUsername() {
        return "SELECT * FROM " + tbl() + " WHERE " + quote("username") + " = #{username} AND " + quote("status") + " >= 0";
    }

    public String getByUsernameLike() {
        return "SELECT * FROM " + tbl() + " WHERE " + quote("username") + " LIKE " + likeContainsAny("#{username}") + " AND " + quote("status") + " >= 0" + limitOne();
    }

    public String getByEmail() {
        return "SELECT * FROM " + tbl() + " WHERE " + quote("email") + " = #{email} AND " + quote("status") + " >= 0";
    }

    public String getByEmailLike() {
        return "SELECT * FROM " + tbl() + " WHERE " + quote("email") + " LIKE " + likeContainsAny("#{email}") + " AND " + quote("status") + " >= 0" + limitOne();
    }

    public String getByPhone() {
        return "SELECT * FROM " + tbl() + " WHERE " + quote("mobile") + " = #{mobile} AND " + quote("status") + " >= 0";
    }

    public String getByPhoneLike() {
        return "SELECT * FROM " + tbl() + " WHERE " + quote("mobile") + " LIKE " + likeContainsAny("#{mobile}") + " AND " + quote("status") + " >= 0" + limitOne();
    }

    public String getByUuid() {
        return "SELECT * FROM " + tbl() + " WHERE " + quote("uuid") + " = #{uuid} AND " + quote("status") + " >= 0";
    }

    public String getForDelete() {
        return "SELECT * FROM " + tbl() + " WHERE " + quote("uuid") + " = #{uuid}";
    }

    public String getByUuidLike() {
        return "SELECT * FROM " + tbl() + " WHERE " + quote("uuid") + " LIKE " + likeContainsAny("#{uuid}") + " AND " + quote("status") + " >= 0" + limitOne();
    }

    public String getByQq() {
        return "SELECT * FROM " + tbl() + " WHERE " + quote("qq") + " = #{qq} AND " + quote("status") + " >= 0";
    }

    public String getByQqLike() {
        return "SELECT * FROM " + tbl() + " WHERE " + quote("qq") + " LIKE " + likeContainsAny("#{qq}") + " AND " + quote("status") + " >= 0" + limitOne();
    }

    public String getByWeixing() {
        return "SELECT * FROM " + tbl() + " WHERE " + quote("weixin") + " = #{weixin} AND " + quote("status") + " >= 0";
    }

    public String getByWeixingLike() {
        return "SELECT * FROM " + tbl() + " WHERE " + quote("weixin") + " LIKE " + likeContainsAny("#{weixin}") + " AND " + quote("status") + " >= 0" + limitOne();
    }

    public String getByAlipay() {
        return "SELECT * FROM " + tbl() + " WHERE " + quote("alipay") + " = #{alipay} AND " + quote("status") + " >= 0";
    }

    public String getByAlipayLike() {
        return "SELECT * FROM " + tbl() + " WHERE " + quote("alipay") + " LIKE " + likeContainsAny("#{alipay}") + " AND " + quote("status") + " >= 0" + limitOne();
    }

    public String getByIdCard() {
        return "SELECT * FROM " + tbl() + " WHERE " + quote("id_card") + " = #{idCard} AND " + quote("status") + " >= 0";
    }

    public String getByIdCardLike() {
        return "SELECT * FROM " + tbl() + " WHERE " + quote("id_card") + " LIKE " + likeContainsAny("#{idCard}") + " AND " + quote("status") + " >= 0" + limitOne();
    }
}
