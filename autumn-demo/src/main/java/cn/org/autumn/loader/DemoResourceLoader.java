package cn.org.autumn.loader;

import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.TemplateLoader;
import org.springframework.stereotype.Component;

@Component
public class DemoResourceLoader implements LoaderFactory.Loader {
    @Override
    public TemplateLoader get() {
        TemplateLoader loader = new ClassTemplateLoader(this.getClass(), "/templates");
        return loader;
    }
}
