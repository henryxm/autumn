package cn.org.autumn.modules.client.service;

import cn.org.autumn.modules.client.service.gen.ClientMenuGen;
import org.springframework.stereotype.Service;

/**
 * 网站客户端
 *
 * @author Shaohua Xu
 * @email henryxm@163.com
 * @date 2021-01
 */
@Service
public class ClientMenu extends ClientMenuGen {

    @Override
    protected String order() {
        return "555555";
    }

    @Override
    public String ico() {
        return "fa-eercast";
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
                {client_menu + "_text", "客户端", "Client"},
        };
        return items;
    }
}
