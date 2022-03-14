package cn.org.autumn.loader;

import freemarker.cache.MultiTemplateLoader;
import freemarker.cache.StatefulTemplateLoader;
import freemarker.cache.TemplateLoader;

import java.io.IOException;
import java.io.Reader;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DynamicTemplateLoader extends MultiTemplateLoader {

    private final Map<String, TemplateLoader> templateLoaders = new ConcurrentHashMap();

    private final Map<String, TemplateLoader> lastTemplateLoaderForName = new ConcurrentHashMap();
    private boolean sticky = true;

    public DynamicTemplateLoader(TemplateLoader[] templateLoaders) {
        super(templateLoaders);
    }

    public void add(TemplateLoader templateLoader) {
        String name = templateLoader.getClass().getName();
        if (!templateLoaders.containsKey(name))
            templateLoaders.put(name, templateLoader);
    }

    @Override
    public Object findTemplateSource(String name) throws IOException {
        Object source = super.findTemplateSource(name);
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
            if (!(o instanceof DynamicTemplateLoader.DynamicSource)) {
                return false;
            } else {
                DynamicTemplateLoader.DynamicSource m = (DynamicTemplateLoader.DynamicSource) o;
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