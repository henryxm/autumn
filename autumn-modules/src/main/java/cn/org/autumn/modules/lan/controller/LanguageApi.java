package cn.org.autumn.modules.lan.controller;

import cn.org.autumn.modules.lan.entity.LanguageMetadata;
import cn.org.autumn.modules.lan.service.LanguageService;
import cn.org.autumn.utils.R;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("api")
public class LanguageApi {
    @Autowired
    LanguageService languageService;

    @RequestMapping("/getsupportedlanguage")
    public R getSupportedLanguage(HttpServletRequest request) {
        List<LanguageMetadata> page = languageService.getLanguageMetadata();
        return R.ok().put("language", page);
    }
}
