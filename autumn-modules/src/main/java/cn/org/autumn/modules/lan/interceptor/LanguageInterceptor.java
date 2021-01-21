package cn.org.autumn.modules.lan.interceptor;

import cn.org.autumn.modules.lan.service.LanguageService;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.ui.ModelMap;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.Locale;
import java.util.Map;

@Component
public class LanguageInterceptor extends HandlerInterceptorAdapter {

    private static Logger logger = LoggerFactory.getLogger(LanguageInterceptor.class);

    @Autowired
    LanguageService languageService;

    public static String LANGUAGE_SESSION = "LANGUAGE_SESSION";

    public static Locale getLocale(HttpServletRequest request) {
        String language = request.getParameter("lang");
        Locale locale = Locale.getDefault();
        if (!StringUtils.isEmpty(language)) {
            language = language.replace("-", "_");
            String[] lc = language.split("_");
            if (lc.length == 2)
                locale = new Locale(lc[0], lc[1]);
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
            Locale locale = getLocale(request);
            if (null == locale) {
                logger.error("locale can not be null");
            }
            Map<String, String> lang = languageService.getLanguage(locale);
            if (null == lang || lang.isEmpty())
                logger.error("language cannot be empty");
            modelMap.put("lang", lang);
        }
    }
}
