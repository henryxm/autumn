package cn.org.autumn.base;

import cn.org.autumn.menu.BaseMenu;
import cn.org.autumn.modules.lan.service.Language;
import cn.org.autumn.modules.lan.service.LanguageService;
import cn.org.autumn.modules.sys.service.SysMenuService;
import cn.org.autumn.site.InitFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

public abstract class ModuleMenu implements BaseMenu, InitFactory.Init {

    @Autowired
    protected SysMenuService sysMenuService;

    @Autowired
    protected Language language;

    @Autowired
    protected LanguageService languageService;

    public String getMenu() {
        return SysMenuService.getMenuKey(getNamespace(), getMenuKey());
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
        return new String[][]{
                //{0:菜单名字,1:URL,2:权限,3:菜单类型,4:ICON,5:排序,6:MenuKey,7:ParentKey,8:Language}
                {getName(), "", "", "0", "fa " + ico(), order(), getMenu(), getParentMenu(), getLanguageKey()},
        };
    }

    public List<String[]> getLanguageList() {
        return null;
    }

    public String[][] getLanguageItems() {
        return null;
    }

    private String[][] getLanguageItemsInternal() {
        return new String[][]{{getLanguageKey(), getName()}};
    }
}