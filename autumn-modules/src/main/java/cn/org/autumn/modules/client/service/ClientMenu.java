package cn.org.autumn.modules.client.service;

import cn.org.autumn.modules.sys.service.SysMenuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import cn.org.autumn.modules.lan.service.Language;
import cn.org.autumn.modules.lan.service.LanguageService;

/**
 * 网站客户端
 *
 * @author Shaohua Xu
 * @email henryxm@163.com
 * @date 2021-01
 */
@Service
public class ClientMenu {

    public static final String client_menu = SysMenuService.getMenuKey("Client", "ClientMenu");
    public static final String parent_menu = "";
    public static final String client_language = "client_menu";

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
                {"客户端", "", "", "0", "fa fa-eercast", "555555", client_menu, parent_menu, client_language + "_text"},
        };
        return menus;
    }

    public String[][] getLanguageItemArray() {
        String[][] items = new String[][]{
                {client_language + "_text", "客户端", "Client"},
        };
        return items;
    }
}
