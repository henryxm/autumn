package cn.org.autumn.modules.test.site;

import cn.org.autumn.menu.Menu;
import cn.org.autumn.base.ModuleMenu;
import org.springframework.stereotype.Service;

/**
 * 测试例子
 *
 * @author User
 * @email henryxm@163.com
 * @date 2021-08
 */
@Service
@Menu(name = "测试例子", order = 0, ico = "fa-file-code-o")
public class TestMenu extends ModuleMenu {

    public String[][] getLanguageItems() {
        String[][] items = new String[][]{
                {getLanguageKey(), "测试例子", "Test Example"},
        };
        return items;
    }
}