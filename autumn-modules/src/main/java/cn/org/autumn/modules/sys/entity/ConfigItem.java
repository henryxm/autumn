package cn.org.autumn.modules.sys.entity;

import cn.org.autumn.utils.Utils;
import org.apache.commons.lang.StringUtils;

import java.io.Serializable;
import java.util.Objects;

import static cn.org.autumn.modules.sys.service.SysConfigService.boolean_type;
import static cn.org.autumn.modules.sys.service.SysConfigService.selection_type;

public class ConfigItem implements Serializable {

    private static final long serialVersionUID = 1L;

    private String paramKey;

    private Object paramValue;

    private String type;

    private String category;

    private String name;

    private String description;

    private Object options;

    public ConfigItem() {
    }

    public ConfigItem(SysConfigEntity entity) {
        this.paramKey = entity.getParamKey();
        this.type = entity.getType();
        this.category = entity.getCategory();
        this.name = entity.getName();
        this.description = entity.getDescription();
        if (Objects.equals(this.type, boolean_type)) {
            this.paramValue = Utils.parseBoolean(entity.getParamValue());
        } else {
            this.paramValue = entity.getParamValue();
        }
        if (Objects.equals(this.type, selection_type) && StringUtils.isNotBlank(entity.getOptions())) {
            String options = entity.getOptions();
            this.options = options.split(",");
        } else
            this.options = entity.getOptions();
    }

    public String getParamKey() {
        return paramKey;
    }

    public void setParamKey(String paramKey) {
        this.paramKey = paramKey;
    }

    public Object getParamValue() {
        return paramValue;
    }

    public void setParamValue(Object paramValue) {
        this.paramValue = paramValue;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
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

    public Object getOptions() {
        return options;
    }

    public void setOptions(Object options) {
        this.options = options;
    }
}
