package cn.org.autumn.plugin;

import java.io.Serializable;
import java.util.List;

public class PluginEntry implements Serializable {
    private static final long serialVersionUID = 1L;

    private String url;

    private List<String> classes;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public List<String> getClasses() {
        return classes;
    }

    public void setClasses(List<String> classes) {
        this.classes = classes;
    }
}