package cn.org.autumn.modules.oauth.site;

import cn.org.autumn.base.ModuleMenu;
import cn.org.autumn.menu.Menu;
import org.springframework.stereotype.Service;

/**
 * 授权令牌
 *
 * @author Shaohua Xu
 * @email henryxm@163.com
 * @date 2021-01
 */
@Service
@Menu(name = "授权登录", order = 666666, ico = "fa-sign-in")
public class OauthMenu extends ModuleMenu {

    public String[][] getLanguageItems() {
        String[][] items = new String[][]{
                {getLanguageKey(), "授权登录", "Authentication"},
        };
        return items;
    }
}
