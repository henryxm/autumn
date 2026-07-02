package cn.org.autumn.site;

import cn.org.autumn.loader.DynamicTemplateLoader;
import cn.org.autumn.loader.LoaderFactory;
import cn.org.autumn.utils.SpringContextUtils;
import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.TemplateLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

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
        if (null != map && !map.isEmpty()) {
            for (Map.Entry<Integer, List<Template>> entry : map.entrySet()) {
                List<Template> templates = entry.getValue();
                for (Template template : templates) {
                    templateLoaders.add(template.getTemplateLoader());
                }
            }
        }

        ApplicationContext applicationContext = SpringContextUtils.getApplicationContext();
        appendLegacyLoaders(templateLoaders, applicationContext);
        return new DynamicTemplateLoader(templateLoaders.toArray(new TemplateLoader[0]));
    }

    @SuppressWarnings("deprecation")
    private static void appendLegacyLoaders(List<TemplateLoader> templateLoaders, ApplicationContext applicationContext) {
        // Bean 工厂方法执行时 ApplicationContext 可能尚未绑定到 SpringContextUtils，须判空
        if (applicationContext == null) {
            return;
        }
        Map<String, LoaderFactory.Loader> loaders = applicationContext.getBeansOfType(LoaderFactory.Loader.class);
        for (LoaderFactory.Loader loader : loaders.values()) {
            templateLoaders.add(loader.get());
        }
    }
}
