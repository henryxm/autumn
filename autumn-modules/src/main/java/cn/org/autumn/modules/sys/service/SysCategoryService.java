package cn.org.autumn.modules.sys.service;

import cn.org.autumn.annotation.ConfigField;
import cn.org.autumn.annotation.ConfigParam;
import cn.org.autumn.base.ModuleService;
import cn.org.autumn.config.CategoryHandler;
import cn.org.autumn.config.InputType;
import cn.org.autumn.modules.lan.service.LanguageService;
import cn.org.autumn.modules.sys.entity.CategoryItem;
import cn.org.autumn.modules.sys.entity.ConfigItem;
import cn.org.autumn.modules.sys.entity.SysConfigEntity;
import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import cn.org.autumn.modules.sys.dao.SysCategoryDao;
import cn.org.autumn.modules.sys.entity.SysCategoryEntity;

import java.lang.reflect.Field;
import java.util.*;

import static cn.org.autumn.modules.sys.service.SysConfigService.json_type;

@Service
public class SysCategoryService extends ModuleService<SysCategoryDao, SysCategoryEntity> implements CategoryHandler {

    public Logger log = LoggerFactory.getLogger(getClass());

    public static final String default_config = "default";
    public static final String storage_config = "storage_config";

    @Autowired
    SysConfigService sysConfigService;

    @Autowired
    LanguageService languageService;

    @Autowired
    List<CategoryHandler> categoryHandlerList;

    @Override
    public void init() {
        language.put(getLanguageItemsInternal(), getLanguageItems(), getLanguageList());
        for (CategoryHandler categoryHandler : categoryHandlerList) {
            put(categoryHandler.getCategoryItems());
        }
        put(getCategoryItems());
    }

    public String[][] getLanguageItems() {
        String[][] items = new String[][]{
                {categoryName(default_config), "默认分类", "Default classification"},
                {categoryDescription(default_config), "默认分类配置项，未进行分类的配置进入默认分类", "Default classification configuration items, unclassified configurations enter the default classification"},
                {categoryName(storage_config), "云存储配置", "Cloud Storage Configuration"},
                {categoryDescription(storage_config), "配置公有云存储相关信息", "Configure public cloud storage related information"},
        };
        return items;
    }

    public boolean has(String category) {
        return baseMapper.has(category) > 0;
    }

    public SysCategoryEntity getByCategory(String category, String language) {
        SysCategoryEntity categoryEntity = baseMapper.getByCategory(category);
        if (null == categoryEntity)
            return null;
        if (StringUtils.isNotBlank(language)) {
            Map<String, String> lMap = languageService.getLanguage(language);
            if (null != lMap) {
                if (StringUtils.isNotBlank(categoryEntity.getName()) && categoryEntity.getName().startsWith(category_lang_string)) {
                    String name = lMap.get(categoryEntity.getName());
                    if (StringUtils.isNotBlank(name)) {
                        categoryEntity.setName(name);
                    }
                }
                if (StringUtils.isNotBlank(categoryEntity.getDescription()) && categoryEntity.getDescription().startsWith(category_lang_string)) {
                    String description = lMap.get(categoryEntity.getDescription());
                    if (StringUtils.isNotBlank(description)) {
                        categoryEntity.setDescription(description);
                    }
                }
            }
        }
        return categoryEntity;
    }

    public String[][] getCategoryItems() {
        String[][] mapping = new String[][]{
                {default_config, "1"},
                {storage_config, "1"},
        };
        return mapping;
    }

    public void put(String[][] items) {
        if (null == items)
            return;
        for (String[] item : items) {
            String category = item[0];
            save(category, categoryName(category), Integer.parseInt(item[1]), categoryDescription(category));
        }
    }

