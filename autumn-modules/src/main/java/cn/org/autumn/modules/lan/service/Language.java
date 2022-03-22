package cn.org.autumn.modules.lan.service;

import cn.org.autumn.modules.lan.interceptor.LanguageInterceptor;
import cn.org.autumn.modules.sys.service.SysConfigService;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static cn.org.autumn.modules.lan.interceptor.LanguageInterceptor.LANGUAGE_SESSION;

@Service
public final class Language {
    Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    @Lazy
    LanguageService languageService;

    @Autowired
    @Lazy
    SysConfigService sysConfigService;

    public static Locale getLocale(HttpServletRequest httpServletRequest) {
        return LanguageInterceptor.getLocale(httpServletRequest);
    }

    public String toLang(Locale locale) {
        return languageService.toLang(locale);
    }

    public static Locale toLocale(String lang) {
        Locale locale = null;
        if (StringUtils.isNotEmpty(lang)) {
            lang = lang.replace("-", "_");
            String[] lc = lang.split("_");
            if (lc.length == 2)
                locale = new Locale(lc[0], lc[1]);
        }
        return locale;
    }

    /**
     * lang example: zh_CN
     *
     * @param httpServletRequest
     * @param lang
     */
    public static void setLocale(HttpServletRequest httpServletRequest, String lang) {
        setLocale(httpServletRequest, toLocale(lang));
    }

    public static void setLocale(HttpServletRequest httpServletRequest, Locale locale) {
        try {
            if (null != locale && null != httpServletRequest) {
                HttpSession session = httpServletRequest.getSession();
                session.setAttribute(LANGUAGE_SESSION, locale);
            }
        } catch (Exception e) {
        }
    }

    public String get(String key, HttpServletRequest httpServletRequest) {
        Map<String, String> language = languageService.getLanguage(getLocale(httpServletRequest));
        if (null != language && language.containsKey(key)) {
            return language.get(key);
        }
        return key;
    }

    public void put(boolean update, Object... objects) {
        languageService.put(update, objects);
    }

    public void put(Object... objects) {
        try {
            languageService.put(objects);
        } catch (Exception e) {
            log.debug(e.getMessage());
        }
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
