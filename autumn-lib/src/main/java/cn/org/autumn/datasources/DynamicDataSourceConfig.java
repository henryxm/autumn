package cn.org.autumn.datasources;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.spring.boot.autoconfigure.DruidDataSourceBuilder;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 配置多数据源
 */
@Configuration
@EnableAutoConfiguration(exclude = {DataSourceAutoConfiguration.class})
public class DynamicDataSourceConfig {

    @Bean
    @ConfigurationProperties("spring.datasource.druid.first")
    public DataSource firstDataSource() {
        return DruidDataSourceBuilder.create().build();
    }

    /**
     * 嵌入式 Derby 同一库目录只能在一个 JVM 内 boot 一次；first、second 若配置相同 URL 却建两个 Druid 池，
     * 创建连接线程并发 boot 会触发 ERROR XSDB6。second 在此与 first 共用一个物理数据源。
     * <p>
     * 不在此单独注册 {@code secondDataSource} Bean，避免其依赖 {@code firstDataSource} 与路由 {@code dataSource}
     * 之间形成 Spring 无法拆解的循环依赖。
     */
    @Bean
    @Primary
    public DynamicDataSource dataSource(Environment environment, @Qualifier("firstDataSource") DataSource firstDataSource) {
        String u1 = environment.getProperty("spring.datasource.druid.first.url", "");
        String u2 = environment.getProperty("spring.datasource.druid.second.url", "");
        DataSource second;
        if (sameEmbeddedDerbyHome(u1, u2)) {
            second = firstDataSource;
        } else {
            DruidDataSource ds = DruidDataSourceBuilder.create().build();
            Binder.get(environment).bind("spring.datasource.druid.second", Bindable.ofInstance(ds));
            second = ds;
        }
        Map<Object, Object> targetDataSources = new HashMap<>();
        targetDataSources.put(DataSourceNames.FIRST, firstDataSource);
        targetDataSources.put(DataSourceNames.SECOND, second);
        return new DynamicDataSource(firstDataSource, targetDataSources);
    }

    /**
     * 比较 {@code jdbc:derby:…} 是否指向同一嵌入式库（忽略属性顺序、大小写）；非 Derby 则视为不同池。
     */
    static boolean sameEmbeddedDerbyHome(String firstUrl, String secondUrl) {
        if (firstUrl == null || secondUrl == null) {
            return false;
        }
        String a = firstUrl.trim().toLowerCase(Locale.ROOT);
        String b = secondUrl.trim().toLowerCase(Locale.ROOT);
        if (!a.startsWith("jdbc:derby:") || !b.startsWith("jdbc:derby:")) {
            return false;
        }
        return derbyPath(a).equals(derbyPath(b));
    }

    private static String derbyPath(String jdbcUrl) {
        String rest = jdbcUrl.substring("jdbc:derby:".length());
        int semi = rest.indexOf(';');
        return (semi >= 0 ? rest.substring(0, semi) : rest).trim();
    }
}
