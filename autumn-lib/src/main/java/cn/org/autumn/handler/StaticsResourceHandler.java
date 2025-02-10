package cn.org.autumn.handler;

import cn.org.autumn.config.Config;
import cn.org.autumn.config.ResourceHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;

import java.io.File;

@Component
public class StaticsResourceHandler implements ResourceHandler {

    public String combiner(boolean local) {
        if (Config.windows() && local)
            return "\\";
        else
            return "/";
    }

    @Override
    public void apply(ResourceHandlerRegistry registry) {
        if (!registry.hasMappingForPattern("/statics/**"))
            registry.addResourceHandler("/statics/**").addResourceLocations("classpath:/statics/");
        if (!registry.hasMappingForPattern("/files/**")) {
            String home = System.getProperty("user.home");
            String combiner = combiner(true);
            if (!home.endsWith(combiner))
                home = home + combiner;
            home = home + "files" + combiner;
            File file = new File(home);
            if (!file.exists())
                file.mkdir();
            registry.addResourceHandler("/files/**").addResourceLocations("file:" + home);
        }
    }
}