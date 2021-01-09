package cn.org.autumn.modules.lan.service;

import cn.org.autumn.modules.lan.interceptor.LanguageInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public final class Language {

    @Autowired
    LanguageService languageService;

    public Locale getLocale(HttpServletRequest httpServletRequest) {
        return LanguageInterceptor.getLocale(httpServletRequest);
    }

    public String get(String key, HttpServletRequest httpServletRequest) {
        Map<String, String> language = languageService.getLanguage(getLocale(httpServletRequest));
        if (null != language && language.containsKey(key)) {
            return language.get(key);
        }
        return key;
    }

    public void add(String key, String zh_CN) {
        languageService.addLanguageColumnItem(key, zh_CN);
    }

    public void add(String key, String zh_CN, String en_US) {
        languageService.addLanguageColumnItem(key, zh_CN, en_US);
    }

    public void add(String[][] items) {
        if (null == items)
            return;
        for (String[] item : items) {
            if (item.length == 2) {
                languageService.addLanguageColumnItem(item[0], item[1]);
            }
            if (item.length > 2) {
                languageService.addLanguageColumnItem(item[0], item[1], item[2]);
            }
        }
    }

    public void add(List<String[]> items) {
        if (null == items)
            return;
        for (String[] item : items) {
            if (item.length == 2) {
                languageService.addLanguageColumnItem(item[0], item[1]);
            }
            if (item.length > 2) {
                languageService.addLanguageColumnItem(item[0], item[1], item[2]);
            }
        }
    }
}
