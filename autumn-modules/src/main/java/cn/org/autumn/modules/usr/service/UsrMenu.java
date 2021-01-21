package cn.org.autumn.modules.usr.service;

import cn.org.autumn.modules.usr.service.gen.UsrMenuGen;
import org.springframework.stereotype.Service;

/**
 * 用户信息
 *
 * @author Shaohua Xu
 * @email henryxm@163.com
 * @date 2021-01
 */
@Service
public class UsrMenu extends UsrMenuGen {

    @Override
    protected String order() {
        return "444444";
    }

    @Override
    public String ico() {
        return "fa-users";
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
                {usr_menu + "_text", "用户管理", "User profiles"},
        };
        return items;
    }
}
