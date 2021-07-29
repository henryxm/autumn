package cn.org.autumn;

import cn.org.autumn.site.TemplateFactory;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAspectJAutoProxy(proxyTargetClass = true, exposeProxy = true)
@ServletComponentScan
@EnableAsync
@SpringBootApplication
@MapperScan(basePackages = {"cn.org.autumn.modules.*.dao", "cn.org.autumn.table.dao",})
public class AutumnApplication extends SpringBootServletInitializer implements TemplateFactory.Template {
    public static void main(String[] args) {
        new SpringApplication(AutumnApplication.class).run(args);
    }
}
