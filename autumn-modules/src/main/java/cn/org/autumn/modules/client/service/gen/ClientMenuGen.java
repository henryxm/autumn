package cn.org.autumn.modules.client.service.gen;

import cn.org.autumn.modules.lan.service.Language;
import cn.org.autumn.modules.lan.service.LanguageService;
import cn.org.autumn.modules.sys.service.SysMenuService;
import cn.org.autumn.site.InitFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 网站客户端
 *
 * @author Shaohua Xu
 * @email henryxm@163.com
 * @date 2021-01
 */
public class ClientMenuGen implements InitFactory.Init {

    public static final String client_menu = "client_menu";

    @Autowired
    protected SysMenuService sysMenuService;

    @Autowired
    protected Language language;

    @Autowired
    protected LanguageService languageService;

    public String ico() {
        return "fa-file-code-o";
    }

    public int menuOrder() {
        return 0;
    }

    protected String order() {
        return String.valueOf(menuOrder());
    }

    public String getMenu() {
        return SysMenuService.getMenuKey("Client", "ClientMenu");
    }

    public String getParentMenu() {
        return "";
    }

    public void init() {
        sysMenuService.put(getMenuItemsInternal(), getMenuItems(), getMenuList());
        language.put(getLanguageItemsInternal(), getLanguageItems(), getLanguageList());
        addLanguageColumnItem();
    }

    @Deprecated
    public void addLanguageColumnItem() {
    }

    public List<String[]> getMenuList() {
        return null;
    }

    public String[][] getMenuItems() {
        return null;
    }

    public String[][] getMenuItemsInternal() {
        String[][] menus = new String[][]{
                //{0:菜单名字,1:URL,2:权限,3:菜单类型,4:ICON,5:排序,6:MenuKey,7:ParentKey,8:Language}
                {"客户端", "", "", "0", "fa " + ico(), order(), getMenu(), getParentMenu(), client_menu + "_text"},
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
                {client_menu + "_text", "客户端"},
        };
        return items;
    }
}
