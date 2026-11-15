package cn.org.autumn.config;

import cn.org.autumn.site.ViewFactory;
import cn.org.autumn.view.NameBasedViewResolver;
import cn.org.autumn.view.ViewTemplateSupport;
import freemarker.cache.TemplateLoader;
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

    /**
     * FreeMarker 配置。
     * <p>
     * 故意不设置全局 {@code output_format=HTML} / auto-escaping：存量模板大量使用 {@code ?html}，
     * 与 Markup 自动转义互斥（会 ParseException）。XSS 仍依赖模板显式 {@code ?html} 等；
     * 若单页需要自动转义，可在该模板写 {@code <#ftl output_format="HTML">}（且勿再写 {@code ?html}）。
     */
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
        configurer.setFreemarkerSettings(settings);
        try {
            // 与 Spring @Configuration 同名，此处用 FQN
            freemarker.template.Configuration configuration = configurer.createConfiguration();
            configuration.setTemplateLoader(templateLoader);
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
