package cn.org.autumn.modules.gen.entity;

import java.util.LinkedHashMap;
import java.util.Map;

public class GenTypeWrapper {
    private GenTypeEntity entity;

    public GenTypeWrapper(GenTypeEntity entity) {
        this.mapping = new LinkedHashMap<>();
        this.entity = entity;
        if (null == entity)
            return;
        String m = this.entity.getMappingString();
        String[] t = m.split(",");
        for (String p : t) {
            String[] a = p.split("=");
            mapping.put(a[0], a[1]);
        }
    }

    public String getModuleText() {
        return entity.getModuleText();
    }

    public String getDatabaseType() {
        return entity.getDatabaseType();
    }

    public String getRootPackage() {
        return entity.getRootPackage();
    }

    public String getModulePackage() {
        return entity.getModulePackage();
    }

    public String getModuleName() {
        return entity.getModuleName();
    }

    public String getAuthorName() {
        return entity.getAuthorName();
    }

    public String getEmail() {
        return entity.getEmail();
    }

    public String getTablePrefix() {
        return entity.getTablePrefix();
    }

    public String getMappingString() {
        return entity.getMappingString();
    }

    private Map<String, String> mapping;

    public Map<String, String> getMapping() {
        return mapping;
    }

    public void setMapping(Map<String, String> mapping) {
        this.mapping = mapping;
    }

    public GenTypeEntity getEntity() {
        return entity;
    }

    public String getModuleId() {
        return entity.getModuleId();
    }
}
