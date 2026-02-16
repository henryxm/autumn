package cn.org.autumn.autoconfigure;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Autumn 框架自动配置类。
 * <p>
 * 外部项目引入 {@code autumn-spring-boot-starter} 后，
 * 此类自动完成以下配置：
 * <ul>
 *     <li>扫描 {@code cn.org.autumn} 包下的所有 Spring 组件</li>
 *     <li>扫描 MyBatis Mapper 接口</li>
 *     <li>扫描 Servlet 组件（如 Druid 监控过滤器）</li>
 *     <li>启用 AOP 代理（exposeProxy=true 支持同类方法内部调用增强）</li>
 *     <li>启用异步方法支持</li>
 * </ul>
 */
@AutoConfiguration
@ComponentScan("cn.org.autumn")
@ServletComponentScan("cn.org.autumn")
@EnableAspectJAutoProxy(exposeProxy = true)
@EnableAsync
@MapperScan(basePackages = {"cn.org.autumn.modules.*.dao", "cn.org.autumn.table.dao"})
public class AutumnAutoConfiguration {
}
