package cn.org.autumn;

import cn.org.autumn.site.TemplateFactory;
import cn.org.autumn.version.Autumn;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Autumn 框架启动入口 & 版本信息中心。
 * <p>
 * 提供统一的版本号读取接口，并在启动完成后以日志方式输出各核心组件版本，
 * 方便排查环境问题和确认部署版本。
 */
@EnableAspectJAutoProxy(proxyTargetClass = true, exposeProxy = true)
@ServletComponentScan
@EnableAsync
@SpringBootApplication
@MapperScan(basePackages = {"cn.org.autumn.modules.*.dao", "cn.org.autumn.table.dao"})
public class Web extends SpringBootServletInitializer implements TemplateFactory.Template {

    public static void main(String[] args) {
        new SpringApplication(Web.class).run(args);
        Autumn.versions();
    }
}