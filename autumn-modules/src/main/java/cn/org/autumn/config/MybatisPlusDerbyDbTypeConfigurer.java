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
 * MyBatis-Plus 3.x 的 {@code JdbcUtils#getDbType(String)} 对 {@code jdbc:derby:} 仍可能给出与 Autumn
 * {@link DatabaseType#DERBY} 不一致的推断或 WARN；分页方言由 {@link DatabaseHolder} 与
 * {@link RoutingPaginationInnerInterceptor} 按 {@link cn.org.autumn.database.DatabaseType} 驱动，不依赖该推断路径。
 * <p>
 * MP 3 的 {@code QueryWrapper} 字符串列不自动加引号，须用 {@code LambdaQueryWrapper} 或
 * {@link cn.org.autumn.database.runtime.WrapperColumns#columnInWrapper(String)}，与 {@code docs/AI_DATABASE.md} 一致。
 * <p>
 * <b>限制</b>：{@link org.apache.ibatis.session.Configuration} 为进程级单例，首源为 Derby、second 为其它库时
 * {@code jdbcTypeForNull} 等无法按线程切换；异构场景需接受该限制。
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
