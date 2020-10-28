package cn.org.autumn.modules.lan.service;

import cn.org.autumn.modules.lan.entity.LanguageEntity;
import cn.org.autumn.modules.lan.service.gen.LanguageServiceGen;
import cn.org.autumn.table.utils.HumpConvert;
import io.netty.util.internal.StringUtil;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.lang.reflect.Field;
import java.util.*;

@Service
public class LanguageService extends LanguageServiceGen {

    private static Map<String, Map<String, String>> languages;

    static {
        languages = new LinkedHashMap<>();
    }

    @Override
    public int menuOrder() {
        return 6;
    }

    @Override
    public String ico() {
        return "fa-language";
    }

    public int parentMenu() {
        return 1;
    }

    public static void f(LanguageEntity languageEntity) {
        if (null == languageEntity)
            return;
        if (StringUtil.isNullOrEmpty(languageEntity.getName()))
            return;
        Field[] fields = LanguageEntity.class.getDeclaredFields();
        for (Field field : fields) {
            String name = field.getName();
            if ("serialVersionUID".equalsIgnoreCase(name) || "id".equalsIgnoreCase(name) || "name".equalsIgnoreCase(name))
                continue;
            name = HumpConvert.HumpToUnderline(name);
            Map<String, String> map = null;
            if (!languages.containsKey(name)) {
                map = new LinkedHashMap<>();
                languages.put(name, map);
            } else {
                map = languages.get(name);
            }
            field.setAccessible(true);
            Object v = null;
            try {
                v = field.get(languageEntity);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
            if (null == v)
                map.put(languageEntity.getName(), languageEntity.getZhCn());
            else
                map.put(languageEntity.getName(), v.toString());
        }
    }

    public void load() {
        List<LanguageEntity> languageEntityList = baseMapper.selectByMap(new HashMap<>());
        for (LanguageEntity languageEntity : languageEntityList) {
            f(languageEntity);
        }
    }

    public Map<String, String> getLanguage(Locale locale) {
        String t = locale.toLanguageTag();
        t = t.replace("-", "_").toLowerCase();
        return languages.get(t);
    }

    /**
     * key定义规范：{模块包名}_{类型}_{单词或缩写}
     *
     * @param key
     * @param zhCn
     * @return
     */
    public boolean addItem(String key, String zhCn, String enUs) {
        if (baseMapper.hasKey(key) > 0)
            return false;
        LanguageEntity languageEntity = new LanguageEntity();
        languageEntity.setName(key);
        languageEntity.setZhCn(zhCn);
        languageEntity.setEnUs(enUs);
        insert(languageEntity);
        return true;
    }

    @PostConstruct
    public void init() {
        super.init();
        addItem("sys_string_management", "Autumn 后台管理系统", "Autumn Management System");
        addItem("sys_string_welcome", "欢迎", "Welcome");
        addItem("sys_string_login", "登录", "Login");
        addItem("sys_string_logout", "退出系统", "Logout");
        addColumnItem();
        load();
    }

    public void addColumnItem() {
        addItem("sys_string_lan_column_name", "标识", "Unique Name");
        addItem("sys_string_lan_column_en_us", "英语(美国)", "English(United States)");
    }
}
