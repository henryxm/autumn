package cn.org.autumn.modules.wall.site;

import cn.org.autumn.menu.Menu;
import cn.org.autumn.base.ModuleMenu;
import org.springframework.stereotype.Service;

/**
 * @author User
 * @email henryxm@163.com
 * @date 2021-11
 */
@Service
@Menu(name = "防火墙", order = 777777, ico = "fa-firefox")
public class WallMenu extends ModuleMenu {

    public String[][] getLanguageItems() {
        return new String[][]{
                {getLanguageKey(), "防火墙", "Fire Wall"},
        };
    }
}