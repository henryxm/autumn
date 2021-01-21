package cn.org.autumn.modules.oauth.service;

import cn.org.autumn.modules.oauth.service.gen.OauthMenuGen;
import org.springframework.stereotype.Service;

/**
 * 授权令牌
 *
 * @author Shaohua Xu
 * @email henryxm@163.com
 * @date 2021-01
 */
@Service
public class OauthMenu extends OauthMenuGen {

    @Override
    protected String order() {
        return "666666";
    }

    @Override
    public String ico() {
        return "fa-sign-in";
    }

    @Override
    public String getMenu() {
        return super.getMenu();
    }

    @Override
    public String getParentMenu() {
        return super.getParentMenu();
    }

    @Override
    public void init() {
        super.init();
    }

    public String[][] getLanguageItems() {
        String[][] items = new String[][]{
                {oauth_menu + "_text", "授权登录", "Authentication"},
        };
        return items;
    }
}
