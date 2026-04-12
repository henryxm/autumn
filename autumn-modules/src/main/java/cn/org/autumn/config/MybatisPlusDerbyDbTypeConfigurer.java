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
 * 列名引用与 {@code column-format}/{@code table-format} 由 {@link MybatisPlusConfig} 配置。
 * <p>
 * master（MP 2.x）在同类场景下曾设 {@code GlobalConfiguration#setDbType("postgresql")}，使字符串 {@code EntityWrapper}
 * 条件列也走转义；MP 3 的 {@code QueryWrapper} 字符串列仍不自动加引号，须用 {@code LambdaQueryWrapper} 或
 * {@link cn.org.autumn.database.runtime.WrapperColumns#columnInWrapper(String)}，与 {@code docs/AI_DATABASE.md} 一致。
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