    public synchronized void save(String category, String name, int status, String description) {
        SysCategoryEntity categoryEntity = getByCategory(category, null);
        if (null == categoryEntity) {
            categoryEntity = new SysCategoryEntity();
        } else {
            if (categoryEntity.isFrozen())
                return;
        }
        categoryEntity.setCategory(category);
        categoryEntity.setStatus(status);
        categoryEntity.setName(name);
        categoryEntity.setFrozen(false);
        categoryEntity.setDescription(description);
        saveOrUpdate(categoryEntity);
    }


    public Map<String, CategoryItem> reverse(Class<?> clazz, String fieldName, Object obj, String language) {
        try {
            ConfigParam configParam = clazz.getAnnotation(ConfigParam.class);
            Map<String, CategoryItem> map = new HashMap<>();
            if (null != configParam) {
                Field[] fields = clazz.getDeclaredFields();
                for (Field field : fields) {
                    field.setAccessible(true);
                    ConfigField configField = field.getDeclaredAnnotation(ConfigField.class);
                    if (null != configField) {
                        Object value = field.get(obj);
                        String category = configParam.category();
                        if (StringUtils.isBlank(category))
                            category = default_config;
                        CategoryItem categoryItem = map.get(category);
                        if (null != categoryItem) {
                            ConfigItem configItem = new ConfigItem(configParam, configField, fieldName, field, value);
                            configItem.setName(translate(language, configItem.getName()));
                            configItem.setDescription(translate(language, configItem.getDescription()));
                            categoryItem.getConfigs().add(configItem);
                        } else {
                            SysCategoryEntity sysCategoryEntity = getByCategory(category, language);
                            if (null != sysCategoryEntity && sysCategoryEntity.getStatus() > 0) {
                                categoryItem = new CategoryItem(configParam);
                                categoryItem.setName(translate(language, categoryItem.getName()));
                                categoryItem.setDescription(translate(language, categoryItem.getDescription()));
                                ConfigItem configItem = new ConfigItem(configParam, configField, fieldName, field, value);
                                configItem.setName(translate(language, configItem.getName()));
                                configItem.setDescription(translate(language, configItem.getDescription()));
                                categoryItem.getConfigs().add(configItem);
                                map.put(category, categoryItem);
                            }
                        }
                    } else {
                        ConfigParam nestParam = field.getDeclaredAnnotation(ConfigParam.class);
                        if (null != nestParam) {
                            Class<?> nestClazz = field.getType();
                            String fn = field.getName();
                            if (StringUtils.isNotBlank(fieldName)) {
                                fn = fieldName + "." + fn;
                            }
                            map.putAll(reverse(nestClazz, fn, field.get(obj), language));
                        }
                    }
                }
            }
            return map;
        } catch (IllegalAccessException e) {
            log.error("非法访问:{}", clazz.getName());
        }
        return null;
    }

    public Map<String, CategoryItem> category(SysConfigEntity sysConfigEntity, String language) {
        try {
            if (sysConfigEntity.getType().equals(InputType.JsonType.getValue()) && StringUtils.isNotBlank(sysConfigEntity.getOptions())) {
                Class<?> clazz = Class.forName(sysConfigEntity.getOptions());
                Object o = new Gson().fromJson(sysConfigEntity.getParamValue(), clazz);
                return reverse(clazz, "", o, language);
            }
        } catch (ClassNotFoundException e) {
            log.error("类不存在:{}", sysConfigEntity.getOptions());
        }
        return null;
    }

    public String translate(String language, String key) {
        Map<String, String> map = languageService.getLanguage(language);
        if (StringUtils.isNotBlank(key) && (key.startsWith(SysConfigService.config_lang_prefix) || key.startsWith(category_lang_string)) && map.containsKey(key)) {
            return map.get(key);
        }
        return key;
    }

