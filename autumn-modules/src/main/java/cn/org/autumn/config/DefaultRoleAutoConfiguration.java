package cn.org.autumn.config;

import cn.org.autumn.modules.sys.service.SysUserRoleService;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * 在无任何 {@link Role} Bean 时注册 {@link StandardRole}。
 * 通过自动配置靠后执行，优先让业务模块（如 Minclouds）先注册 {@link Role}。
 */
@Configuration
@AutoConfigureOrder(Ordered.LOWEST_PRECEDENCE)
public class DefaultRoleAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(Role.class)
    public Role standardRole(SysUserRoleService sysUserRoleService) {
        return new StandardRole(sysUserRoleService);
    }
}
