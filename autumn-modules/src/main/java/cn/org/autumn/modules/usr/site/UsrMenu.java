package cn.org.autumn.modules.usr.site;

import cn.org.autumn.menu.Menu;
import cn.org.autumn.base.ModuleMenu;
import org.springframework.stereotype.Service;

@Service
@Menu(name = "用户管理", order = 444444, ico = "fa-users")
public class UsrMenu extends ModuleMenu {
    public static final String usr_menu = "usr_menu";

    @Override
    public String order() {
        return "444444";
    }

    @Override
    public String ico() {
        return "fa-users";
    }

    public String[][] getLanguageItems() {
        return new String[][]{
                {usr_menu + "_text", "用户管理", "User profiles"},
        };
    }
}