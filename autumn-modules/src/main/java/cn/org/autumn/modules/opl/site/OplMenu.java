package cn.org.autumn.modules.opl.site;

import cn.org.autumn.base.ModuleMenu;
import cn.org.autumn.menu.Menu;
import org.springframework.stereotype.Service;

@Service
@Menu(name = "开放平台", order = 666660, ico = "fa-share-alt")
public class OplMenu extends ModuleMenu {

    public String[][] getLanguageItems() {
        return new String[][]{
                {getLanguageKey(), "开放平台", "Open Platform"},
                {"opl_oplmanage", "统一管理", "OPL Manage"},
        };
    }
}
