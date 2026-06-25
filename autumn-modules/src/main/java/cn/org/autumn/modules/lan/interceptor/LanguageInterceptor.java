package cn.org.autumn.modules.lan.interceptor;

import cn.org.autumn.config.Config;
import cn.org.autumn.config.InterceptorHandler;
import cn.org.autumn.install.InstallMode;
import cn.org.autumn.modules.install.InstallWizardLangSupport;
import cn.org.autumn.modules.lan.service.Language;
import cn.org.autumn.modules.lan.service.LanguageService;
import cn.org.autumn.utils.InterceptorUtils;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.ui.ModelMap;
import org.springframework.web.servlet.AsyncHandlerInterceptor;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

@Slf4j
@Component
public class LanguageInterceptor implements InterceptorHandler, AsyncHandlerInterceptor {

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
                log.debug("LanguageInterceptor.getLocale error: " + e.getMessage());
            }
        }
        return locale;
    }

    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, @Nullable ModelAndView modelAndView) throws Exception {
        if (InterceptorUtils.skip(handler, this.getClass())) {
            return;
        }
        /*
         * 安装阶段：统一注入 lang，与业务页契约一致。词条来源分层——
         * ① classpath 种子（{@link InstallWizardLangSupport}，不访问 DB）；
         * ② 若 {@link LanguageService} 内存中已有词条（如引导库已存在 lan 表且曾加载），再合并覆盖。
         * 绝不调用 getUserDefaultLanguage() / sys_config，避免「库未初始化」悖论。
         */
        if (InstallMode.isActive()) {
            if (modelAndView != null) {
                Locale loc = resolveLocaleInstallSafe(request);
                Map<String, String> lang = new HashMap<>(InstallWizardLangSupport.seedMap(loc));
                try {
                    String tag = languageService.toLang(loc);
                    Map<String, String> mem = languageService.getLanguage(tag);
                    if (mem != null && !mem.isEmpty()) {
                        lang.putAll(mem);
                    }
                } catch (Exception ignored) {
                }
                modelAndView.getModelMap().put("lang", lang);
            }
            return;
        }
        if (null != modelAndView) {
            ModelMap modelMap = modelAndView.getModelMap();
            String locale = null;
            Map<String, String> lang = null;
            if (modelMap.containsAttribute("locale")) {
                Object obj = modelMap.getAttribute("locale");
                if (obj instanceof String) {
                    locale = (String) obj;
                }
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

    /**
     * 安装模式下解析界面语言：仅 ?lang= 与 session，不读 sys_config。
     */
    private static Locale resolveLocaleInstallSafe(HttpServletRequest request) {
        String language = request.getParameter("lang");
        if (StringUtils.isNotEmpty(language)) {
            Locale l = Language.toLocale(language);
            if (l != null) {
                try {
                    HttpSession session = request.getSession();
                    session.setAttribute(LANGUAGE_SESSION, l);
                } catch (Exception ignored) {
                }
                return l;
            }
        }
        try {
            HttpSession session = request.getSession();
            Object o = session.getAttribute(LANGUAGE_SESSION);
            if (o instanceof Locale) {
                return (Locale) o;
            }
        } catch (Exception ignored) {
        }
        return Locale.SIMPLIFIED_CHINESE;
    }

    @Override
    public HandlerInterceptor getHandlerInterceptor() {
        return this;
    }
}
