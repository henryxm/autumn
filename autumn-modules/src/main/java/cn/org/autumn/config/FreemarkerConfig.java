package cn.org.autumn.config;

import cn.org.autumn.loader.LoaderFactory;
import cn.org.autumn.modules.sys.shiro.ShiroTag;
import freemarker.template.TemplateException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

@Configuration
public class FreemarkerConfig {

    @Bean
    public FreeMarkerConfigurer freeMarkerConfigurer(ShiroTag shiroTag) {
        FreeMarkerConfigurer configurer = new FreeMarkerConfigurer();
        Map<String, Object> variables = new HashMap<>(1);
        variables.put("shiro", shiroTag);
        configurer.setFreemarkerVariables(variables);
        Properties settings = new Properties();
        settings.setProperty("default_encoding", "utf-8");
        settings.setProperty("number_format", "0.##");
        configurer.setFreemarkerSettings(settings);
        try {
            freemarker.template.Configuration configuration = null;
            configuration = configurer.createConfiguration();
            configuration.setTemplateLoader(LoaderFactory.getTemplateLoader());
            configurer.setConfiguration(configuration);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (TemplateException e) {
            e.printStackTrace();
        }
        return configurer;
    }

}
