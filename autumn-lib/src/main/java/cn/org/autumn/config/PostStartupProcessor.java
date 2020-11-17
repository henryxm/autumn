package cn.org.autumn.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

@Component
public class PostStartupProcessor implements ApplicationListener<ContextRefreshedEvent> {

    private static final Logger log = LoggerFactory.getLogger(PostStartupProcessor.class);

    @Autowired
    PostLoadFactory postLoadFactory;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        ApplicationContext context = (ApplicationContext) event.getSource();
        Config.getInstance().setApplicationContext(context);
        String[] profiles = context.getEnvironment().getActiveProfiles();

        if (profiles.length > 0) {
            Config.getInstance().setEnv(context.getEnvironment());
        }

        if (event.getApplicationContext().getParent() == null) {

        }
        postLoadFactory.load();
    }
}
