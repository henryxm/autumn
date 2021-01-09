package cn.org.autumn.modules.gen.service;

import cn.org.autumn.modules.sys.service.SysMenuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import cn.org.autumn.modules.lan.service.Language;
import cn.org.autumn.modules.lan.service.LanguageService;

/**
 * 生成方案
 *
 * @author Shaohua Xu
 * @email henryxm@163.com
 * @date 2021-01
 */
@Service
public class GenMenu {

    public static final String gen_menu = SysMenuService.getMenuKey("Gen", "GenMenu");
    public static final String parent_menu = "";
    public static final String gen_language = "gen_menu";

    @Autowired
    protected SysMenuService sysMenuService;

    @Autowired
    protected Language language;

    @Autowired
    protected LanguageService languageService;

    protected String order(){
        return "0";
    }

    protected String ico(){
        return "fa-file-code-o";
    }

    public void init() {
    }
}
