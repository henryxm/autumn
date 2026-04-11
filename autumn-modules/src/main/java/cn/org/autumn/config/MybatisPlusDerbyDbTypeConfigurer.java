package cn.org.autumn.config;

import cn.org.autumn.database.DatabaseHolder;
import cn.org.autumn.database.DatabaseType;
import com.baomidou.mybatisplus.entity.GlobalConfiguration;
import com.baomidou.mybatisplus.toolkit.GlobalConfigUtils;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.type.JdbcType;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * MyBatis-Plus 2.x {@link com.baomidou.mybatisplus.toolkit.JdbcUtils#getDbType} 未识别 {@code jdbc:derby:}，
 * 会打 WARN 并将 {@link com.baomidou.mybatisplus.enums.DBType} 置为 {@code OTHER}，影响主键/SQL 片段等。
 * Derby 下须将全局类型设为 {@code postgresql}，使 {@link com.baomidou.mybatisplus.toolkit.SqlReservedWords} 对
 * <b>所有</b>列名加引号（与注解双引号 DDL 一致）；若用 {@code db2} 则仅保留字列加引号，其余列被 Derby 折成大写导致
 * 与 {@code "param_key"} 等不匹配。分页方言仍由 {@link cn.org.autumn.database.DatabaseHolder} 的 derby 方言提供。
 * <p>
 * Derby 对 {@code setNull(..., OTHER)} 与 {@code Types.NULL} 均不支持，故将 {@code jdbcTypeForNull} 设为 {@link JdbcType#VARCHAR}。
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
        Configuration configuration = sqlSessionFactory.getConfiguration();
        configuration.setJdbcTypeForNull(JdbcType.VARCHAR);
        GlobalConfiguration globalConfiguration = GlobalConfigUtils.getGlobalConfig(configuration);
        if (globalConfiguration == null) {
            return;
        }
        // 与 MP 2.x 共享 GlobalConfiguration 时也会落到 defaults()；Derby 须 postgresql 以全列引号对齐 DDL，见类注释。
        globalConfiguration.setDbType("postgresql");
    }
}
