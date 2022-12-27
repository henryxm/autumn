package cn.org.autumn.modules.sys.entity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class CategoryItem implements Serializable {

    private static final long serialVersionUID = 1L;
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