package cn.org.autumn.modules.gen.service;

import cn.org.autumn.modules.sys.service.SysMenuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import cn.org.autumn.modules.lan.service.LanguageService;

/**
 * 代码生成设置
 *
 * @author Shaohua Xu
 * @email henryxm@163.com
 * @date 2020-10
 */

@Service
public class GenMenu {

    public static final String gen_menu = SysMenuService.getMenuKey("Gen", "TopMenu");
    public static final String parent_menu = "";
    public static final String gen_language = "gen_menu";

    @Autowired
    protected SysMenuService sysMenuService;

    @Autowired
    protected LanguageService languageService;

    public void init() {
    }
}
