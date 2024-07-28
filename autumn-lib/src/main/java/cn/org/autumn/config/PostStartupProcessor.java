package cn.org.autumn.config;

import cn.org.autumn.site.InitFactory;
import cn.org.autumn.site.LoadFactory;
import cn.org.autumn.site.UpgradeFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Component;

@Component
public class PostStartupProcessor implements ApplicationListener<ContextRefreshedEvent> {

    private static final Logger log = LoggerFactory.getLogger(PostStartupProcessor.class);

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
        if (null != Config.getInstance().getApplicationContext())
            return;
        Config.getInstance().setApplicationContext(context);
        String[] profiles = context.getEnvironment().getActiveProfiles();

        if (profiles.length > 0) {
            Config.getInstance().setEnv(context.getEnvironment());
        }

        if (event.getApplicationContext().getParent() == null) {

        }
    }

    @Bean
    public CommandLineRunner run() {
        return args -> {
            log.debug("Processing command line arguments.");
        };
    }
}
