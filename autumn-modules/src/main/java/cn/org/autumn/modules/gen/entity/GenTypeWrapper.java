package cn.org.autumn.modules.gen.entity;

import cn.org.autumn.modules.sys.entity.SysMenuEntity;
import org.apache.commons.lang.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;

public class GenTypeWrapper {
    private final GenTypeEntity entity;
    private final SysMenuEntity menuEntity;

    public GenTypeWrapper(GenTypeEntity entity, SysMenuEntity sysMenuEntity) {
        this.mapping = new LinkedHashMap<>();
        this.entity = entity;
        this.menuEntity = sysMenuEntity;
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
        String text = entity.getModuleText();
        if (StringUtils.isBlank(text))
            text = "";
        return text;
    }

    public String getModuleIcon() {
        String ico = null;
        if (null != menuEntity) {
            ico = menuEntity.getIcon();
            while (StringUtils.isNotBlank(ico) && ico.startsWith("fa "))
                ico = ico.substring(2).trim();
        }
        if (StringUtils.isBlank(ico))
            ico = "fa-file-word-o";
        return ico;
    }

    public String getModuleOrder() {
        if (null != menuEntity && null != menuEntity.getOrderNum())
            return String.valueOf(menuEntity.getOrderNum());
        return "0";
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