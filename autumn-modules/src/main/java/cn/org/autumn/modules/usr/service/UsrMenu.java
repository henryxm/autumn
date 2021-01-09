package cn.org.autumn.modules.usr.service;

import cn.org.autumn.modules.sys.service.SysMenuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import cn.org.autumn.modules.lan.service.Language;
import cn.org.autumn.modules.lan.service.LanguageService;

/**
 * 用户信息
 *
 * @author Shaohua Xu
 * @email henryxm@163.com
 * @date 2021-01
 */
@Service
public class UsrMenu {

    public static final String usr_menu = SysMenuService.getMenuKey("Usr", "UsrMenu");
    public static final String parent_menu = "";
    public static final String usr_language = "usr_menu";

    @Autowired
    protected SysMenuService sysMenuService;

    @Autowired
    protected Language language;

    @Autowired
    protected LanguageService languageService;

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
                {"用户管理", "", "", "0", "fa fa-users", "444444", usr_menu, parent_menu, usr_language + "_text"},
        };
        return menus;
    }

    public String[][] getLanguageItemArray() {
        String[][] items = new String[][]{
                {usr_language + "_text", "用户管理", "User profiles"},
        };
        return items;
    }
}
