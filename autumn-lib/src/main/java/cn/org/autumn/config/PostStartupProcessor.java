package cn.org.autumn.config;

import cn.org.autumn.site.InitFactory;
import cn.org.autumn.site.LoadFactory;
import cn.org.autumn.site.UpgradeFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class PostStartupProcessor implements ApplicationListener<ContextRefreshedEvent> {
    @Autowired
    LoadFactory loadFactory;

    @Autowired
    InitFactory initFactory;

    @Autowired
    UpgradeFactory upgradeFactory;

    @Autowired
    AsyncTaskExecutor asyncTaskExecutor;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        ApplicationContext context = (ApplicationContext) event.getSource();
        ApplicationContext existing = Config.getInstance().getApplicationContext();
        if (existing != null) {
            if (existing == context) {
                return;
            }
            if (existing instanceof ConfigurableApplicationContext
                    && !((ConfigurableApplicationContext) existing).isActive()) {
                Config.getInstance().setApplicationContext(null);
            } else {
                return;
            }
        }
        Config.getInstance().setApplicationContext(context);
        Config.getInstance().setEnv(context.getEnvironment());

        event.getApplicationContext().getParent();
    }

    @Bean
    public CommandLineRunner run() {
        return args -> log.debug("Processing command line arguments.");
    }
}
