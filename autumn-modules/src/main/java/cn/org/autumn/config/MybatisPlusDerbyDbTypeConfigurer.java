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
 * MyBatis-Plus 2.1.x {@link com.baomidou.mybatisplus.toolkit.JdbcUtils#getDbType(String)} 仅识别
 * mysql/oracle/sqlserver/postgresql/hsqldb/db2/sqlite/h2 等前缀，<b>没有</b> {@code jdbc:derby:} 分支。
 * 因此任意合法 Derby URL（如 {@code jdbc:derby:embedded/derby/autumn;create=true}）都会触发
 * {@code Cannot Read Database type} 的 WARN 并得到 {@link com.baomidou.mybatisplus.enums.DBType#OTHER}——属 MP 上游局限，
 * 非 Autumn 或 JDBC URL 配置错误；{@link cn.org.autumn.database.DatabaseHolder} 仍按 URL 正确解析为 {@link DatabaseType#DERBY}。
 * <p>
 * 本类在首源为 Derby 时将 {@link GlobalConfiguration#setDbType} 设为 {@code postgresql}，使
 * {@link com.baomidou.mybatisplus.toolkit.SqlReservedWords} 对<b>所有</b>列加引号（与注解双引号 DDL 一致），抵消 {@code OTHER}
 * 带来的保留字/大小写问题；若误用 {@code db2} 则仅保留字列加引号，普通列会被 Derby 折成大写而与 {@code "param_key"} 等不一致。
 * 分页方言由 {@link cn.org.autumn.database.DatabaseHolder}（及 {@link ThreadLocalPaginationInterceptor}）提供，不依赖该 WARN 路径。
 * <p>
 * Derby 对 {@code setNull(..., OTHER)} 与 {@code Types.NULL} 均不支持，故将 {@code jdbcTypeForNull} 设为 {@link JdbcType#VARCHAR}。
 * <p>
 * <b>限制</b>：{@link GlobalConfiguration#setDbType} 与 {@link org.apache.ibatis.session.Configuration} 为进程级单例，
 * 首源为 Derby、second 为其它库时，MP 列引号策略无法按线程切换；异构场景需接受该限制或避免在 second 上依赖 MP 自动引号。
 * <p>
 * 误导性 WARN 可在应用 {@code logback-spring.xml} 中将 {@code com.baomidou.mybatisplus.toolkit.JdbcUtils} 设为 {@code ERROR}（见 web 模块）。
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
