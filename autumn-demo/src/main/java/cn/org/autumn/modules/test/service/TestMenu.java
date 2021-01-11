package cn.org.autumn.modules.test.service;

import cn.org.autumn.modules.test.service.gen.TestMenuGen;
import org.springframework.stereotype.Service;

/**
 * 测试例子
 *
 * @author Shaohua Xu
 * @email henryxm@163.com
 * @date 2021-01
 */
@Service
public class TestMenu extends TestMenuGen {

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
        super.init();
    }

    public String[][] getLanguageItems() {
        String[][] items = new String[][]{
                {test_menu + "_text", "测试例子", "Test Example"},
        };
        return items;
    }
}
