package cn.org.autumn.modules.usr.service.gen;

import cn.org.autumn.modules.sys.service.SysMenuService;
import cn.org.autumn.site.InitFactory;
import org.springframework.beans.factory.annotation.Autowired;
import cn.org.autumn.modules.lan.service.Language;
import cn.org.autumn.modules.lan.service.LanguageService;
import java.util.List;
/**
 * 用户信息
 *
 * @author Shaohua Xu
 * @email henryxm@163.com
 * @date 2021-01
 */
public class UsrMenuGen implements InitFactory.Init {

    public static final String usr_menu = "usr_menu";

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

    public String getMenu() {
        return SysMenuService.getMenuKey("Usr", "UsrMenu");
    }

    public String getParentMenu() {
        return "";
    }

    public void init() {
        sysMenuService.put(getMenuItemsInternal(), getMenuItems(), getMenuList());
        language.put(getLanguageItemsInternal(), getLanguageItems(), getLanguageList());
    }

    public List<String[]> getMenuList() {
        return null;
    }

    public String[][] getMenuItems() {
        return null;
    }

    private String[][] getMenuItemsInternal() {
        String[][] menus = new String[][]{
                //{0:菜单名字,1:URL,2:权限,3:菜单类型,4:ICON,5:排序,6:MenuKey,7:ParentKey,8:Language}
                {"用户管理", "", "", "0", "fa " + ico(), order(), getMenu(), getParentMenu(), usr_menu + "_text"},
        };
        return menus;
    }

    public List<String[]> getLanguageList() {
        return null;
    }

    public String[][] getLanguageItems() {
        return null;
    }

    private String[][] getLanguageItemsInternal() {
        String[][] items = new String[][]{
                {usr_menu + "_text", "用户管理"},
        };
        return items;
    }
}
