package cn.org.autumn.modules.spm.service;

import cn.org.autumn.modules.spm.service.gen.SpmMenuGen;
import org.springframework.stereotype.Service;

/**
 * 超级位置模型
 *
 * @author Shaohua Xu
 * @email henryxm@163.com
 * @date 2021-01
 */
@Service
public class SpmMenu extends SpmMenuGen {

    @Override
    protected String order() {
        return "888888";
    }

    @Override
    public String ico() {
        return "fa-dot-circle-o";
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
                {spm_menu + "_text", "超级模型", "Super model"},
        };
        return items;
    }
}
