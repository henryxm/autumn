package cn.org.autumn.modules.lan.service;

import cn.org.autumn.modules.lan.interceptor.LanguageInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.util.Locale;
import java.util.Map;

@Component
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
}
