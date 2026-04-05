package cn.org.autumn.modules.spm.dao.sql;

import cn.org.autumn.database.runtime.RuntimeSqlDialect;
import cn.org.autumn.database.runtime.RuntimeSqlDialectRegistry;

/**
 * {@link cn.org.autumn.modules.spm.dao.SuperPositionModelDao} 可移植 SQL。
 */
public class SuperPositionModelDaoSql {

    private RuntimeSqlDialect d() {
        return RuntimeSqlDialectRegistry.get();
    }

    public String getByResourceId() {
        return "select * from spm_super_position_model where resource_id = #{resourceId}" + d().limitOne();
    }

    public String getByUrlKey() {
        return "select * from spm_super_position_model where url_key = #{urlKey}" + d().limitOne();
    }
}
