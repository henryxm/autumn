package cn.org.autumn.loader;

import cn.org.autumn.plugin.PluginManager;
import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.MultiTemplateLoader;
import freemarker.cache.StatefulTemplateLoader;
import freemarker.cache.TemplateLoader;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class DynamicTemplateLoader extends MultiTemplateLoader {

    private final Map<String, TemplateLoader> templateLoaders = new ConcurrentHashMap<>();

    private final Map<String, TemplateLoader> lastTemplateLoaderForName = new ConcurrentHashMap<>();
    private boolean sticky = true;

    public DynamicTemplateLoader(TemplateLoader[] templateLoaders) {
        super(templateLoaders);
    }

    public void add(TemplateLoader templateLoader) {
        ClassTemplateLoader classTemplateLoader = (ClassTemplateLoader) templateLoader;
        String name = classTemplateLoader.getResourceLoaderClass().getName();
        if (!templateLoaders.containsKey(name))
            templateLoaders.put(name, templateLoader);
    }

    public void remove(TemplateLoader templateLoader) {
        ClassTemplateLoader classTemplateLoader = (ClassTemplateLoader) templateLoader;
        String name = classTemplateLoader.getResourceLoaderClass().getName();
        templateLoaders.remove(name);
    }

    public boolean exists(Object obj) {
        try {
            if (null != obj) {
                if (obj.getClass().getSimpleName().equals("MultiSource")) {
                    Field field = obj.getClass().getDeclaredField("source");
                    field.setAccessible(true);
                    Object source = field.get(obj);
                    if (source.getClass().getSimpleName().equals("URLTemplateSource")) {
                        Field urlField = source.getClass().getDeclaredField("url");
                        urlField.setAccessible(true);
                        Object url = urlField.get(source);
                        if (url instanceof URL a) {
                            if (a.getFile().startsWith("file:")) {
                                String file = a.getFile().replace("file:", "");
                                if (file.startsWith(PluginManager.getPluginBaseDir()) && file.contains("!")) {
                                    file = file.split("!")[0];
                                    File f = new File(file);
                                    return f.exists();
                                }
                            }
                        }
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return true;
    }

    @Override
    public Object findTemplateSource(String name) throws IOException {
        Object source = super.findTemplateSource(name);
        if (!exists(source))
            return null;
        if (null == source) {
            TemplateLoader lastTemplateLoader = null;
            if (this.sticky) {
                lastTemplateLoader = this.lastTemplateLoaderForName.get(name);
                if (lastTemplateLoader != null) {
                    source = lastTemplateLoader.findTemplateSource(name);
                    if (source != null) {
                        return new DynamicTemplateLoader.DynamicSource(source, lastTemplateLoader);
                    }
                }
            }
            for (TemplateLoader templateLoader : templateLoaders.values()) {
                if (lastTemplateLoader != templateLoader) {
                    source = templateLoader.findTemplateSource(name);
                    if (source != null) {
                        if (this.sticky) {
                            this.lastTemplateLoaderForName.put(name, templateLoader);
                        }
                        source = new DynamicSource(source, templateLoader);
                        break;
                    }
                }
            }
            if (this.sticky) {
                this.lastTemplateLoaderForName.remove(name);
            }
        }
        return source;
    }

    public Reader getReader(Object templateSource, String encoding) throws IOException {
        if (templateSource.getClass().getSimpleName().contains("MultiSource"))
            return super.getReader(templateSource, encoding);
        return ((DynamicTemplateLoader.DynamicSource) templateSource).getReader(encoding);
    }

    @Override
    public void closeTemplateSource(Object templateSource) throws IOException {
        if (templateSource.getClass().getSimpleName().contains("MultiSource"))
            super.closeTemplateSource(templateSource);
        else {
            if (templateSource instanceof DynamicTemplateLoader.DynamicSource) {
                ((DynamicTemplateLoader.DynamicSource) templateSource).close();
            }
        }
    }

    @Override
    public long getLastModified(Object templateSource) {
        if (templateSource.getClass().getSimpleName().contains("MultiSource"))
            return super.getLastModified(templateSource);
        else {
            return ((DynamicTemplateLoader.DynamicSource) templateSource).getLastModified();
        }
    }

    public void resetState() {
        super.resetState();
        this.lastTemplateLoaderForName.clear();
        for (TemplateLoader templateLoader : templateLoaders.values()) {
            if (templateLoader instanceof StatefulTemplateLoader) {
                ((StatefulTemplateLoader) templateLoader).resetState();
            }
        }
    }

    public int getTemplateLoaderCount() {
        return this.templateLoaders.size();
    }

    public boolean isSticky() {
        return this.sticky;
    }

    public void setSticky(boolean sticky) {
        this.sticky = sticky;
    }

    static final class DynamicSource {
        private final Object source;
        private final TemplateLoader loader;

        DynamicSource(Object source, TemplateLoader loader) {
            this.source = source;
            this.loader = loader;
        }

        long getLastModified() {
            return this.loader.getLastModified(this.source);
        }

        Reader getReader(String encoding) throws IOException {
            return this.loader.getReader(this.source, encoding);
        }

        void close() throws IOException {
            this.loader.closeTemplateSource(this.source);
        }

        Object getWrappedSource() {
            return this.source;
        }

        public boolean equals(Object o) {
            if (!(o instanceof DynamicSource m)) {
                return false;
            } else {
                return m.loader.equals(this.loader) && m.source.equals(this.source);
            }
        }

        public int hashCode() {
            return this.loader.hashCode() + 31 * this.source.hashCode();
        }

        public String toString() {
            return this.source.toString();
        }
    }
}