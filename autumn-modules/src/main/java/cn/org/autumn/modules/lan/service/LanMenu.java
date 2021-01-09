package cn.org.autumn.modules.lan.service;

import cn.org.autumn.modules.sys.service.SysMenuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 国家语言
 *
 * @author Shaohua Xu
 * @email henryxm@163.com
 * @date 2021-01
 */
@Service
public class LanMenu {

    public static final String lan_menu = SysMenuService.getMenuKey("Lan", "LanMenu");
    public static final String parent_menu = "";
    public static final String lan_language = "lan_menu";

    @Autowired
    protected SysMenuService sysMenuService;

    @Autowired
    protected Language language;

    @Autowired
    protected LanguageService languageService;

    public void init() {
    }
}
