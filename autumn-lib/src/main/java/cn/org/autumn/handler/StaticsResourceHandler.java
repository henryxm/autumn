package cn.org.autumn.handler;

import cn.org.autumn.config.ResourceHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;

@Component
public class StaticsResourceHandler implements ResourceHandler {
    @Override
    public void apply(ResourceHandlerRegistry registry) {
        if (!registry.hasMappingForPattern("/statics/**"))
            registry.addResourceHandler("/statics/**").addResourceLocations("classpath:/statics/");
    }
}