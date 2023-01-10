package cn.org.autumn.modules.sys.entity;

import cn.org.autumn.annotation.ConfigField;
import cn.org.autumn.annotation.ConfigParam;
import cn.org.autumn.utils.Utils;
import org.apache.commons.lang.StringUtils;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.Objects;

import static cn.org.autumn.modules.sys.service.SysConfigService.boolean_type;
import static cn.org.autumn.modules.sys.service.SysConfigService.selection_type;

public class ConfigItem implements Serializable {

    private static final long serialVersionUID = 1L;

    private String paramKey;

    private String fieldName;

    private Object paramValue;

    private String type;

    private String category;

    private String name;

    private String description;

    private Object options;

    private boolean readonly;

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
        this.readonly = entity.isReadonly();
    }

    public ConfigItem(ConfigParam configParam, ConfigField configField, String prefix, Field field, Object paramValue) {
        this.paramKey = configParam.paramKey();
        this.type = configField.category().getValue();
        this.category = configParam.category();
        this.name = configField.name();
        this.description = configField.description();
        this.paramValue = paramValue;
        this.fieldName = field.getName();
        if (StringUtils.isNotBlank(prefix)) {
            this.fieldName = prefix + "." + this.fieldName;
        }
        if (Objects.equals(this.type, selection_type) && StringUtils.isNotBlank(configField.options())) {
            String options = configField.options();
            this.options = options.split(",");
        } else
            this.options = configField.options();
        this.readonly = configField.readonly();
    }

    public String getParamKey() {
        return paramKey;
    }

    public void setParamKey(String paramKey) {
        this.paramKey = paramKey;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
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

    public boolean isReadonly() {
        return readonly;
    }

    public void setReadonly(boolean readonly) {
        this.readonly = readonly;
    }
}
