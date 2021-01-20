package cn.org.autumn.modules.lan.service;

import cn.org.autumn.modules.lan.service.gen.LanMenuGen;
import org.springframework.stereotype.Service;

/**
 * 国家语言
 *
 * @author Shaohua Xu
 * @email henryxm@163.com
 * @date 2021-01
 */
@Service
public class LanMenu extends LanMenuGen {

    @Override
    protected String order() {
        return super.order();
    }

    @Override
    public String ico() {
        return super.ico();
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
    }

    public String[][] getLanguageItems() {
        return null;
    }
}
