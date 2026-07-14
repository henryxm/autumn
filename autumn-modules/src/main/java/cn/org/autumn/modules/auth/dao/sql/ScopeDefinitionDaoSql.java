package cn.org.autumn.modules.auth.dao.sql;

import cn.org.autumn.database.runtime.RuntimeSql;

public class ScopeDefinitionDaoSql extends RuntimeSql {

    private String tbl() {
        return quote("auth_scope_definition");
    }

    public String getByCode() {
        return "SELECT * FROM " + tbl() + " WHERE " + quote("code") + " = #{code}" + limitOne();
    }

    public String getByUuid() {
        return "SELECT * FROM " + tbl() + " WHERE " + quote("uuid") + " = #{uuid}" + limitOne();
    }
}
