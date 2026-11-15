package cn.org.autumn.config;

import cn.org.autumn.site.ViewFactory;
import cn.org.autumn.view.NameBasedViewResolver;
import cn.org.autumn.view.ViewTemplateSupport;
import freemarker.cache.TemplateLoader;
import freemarker.core.HTMLOutputFormat;
import freemarker.template.TemplateException;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.freemarker.FreeMarkerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer;
import org.springframework.web.servlet.view.freemarker.FreeMarkerViewResolver;

@Slf4j
@Configuration
public class FreemarkerConfig {

    @Bean
    public FreeMarkerConfigurer freeMarkerConfigurer(List<VariablesHandler> variablesHandlers, TemplateLoader templateLoader) {
        FreeMarkerConfigurer configurer = new FreeMarkerConfigurer();
        Map<String, Object> variables = new HashMap<>();
        if (null != variablesHandlers && !variablesHandlers.isEmpty()) {
            for (VariablesHandler variablesHandler : variablesHandlers) {
                variables.put(variablesHandler.getName(), variablesHandler);
            }
        }
        configurer.setFreemarkerVariables(variables);
        Properties settings = new Properties();
        settings.setProperty("default_encoding", "utf-8");
        settings.setProperty("number_format", "0.##");
        // FreeMarker 合法取值：enable_if_default | enable_if_supported | force | disable（勿写 *_true 后缀）
        settings.setProperty("auto_escaping_policy", "enable_if_default");
        settings.setProperty("output_format", "HTML");
        configurer.setFreemarkerSettings(settings);
        try {
            // 与 Spring @Configuration 同名，此处用 FQN
            freemarker.template.Configuration configuration = configurer.createConfiguration();
            configuration.setTemplateLoader(templateLoader);
            // 程序化双保险，避免字符串拼写错误导致启动失败
            configuration.setOutputFormat(HTMLOutputFormat.INSTANCE);
            configuration.setAutoEscapingPolicy(freemarker.template.Configuration.ENABLE_IF_DEFAULT_AUTO_ESCAPING_POLICY);
            configurer.setConfiguration(configuration);
        } catch (IOException | TemplateException e) {
            log.debug("freeMarkerConfigurer", e);
        }
        return configurer;
    }

    @Bean
    FreeMarkerViewResolver freeMarkerViewResolver(FreeMarkerProperties properties, ViewFactory viewFactory, ViewTemplateSupport viewTemplateSupport) {
        NameBasedViewResolver freeMarkerViewResolver = new NameBasedViewResolver(viewFactory);
        // 注入模板校验，与 ViewNameReturnValueHandler 共用 ViewTemplateSupport
        freeMarkerViewResolver.setViewTemplateSupport(viewTemplateSupport);
        properties.applyToMvcViewResolver(freeMarkerViewResolver);
        return freeMarkerViewResolver;
    }
}
