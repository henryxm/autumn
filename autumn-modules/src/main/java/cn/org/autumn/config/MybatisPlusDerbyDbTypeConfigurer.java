package cn.org.autumn.config;

import cn.org.autumn.database.DatabaseHolder;
import cn.org.autumn.database.DatabaseType;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.type.JdbcType;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Derby 下将 MyBatis {@code jdbcTypeForNull} 设为 {@link JdbcType#VARCHAR}（驱动不支持 {@code OTHER}/{@code NULL}）。
 * 列名引用与全局 {@code dbType}/{@code column-format} 由 {@link MybatisPlusConfig} 配置。
 */
@Component
public class MybatisPlusDerbyDbTypeConfigurer implements InitializingBean {

    @Autowired
    private SqlSessionFactory sqlSessionFactory;

    @Autowired
    private DatabaseHolder databaseHolder;

    @Override
    public void afterPropertiesSet() {
        if (databaseHolder.getType() != DatabaseType.DERBY) {
            return;
        }
        sqlSessionFactory.getConfiguration().setJdbcTypeForNull(JdbcType.VARCHAR);
    }
}
