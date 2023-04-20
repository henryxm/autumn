package cn.org.autumn.modules.client.site;

import cn.org.autumn.base.ModuleMenu;
import cn.org.autumn.menu.Menu;
import org.springframework.stereotype.Service;

/**
 * 网站客户端
 *
 * @author Shaohua Xu
 * @email henryxm@163.com
 * @date 2021-01
 */
@Service
@Menu(name = "客户端", order = 555555, ico = "fa-eercast")
public class ClientMenu extends ModuleMenu {

    public String[][] getLanguageItems() {
        String[][] items = new String[][]{
                {getLanguageKey(), "客户端", "Client"},
        };
        return items;
    }
}
