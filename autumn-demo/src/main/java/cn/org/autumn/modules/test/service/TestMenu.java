package cn.org.autumn.modules.test.service;

import cn.org.autumn.modules.sys.service.SysMenuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import cn.org.autumn.modules.lan.service.Language;
import cn.org.autumn.modules.lan.service.LanguageService;

/**
 * 测试例子
 *
 * @author Shaohua Xu
 * @email henryxm@163.com
 * @date 2021-01
 */
@Service
public class TestMenu {

    public static final String test_menu = SysMenuService.getMenuKey("Test", "TestMenu");
    public static final String parent_menu = "";
    public static final String test_language = "test_menu";

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
        sysMenuService.put(getMenus());
        language.add(getLanguageItemArray());
        addLanguageColumnItem();
    }

    public void addLanguageColumnItem() {
    }

    public String[][] getMenus() {
        String[][] menus = new String[][]{
                //{0:菜单名字,1:URL,2:权限,3:菜单类型,4:ICON,5:排序,6:MenuKey,7:ParentKey,8:Language}
                {"测试例子", "", "", "0", "fa " + ico(), order(), test_menu, parent_menu, test_language + "_text"},
        };
        return menus;
    }

    public String[][] getLanguageItemArray() {
        String[][] items = new String[][]{
                {test_language + "_text", "测试例子", "Test example"},
        };
        return items;
    }
}
