package cn.org.autumn.modules.sys.dao.sql;

import cn.org.autumn.database.runtime.RuntimeSqlDialect;
import cn.org.autumn.database.runtime.RuntimeSqlDialectRegistry;

/**
 * {@link cn.org.autumn.modules.sys.dao.SysUserDao} 的可移植 SQL（MySQL / PostgreSQL）。
 */
public class SysUserDaoSql {

    private RuntimeSqlDialect d() {
        return RuntimeSqlDialectRegistry.get();
    }

    public String verify() {
        return "update sys_user set " + d().quote("verify") + " = #{verify} where " + d().quote("uuid") + " = #{uuid}";
    }

    public String getByUsername() {
        return "select * from sys_user where " + d().quote("username") + " = #{username} and " + d().quote("status") + " >= 0";
    }

    public String getByUsernameLike() {
        return "select * from sys_user where " + d().quote("username") + " like concat('%', #{username}, '%') and " + d().quote("status") + " >= 0" + d().limitOne();
    }

    public String getByEmail() {
        return "SELECT * FROM sys_user WHERE " + d().quote("email") + " = #{email} and " + d().quote("status") + " >= 0";
    }

    public String getByEmailLike() {
        return "SELECT * FROM sys_user WHERE " + d().quote("email") + " like concat('%', #{email}, '%') and " + d().quote("status") + " >= 0" + d().limitOne();
    }

    public String getByPhone() {
        return "SELECT * FROM sys_user WHERE " + d().quote("mobile") + " = #{mobile} and " + d().quote("status") + " >= 0";
    }

    public String getByPhoneLike() {
        return "SELECT * FROM sys_user WHERE " + d().quote("mobile") + " like concat('%', #{mobile}, '%') and " + d().quote("status") + " >= 0" + d().limitOne();
    }

    public String getByUuid() {
        return "SELECT * FROM sys_user WHERE " + d().quote("uuid") + " = #{uuid} and " + d().quote("status") + " >= 0";
    }

    public String getForDelete() {
        return "SELECT * FROM sys_user WHERE " + d().quote("uuid") + " = #{uuid}";
    }

    public String getByUuidLike() {
        return "SELECT * FROM sys_user WHERE " + d().quote("uuid") + " like concat('%', #{uuid}, '%')  and " + d().quote("status") + " >= 0" + d().limitOne();
    }

    public String getByQq() {
        return "SELECT * FROM sys_user WHERE " + d().quote("qq") + " = #{qq} and " + d().quote("status") + " >= 0";
    }

    public String getByQqLike() {
        return "SELECT * FROM sys_user WHERE " + d().quote("qq") + " like concat('%', #{qq}, '%') and " + d().quote("status") + " >= 0" + d().limitOne();
    }

    public String getByWeixing() {
        return "SELECT * FROM sys_user WHERE " + d().quote("weixin") + " = #{weixin} and " + d().quote("status") + " >= 0";
    }

    public String getByWeixingLike() {
        return "SELECT * FROM sys_user WHERE " + d().quote("weixin") + " like concat('%', #{weixin}, '%') and " + d().quote("status") + " >= 0" + d().limitOne();
    }

    public String getByAlipay() {
        return "SELECT * FROM sys_user WHERE " + d().quote("alipay") + " = #{alipay} and " + d().quote("status") + " >= 0";
    }

    public String getByAlipayLike() {
        return "SELECT * FROM sys_user WHERE " + d().quote("alipay") + " like concat('%', #{alipay}, '%') and " + d().quote("status") + " >= 0" + d().limitOne();
    }

    public String getByIdCard() {
        return "SELECT * FROM sys_user WHERE " + d().quote("id_card") + " = #{idCard} and " + d().quote("status") + " >= 0";
    }

    public String getByIdCardLike() {
        return "SELECT * FROM sys_user WHERE " + d().quote("id_card") + " like concat('%', #{idCard}, '%') and " + d().quote("status") + " >= 0" + d().limitOne();
    }
}
