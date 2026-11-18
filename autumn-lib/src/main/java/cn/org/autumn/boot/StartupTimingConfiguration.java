package cn.org.autumn.boot;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Role;
import org.springframework.core.env.Environment;

/**
 * 注册启动期 Bean 初始化耗时采集（见 {@link StartupBeanTimingBeanPostProcessor}）。
 */
@Configuration
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
public class StartupTimingConfiguration {

    @Bean
    public static StartupBeanTimingBeanPostProcessor startupBeanTimingBeanPostProcessor(Environment environment) {
        return new StartupBeanTimingBeanPostProcessor(environment);
    }
}
