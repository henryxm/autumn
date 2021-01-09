package cn.org.autumn.modules.wall.service;

import cn.org.autumn.modules.sys.service.SysMenuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import cn.org.autumn.modules.lan.service.Language;
import cn.org.autumn.modules.lan.service.LanguageService;

/**
 * 主机统计
 *
 * @author Shaohua Xu
 * @email henryxm@163.com
 * @date 2021-01
 */
@Service
public class WallMenu {

    public static final String wall_menu = SysMenuService.getMenuKey("Wall", "WallMenu");
    public static final String parent_menu = "";
    public static final String wall_language = "wall_menu";

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
                {"防火墙", "", "", "0", "fa fa-firefox", "777777", wall_menu, parent_menu, wall_language + "_text"},
        };
        return menus;
    }

    public String[][] getLanguageItemArray() {
        String[][] items = new String[][]{
                {wall_language + "_text", "防火墙", "Fire wall"},
        };
        return items;
    }
}
