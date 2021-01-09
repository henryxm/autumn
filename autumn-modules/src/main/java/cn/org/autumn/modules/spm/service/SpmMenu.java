package cn.org.autumn.modules.spm.service;

import cn.org.autumn.modules.sys.service.SysMenuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import cn.org.autumn.modules.lan.service.Language;
import cn.org.autumn.modules.lan.service.LanguageService;

/**
 * 访问统计
 *
 * @author Shaohua Xu
 * @email henryxm@163.com
 * @date 2021-01
 */
@Service
public class SpmMenu {

    public static final String spm_menu = SysMenuService.getMenuKey("Spm", "SpmMenu");
    public static final String parent_menu = "";
    public static final String spm_language = "spm_menu";

    @Autowired
    protected SysMenuService sysMenuService;

    @Autowired
    protected Language language;

    @Autowired
    protected LanguageService languageService;

    protected String order() {
        return "888888";
    }

    protected String ico() {
        return "fa-dot-circle-o";
    }

    public void init() {
        sysMenuService.put(getMenus());
        language.add(getLanguageItemArray());
        addLanguageColumnItem();
    }

    public void addLanguageColumnItem() {
    }

    public String[][] getMenus() {
        String[][] menus = new String[][]{
                //{0:菜单名字,1:URL,2:权限,3:菜单类型,4:ICON,5:排序,6:MenuKey,7:ParentKey,8:Language}
                {"超级模型", "", "", "0", "fa " + ico(), order(), spm_menu, parent_menu, spm_language + "_text"},
        };
        return menus;
    }

    public String[][] getLanguageItemArray() {
        String[][] items = new String[][]{
                {spm_language + "_text", "超级模型", "Super model"},
        };
        return items;
    }
}
