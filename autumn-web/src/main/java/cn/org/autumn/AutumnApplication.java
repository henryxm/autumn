package cn.org.autumn;

import cn.org.autumn.loader.LoaderFactory;
import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.TemplateLoader;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;


@SpringBootApplication
@MapperScan(basePackages = {"cn.org.autumn.modules.*.dao", "cn.org.autumn.table.dao",})
public class AutumnApplication extends SpringBootServletInitializer implements LoaderFactory.Loader {

    public static void main(String[] args) {
        SpringApplication springApplication = new SpringApplication(AutumnApplication.class);
        ConfigurableApplicationContext configurableApplicationContext = springApplication.run(args);
    }

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(AutumnApplication.class);
    }

    /**
     * 自定义异步线程池
     *
     * @return
     */
    @Bean
    public AsyncTaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("Anno-Executor");
        executor.setMaxPoolSize(50000);
        executor.setCorePoolSize(1000);

        // 设置拒绝策略
        executor.setRejectedExecutionHandler(new RejectedExecutionHandler() {
            @Override
            public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                // .....
            }
        });
        // 使用预定义的异常处理类
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        return executor;
    }

    @Override
    public TemplateLoader get() {
        TemplateLoader loader = new ClassTemplateLoader(this.getClass(), "/templates");
        return loader;
    }
}
