package cn.org.autumn.config;

import cn.org.autumn.site.ViewFactory;
import cn.org.autumn.view.NameBasedViewResolver;
import freemarker.cache.TemplateLoader;
import freemarker.template.TemplateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.freemarker.FreeMarkerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer;
import org.springframework.web.servlet.view.freemarker.FreeMarkerViewResolver;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

@Configuration
public class FreemarkerConfig {
    Logger log = LoggerFactory.getLogger(getClass());

    @Bean
    public FreeMarkerConfigurer freeMarkerConfigurer(List<VariablesHandler> variablesHandlers, TemplateLoader templateLoader) {
        FreeMarkerConfigurer configurer = new FreeMarkerConfigurer();
        Map<String, Object> variables = new HashMap<>();
        if (null != variablesHandlers && variablesHandlers.size() > 0) {
            for (VariablesHandler variablesHandler : variablesHandlers) {
                variables.put(variablesHandler.getName(), variablesHandler);
            }
        }
        configurer.setFreemarkerVariables(variables);
        Properties settings = new Properties();
        settings.setProperty("default_encoding", "utf-8");
        settings.setProperty("number_format", "0.##");
        configurer.setFreemarkerSettings(settings);
        try {
            freemarker.template.Configuration configuration = null;
            configuration = configurer.createConfiguration();
            configuration.setTemplateLoader(templateLoader);
            configurer.setConfiguration(configuration);
        } catch (IOException | TemplateException e) {
            log.debug("freeMarkerConfigurer", e);
        }
        return configurer;
    }

    @Bean
    FreeMarkerViewResolver freeMarkerViewResolver(FreeMarkerProperties properties, ViewFactory viewFactory) {
        FreeMarkerViewResolver freeMarkerViewResolver = new NameBasedViewResolver(viewFactory);
        properties.applyToMvcViewResolver(freeMarkerViewResolver);
        return freeMarkerViewResolver;
    }
}
