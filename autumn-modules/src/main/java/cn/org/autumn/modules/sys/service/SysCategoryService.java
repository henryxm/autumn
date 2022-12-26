package cn.org.autumn.modules.sys.service;

import cn.org.autumn.base.ModuleService;
import cn.org.autumn.modules.lan.service.LanguageService;
import cn.org.autumn.modules.sys.entity.SysConfigEntity;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import cn.org.autumn.modules.sys.dao.SysCategoryDao;
import cn.org.autumn.modules.sys.entity.SysCategoryEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SysCategoryService extends ModuleService<SysCategoryDao, SysCategoryEntity> {

    public static final String default_category = "default";

    public static final String category_lang_prefix = "category_lang_string_";

    @Autowired
    SysConfigService sysConfigService;

    @Autowired
    LanguageService languageService;

    @Override
    public void init() {
        save(getCategoryItems());
        language.put(getLanguageItemsInternal(), getLanguageItems(), getLanguageList());
    }

    public String[][] getLanguageItems() {
        String[][] items = new String[][]{
                {getCategoryLangKey(default_category), "默认分类", "Default classification"},
                {getDescriptionLangKey(default_category), "默认分类配置项，未进行分类的配置进入默认分类", "Default classification configuration items, unclassified configurations enter the default classification"},
        };
        return items;
    }

    public String getCategoryLangKey(String category) {
        return category_lang_prefix + category + "_category";
    }

    public String getDescriptionLangKey(String category) {
        return category_lang_prefix + category + "_description";
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
                if (StringUtils.isNotBlank(categoryEntity.getName()) && categoryEntity.getName().startsWith(category_lang_prefix)) {
                    String name = lMap.get(categoryEntity.getName());
                    if (StringUtils.isNotBlank(name)) {
                        categoryEntity.setName(name);
                    }
                }
                if (StringUtils.isNotBlank(categoryEntity.getDescription()) && categoryEntity.getDescription().startsWith(category_lang_prefix)) {
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
                {default_category, "1"},
        };
        return mapping;
    }

    public void save(String[][] items) {
        for (String[] item : items) {
            String category = item[0];
            save(category, getCategoryLangKey(category), Integer.parseInt(item[1]), getDescriptionLangKey(category));
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
        insertOrUpdate(categoryEntity);
    }

    public Map<String, SysCategoryEntity> getCategories(String language) {
        List<SysConfigEntity> sysConfigEntities = sysConfigService.selectByMap(null);
        Map<String, SysCategoryEntity> map = new HashMap<>();
        for (SysConfigEntity sysConfigEntity : sysConfigEntities) {
            if (sysConfigEntity.getStatus() <= 0)
                continue;
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
                category = default_category;
            SysCategoryEntity categoryEntity = map.get(category);
            if (null != categoryEntity) {
                categoryEntity.getConfigs().add(sysConfigEntity);
            } else {
                categoryEntity = getByCategory(category, language);
                if (null != categoryEntity && categoryEntity.getStatus() > 0) {
                    categoryEntity.setConfigs(new ArrayList<>());
                    categoryEntity.getConfigs().add(sysConfigEntity);
                    map.put(category, categoryEntity);
                }
            }
        }
        return map;
    }
}