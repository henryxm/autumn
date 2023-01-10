package cn.org.autumn.modules.sys.entity;

import cn.org.autumn.annotation.ConfigParam;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class CategoryItem implements Serializable {

    private static final long serialVersionUID = 1L;
    private String paramKey;
    private String category;
    private String name;
    private String description;
    private List<ConfigItem> configs = new ArrayList<>();

    public CategoryItem() {
    }

    public CategoryItem(SysCategoryEntity entity) {
        this.category = entity.getCategory();
        this.name = entity.getName();
        this.description = entity.getDescription();
    }

    public CategoryItem(ConfigParam configParam) {
        this.paramKey = configParam.paramKey();
        this.category = configParam.category();
        this.name = configParam.name();
        this.description = configParam.description();
    }
    public String getParamKey() {
        return paramKey;
    }

    public void setParamKey(String paramKey) {
        this.paramKey = paramKey;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<ConfigItem> getConfigs() {
        return configs;
    }

    public void setConfigs(List<ConfigItem> configs) {
        this.configs = configs;
    }
}