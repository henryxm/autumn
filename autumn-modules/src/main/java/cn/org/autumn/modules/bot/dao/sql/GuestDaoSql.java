package cn.org.autumn.modules.bot.dao.sql;

import cn.org.autumn.database.runtime.RuntimeSql;

/**
 * 临时用户表 SQL。列名经 {@link RuntimeSql#quote} 交给方言，不写死引号。
 */
public class GuestDaoSql extends RuntimeSql {

    private String tbl() {
        return quote("bot_guest");
    }

    public String getByUuid() {
        return "SELECT * FROM " + tbl() + " WHERE " + quote("uuid") + " = #{uuid}" + limitOne();
    }

    public String countByUuid() {
        return "SELECT COUNT(*) FROM " + tbl() + " WHERE " + quote("uuid") + " = #{uuid}";
    }

    public String getByGuestKey() {
        return "SELECT * FROM " + tbl() + " WHERE " + quote("guest_key") + " = #{guestKey}" + limitOne();
    }

    /**
     * 物理删除已过期行，不触碰消息库。
     */
    public String deleteExpired() {
        return "DELETE FROM " + tbl() + " WHERE " + quote("expire_time") + " IS NOT NULL AND " + quote("expire_time") + " < #{now}";
    }
}
