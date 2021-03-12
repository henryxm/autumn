package cn.org.autumn.modules.lan.controller;

import cn.org.autumn.modules.lan.entity.LanguageMetadata;
import cn.org.autumn.modules.lan.service.Language;
import cn.org.autumn.modules.lan.service.LanguageService;
import cn.org.autumn.utils.R;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("api")
public class LanguageApi {
    @Autowired
    LanguageService languageService;

    @RequestMapping("/getsupportedlanguage")
    public R getSupportedLanguage(HttpServletRequest request) {
        Locale locale = Language.getLocale(request);
        String lang = languageService.toLang(locale);
        R r = R.ok();
        List<LanguageMetadata> page = languageService.getLanguageMetadata(lang);
        r.put("language", page);
        r.put("lang", lang);
        return r;
    }
}
