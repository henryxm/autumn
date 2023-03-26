package cn.org.autumn.modules.lan.interceptor;

import cn.org.autumn.config.Config;
import cn.org.autumn.config.InterceptorHandler;
import cn.org.autumn.modules.lan.service.Language;
import cn.org.autumn.modules.lan.service.LanguageService;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.ui.ModelMap;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.Locale;
import java.util.Map;

@Component
public class LanguageInterceptor extends HandlerInterceptorAdapter implements InterceptorHandler {

    private static Logger logger = LoggerFactory.getLogger(LanguageInterceptor.class);

    @Autowired
    LanguageService languageService;

    public static LanguageService lang;

    public static String LANGUAGE_SESSION = "LANGUAGE_SESSION";

    public static Locale getLocale(HttpServletRequest request) {
        String language = request.getParameter("lang");
        Locale locale = Locale.getDefault();
        if (StringUtils.isNotEmpty(language)) {
            Locale l = Language.toLocale(language);
            if (null != l)
                locale = l;
            //将国际化语言保存到session
            HttpSession session = request.getSession();
            session.setAttribute(LANGUAGE_SESSION, locale);
        } else {
            //如果没有带国际化参数，则判断session有没有保存，有保存，则使用保存的，也就是之前设置的，避免之后的请求不带国际化参数造成语言显示不对
            try {
                HttpSession session = request.getSession();
                Locale localeInSession = (Locale) session.getAttribute(LANGUAGE_SESSION);
                if (localeInSession != null) {
                    locale = localeInSession;
                } else {
                    if (StringUtils.isEmpty(language)) {
                        if (null == lang)
                            lang = (LanguageService) Config.getBean("languageService");
                        if (null != lang)
                            language = lang.getUserDefaultLanguage();
                        Locale l = Language.toLocale(language);
                        if (null != l)
                            locale = l;
                    }
                }
            } catch (Exception e) {
                logger.error("LanguageInterceptor.getLocale error: " + e.getMessage());
            }
        }
        return locale;
    }

    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, @Nullable ModelAndView modelAndView) throws Exception {
        String uri = request.getRequestURI();
        if (null != modelAndView) {
            ModelMap modelMap = modelAndView.getModelMap();
            String locale = null;
            Map<String, String> lang = null;
            if (modelMap.containsAttribute("locale")) {
                Object obj = modelMap.getAttribute("locale");
                if (obj instanceof String) {
                    locale = (String) obj;
                }
            } else {
                modelMap.addAttribute("locale", "");
            }
            if (StringUtils.isNotBlank(locale)) {
                lang = languageService.getLanguage(locale);
            }
            if (null == lang || lang.isEmpty()) {
                lang = languageService.getLanguage(getLocale(request));
            }
            modelMap.put("lang", lang);
        }
    }

    @Override
    public HandlerInterceptor getHandlerInterceptor() {
        return this;
    }
}
