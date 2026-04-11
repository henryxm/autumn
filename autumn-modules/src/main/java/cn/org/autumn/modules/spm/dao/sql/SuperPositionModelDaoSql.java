package cn.org.autumn.modules.spm.dao.sql;

import cn.org.autumn.database.runtime.RuntimeSql;

/**
 * {@link cn.org.autumn.modules.spm.dao.SuperPositionModelDao} 可移植 SQL。
 */
public class SuperPositionModelDaoSql extends RuntimeSql {

    private String tbl() {
        return quote("spm_super_position_model");
    }

    public String getByResourceId() {
        return "SELECT * FROM " + tbl() + " WHERE " + quote("resource_id") + " = #{resourceId}" + limitOne();
    }

    public String getByUrlKey() {
        return "SELECT * FROM " + tbl() + " WHERE " + quote("url_key") + " = #{urlKey}" + limitOne();
    }
}
