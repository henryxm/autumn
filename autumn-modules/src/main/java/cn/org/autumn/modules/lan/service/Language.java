package cn.org.autumn.modules.lan.service;

import static cn.org.autumn.modules.lan.interceptor.LanguageInterceptor.LANGUAGE_SESSION;

import cn.org.autumn.modules.lan.interceptor.LanguageInterceptor;
import cn.org.autumn.modules.sys.service.SysConfigService;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public final class Language {

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

    /**
     * 将 {@code ?lang=} 参数解析为 {@link Locale}。
     * <p>
     * 支持常见简写（如 {@code en}、{@code cn}、{@code zh}），便于安装页等场景无需输入完整 {@code en_us}/{@code zh_cn}。
     */
    public static Locale toLocale(String lang) {
        if (StringUtils.isEmpty(lang)) {
            return null;
        }
        String norm = lang.trim().replace("-", "_");
        String lower = norm.toLowerCase(Locale.ROOT);
        if ("en".equals(lower) || "english".equals(lower)) {
            return Locale.US;
        }
        if ("cn".equals(lower) || "zh".equals(lower) || "chinese".equals(lower)) {
            return Locale.SIMPLIFIED_CHINESE;
        }
        String[] lc = norm.split("_");
        if (lc.length == 2 && StringUtils.isNotBlank(lc[0]) && StringUtils.isNotBlank(lc[1])) {
            return new Locale(lc[0].toLowerCase(Locale.ROOT), lc[1].toUpperCase(Locale.ROOT));
        }
        if (lc.length == 1 && StringUtils.isNotBlank(lc[0])) {
            String language = lc[0].toLowerCase(Locale.ROOT);
            if ("en".equals(language)) {
                return Locale.US;
            }
            if ("zh".equals(language)) {
                return Locale.SIMPLIFIED_CHINESE;
            }
        }
        return null;
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
