package cn.org.autumn.site;

import cn.org.autumn.loader.DynamicTemplateLoader;
import cn.org.autumn.loader.LoaderFactory;
import cn.org.autumn.utils.SpringContextUtils;
import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.TemplateLoader;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 装载资源模板，每个artifact仅需一个类实现该接口即可
 * 已定义默认实现
 */
@Configuration
public class TemplateFactory extends Factory {

    @Order(DEFAULT_ORDER)
    public interface Template {
        default TemplateLoader getTemplateLoader() {
            String basePath = getBasePackagePath();
            if (!basePath.startsWith("/"))
                basePath = "/" + basePath;
            Class<?> clazz = getClass();
            return new ClassTemplateLoader(clazz, basePath);
        }

        default String getBasePackagePath() {
            return "/templates";
        }
    }

    @Bean
    public TemplateLoader getTemplateLoader() {
        List<TemplateLoader> templateLoaders = new ArrayList<>();
        Map<Integer, List<Template>> map = getOrdered(Template.class, "getTemplateLoader");
        if (null != map && map.size() > 0) {
            for (Map.Entry<Integer, List<Template>> entry : map.entrySet()) {
                List<Template> templates = entry.getValue();
                for (Template template : templates) {
                    templateLoaders.add(template.getTemplateLoader());
                }
            }
        }

        ApplicationContext applicationContext = SpringContextUtils.getApplicationContext();
        Map<String, LoaderFactory.Loader> map1 = applicationContext.getBeansOfType(LoaderFactory.Loader.class);
        for (LoaderFactory.Loader loader : map1.values()) {
            templateLoaders.add(loader.get());
        }

        TemplateLoader[] templateLoaders1 = new TemplateLoader[templateLoaders.size()];
        templateLoaders.toArray(templateLoaders1);
        return new DynamicTemplateLoader(templateLoaders1);
    }
}