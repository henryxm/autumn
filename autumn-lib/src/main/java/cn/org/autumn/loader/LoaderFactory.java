package cn.org.autumn.loader;

import cn.org.autumn.utils.SpringContextUtils;
import freemarker.cache.MultiTemplateLoader;
import freemarker.cache.TemplateLoader;
import org.springframework.context.ApplicationContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class LoaderFactory {
    private LoaderFactory() {
    }

    /**
     * 使用TemplateFactory.Template 代替 LoaderFactory.Loader
     *
     * @see cn.org.autumn.site.TemplateFactory
     */
    @Deprecated
    public interface Loader {
        @Deprecated
        TemplateLoader get();
    }

    public static TemplateLoader getTemplateLoader() {
        List<TemplateLoader> templateLoaders = new ArrayList<>();
        ApplicationContext applicationContext = SpringContextUtils.getApplicationContext();
        Map<String, Loader> map = applicationContext.getBeansOfType(Loader.class);
        for (Loader loader : map.values()) {
            templateLoaders.add(loader.get());
        }
        TemplateLoader[] templateLoaders1 = new TemplateLoader[templateLoaders.size()];
        templateLoaders.toArray(templateLoaders1);
        DynamicTemplateLoader multiTemplateLoader = new DynamicTemplateLoader(templateLoaders1);
        return multiTemplateLoader;
    }
}
