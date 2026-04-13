package cn.org.autumn.config;

import cn.org.autumn.database.DatabaseHolder;
import com.baomidou.mybatisplus.entity.GlobalConfiguration;
import com.baomidou.mybatisplus.toolkit.GlobalConfigUtils;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

/**
 * 在每个 {@link SqlSessionFactory} Bean 完成初始化后、依赖它的 {@link org.mybatis.spring.mapper.MapperFactoryBean}
 * 注册 Mapper 之前，将 MP 2.x {@link GlobalConfiguration} 的 {@code dbType} 与 {@code identifier-quote}
 * 与 {@link DatabaseHolder} 对齐。
 * <p>
 * 若仅用 {@link org.springframework.beans.factory.InitializingBean} 在其它 Bean 上同步，执行顺序可能晚于
 * Mapper 扫描，此时 BaseMapper 等生成的 SQL 已固化，仍带 MySQL 反引号，PostgreSQL 会报
 * {@code syntax error at or near "`"}。{@link BeanPostProcessor#postProcessAfterInitialization} 保证先于 Mapper 注册执行。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MybatisPlusSqlSessionFactoryDialectBeanPostProcessor implements BeanPostProcessor {

    private final DatabaseHolder databaseHolder;

    public MybatisPlusSqlSessionFactoryDialectBeanPostProcessor(DatabaseHolder databaseHolder) {
        this.databaseHolder = databaseHolder;
    }

    @Override
    public Object postProcessAfterInitialization(@NonNull Object bean, @NonNull String beanName) throws BeansException {
        if (bean instanceof SqlSessionFactory) {
            applyDialect((SqlSessionFactory) bean);
        }
        return bean;
    }

    private void applyDialect(SqlSessionFactory sqlSessionFactory) {
        GlobalConfiguration gc = GlobalConfigUtils.getGlobalConfig(sqlSessionFactory.getConfiguration());
        if (gc == null) {
            return;
        }
        MybatisPlusGlobalDialectAlign.apply(databaseHolder.getType(), gc);
    }
}
