package cn.org.autumn.modules.spm.dao.sql;

import cn.org.autumn.database.runtime.RuntimeSql;

/**
 * {@link cn.org.autumn.modules.spm.dao.SuperPositionModelDao} 可移植 SQL。
 */
public class SuperPositionModelDaoSql extends RuntimeSql {

    public String getByResourceId() {
        return "select * from spm_super_position_model where resource_id = #{resourceId}" + limitOne();
    }

    public String getByUrlKey() {
        return "select * from spm_super_position_model where url_key = #{urlKey}" + limitOne();
    }
}
