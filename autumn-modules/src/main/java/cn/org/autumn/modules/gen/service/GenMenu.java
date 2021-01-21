package cn.org.autumn.modules.gen.service;

import cn.org.autumn.modules.gen.service.gen.GenMenuGen;
import org.springframework.stereotype.Service;

/**
 * 生成方案
 *
 * @author Shaohua Xu
 * @email henryxm@163.com
 * @date 2021-01
 */
@Service
public class GenMenu extends GenMenuGen {

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
