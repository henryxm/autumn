package cn.org.autumn.plugin;

import cn.org.autumn.config.Config;
import cn.org.autumn.loader.DynamicTemplateLoader;
import cn.org.autumn.site.TemplateFactory;
import freemarker.cache.TemplateLoader;

public interface Plugin extends TemplateFactory.Template {

    default String version() {
        return "1.0.0";
    }

    default String name() {
        return "Plugin Name";
    }

    PluginEntry entry();

    default void install() {
        TemplateLoader templateLoader = (TemplateLoader) Config.getBean(TemplateLoader.class);
        if (templateLoader instanceof DynamicTemplateLoader) {
            DynamicTemplateLoader dynamicTemplateLoader = (DynamicTemplateLoader) templateLoader;
            dynamicTemplateLoader.add(this.getTemplateLoader());
        }
    }

    default void uninstall() {
        TemplateLoader templateLoader = (TemplateLoader) Config.getBean(TemplateLoader.class);
        if (templateLoader instanceof DynamicTemplateLoader) {
            DynamicTemplateLoader dynamicTemplateLoader = (DynamicTemplateLoader) templateLoader;
            dynamicTemplateLoader.remove(this.getTemplateLoader());
            dynamicTemplateLoader.resetState();
        }
    }
}