    /**
     * 获取配置
     *
     * @param language
     * @param key      paramKey or category
     * @return
     */
    public Map<String, CategoryItem> getCategories(String language, String key) {
        List<SysConfigEntity> sysConfigEntities = null;
        SysCategoryEntity categoryEntity = null;
        if (StringUtils.isNotBlank(key)) {
            SysConfigEntity config = sysConfigService.getByKey(key);
            if (null != config)
                sysConfigEntities = Collections.singletonList(config);
            else {
                categoryEntity = getByCategory(key, language);
                if (null != categoryEntity) {
                    sysConfigEntities = sysConfigService.listByMap(null);
                }
            }
        } else
            sysConfigEntities = sysConfigService.listByMap(null);
        Map<String, CategoryItem> map = new LinkedTreeMap<>();
        if (null == sysConfigEntities)
            return map;
        for (SysConfigEntity sysConfigEntity : sysConfigEntities) {
            if (sysConfigEntity.getStatus() <= 0)
                continue;
            if (sysConfigEntity.getType().equals(json_type)) {
                Map<String, CategoryItem> nn = category(sysConfigEntity, language);
                if (null != nn) {
                    for (Map.Entry<String, CategoryItem> entry : nn.entrySet()) {
                        if (map.containsKey(entry.getKey())) {
                            CategoryItem item = map.get(entry.getKey());
                            if (!entry.getValue().getConfigs().isEmpty())
                                item.getConfigs().addAll(entry.getValue().getConfigs());
                        } else {
                            map.put(entry.getKey(), entry.getValue());
                        }
                    }
                    continue;
                }
            }
            Map<String, String> lMap = languageService.getLanguage(language);
            if (null != lMap) {
                if (StringUtils.isNotBlank(sysConfigEntity.getName()) && sysConfigEntity.getName().startsWith(SysConfigService.config_lang_prefix)) {
                    String name = lMap.get(sysConfigEntity.getName());
                    if (StringUtils.isNotBlank(name)) {
                        sysConfigEntity.setName(name);
                    }
                }
                if (StringUtils.isNotBlank(sysConfigEntity.getDescription()) && sysConfigEntity.getDescription().startsWith(SysConfigService.config_lang_prefix)) {
                    String description = lMap.get(sysConfigEntity.getDescription());
                    if (StringUtils.isNotBlank(description)) {
                        sysConfigEntity.setDescription(description);
                    } else
                        sysConfigEntity.setDescription(sysConfigEntity.getRemark());
                }
            }
            String category = sysConfigEntity.getCategory();
            if (StringUtils.isBlank(category))
                category = default_config;
            CategoryItem categoryItem = map.get(category);
            if (null != categoryItem) {
                categoryItem.getConfigs().add(new ConfigItem(sysConfigEntity));
            } else {
                SysCategoryEntity sysCategoryEntity = getByCategory(category, language);
                if (null != sysCategoryEntity && sysCategoryEntity.getStatus() > 0) {
                    categoryItem = new CategoryItem(sysCategoryEntity);
                    categoryItem.getConfigs().add(new ConfigItem(sysConfigEntity));
                    map.put(category, categoryItem);
                }
            }
        }

        if (null != categoryEntity) {
            CategoryItem item = map.get(categoryEntity.getCategory());
            if (null != item) {
                map.clear();
                map.put(item.getCategory(), item);
            }
        }

        List<Map.Entry<String, CategoryItem>> oList = new ArrayList<>(map.entrySet());
        oList.sort(new Comparator<Map.Entry<String, CategoryItem>>() {
            @Override
            public int compare(Map.Entry<String, CategoryItem> o1, Map.Entry<String, CategoryItem> o2) {
                return o2.getValue().getOrder() - o1.getValue().getOrder();
            }
        });
        map.clear();
        for (Map.Entry<String, CategoryItem> entry : oList) {
            entry.getValue().getConfigs().sort(new Comparator<ConfigItem>() {
                @Override
                public int compare(ConfigItem o1, ConfigItem o2) {
                    return o2.getOrder() - o1.getOrder();
                }
            });
            map.put(entry.getKey(), entry.getValue());
        }
        return map;
    }
}