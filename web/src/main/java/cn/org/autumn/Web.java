package cn.org.autumn;

import cn.org.autumn.boot.AutumnSpringApplication;
import cn.org.autumn.site.TemplateFactory;
import org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAspectJAutoProxy(proxyTargetClass = true, exposeProxy = true)
@ServletComponentScan
@EnableAsync
@SpringBootApplication(exclude = {MybatisAutoConfiguration.class})
public class Web extends SpringBootServletInitializer implements TemplateFactory.Template {
    public static void main(String[] args) {
        AutumnSpringApplication.run(Web.class, args);
    }
}
